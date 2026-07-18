/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.process;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

/**
 * Regression tests for {@link PSProcessDaemon} focused on the {@code java/path-injection} finding
 * at {@code validatePath} (CodeQL alert #707, T043).
 *
 * <p>The pre-fix code in {@code validatePath(byte[])} did:
 *
 * <pre>
 * sourcePath = sourcePath.replace('\\', '/');
 * if (sourcePath.indexOf("../") != -1 || (len==2 && "..".equals(sourcePath))) {
 *   throw new Exception("Cannot use ../ ...");
 * }
 * if (sourcePath.startsWith("/")) sourcePath = sourcePath.substring(1);
 * File fullPath = new File(m_pathRoot, sourcePath);
 * return fullPath;
 * </pre>
 *
 * <p>This only rejected literal "../" substrings and the exact ".." token. It did not protect
 * against NUL bytes, absolute paths that bypass the single leading-/ strip (e.g. "//abs" on Unix or
 * "C:\\abs" on Windows), or canonicalization escapes via symlinks / case / normalization. An
 * attacker-supplied path could reach filesystem operations outside the daemon's configured virtual
 * root.
 *
 * <p>The fix replaces the final {@code new File(m_pathRoot, sourcePath)} with {@link
 * com.percussion.security.io.PSPathInjectionGuard#requireUnderBase(File, String)}, which performs
 * full canonical-path containment verification. The legacy "../" check and leading-/ strip are
 * retained for compatibility; the guard is the authoritative barrier. A sink-line {@code //
 * codeql[java/path-injection]} suppression documents the residual legacy logic.
 *
 * <p>Tests use {@link sun.misc.Unsafe#allocateInstance} to construct a shell {@code
 * PSProcessDaemon} and its private inner {@code RequestHandler}, then reflectively invoke the
 * private {@code validatePath(byte[])} to exercise the guard at the exact call site.
 */
@DisplayName("PSProcessDaemon - Path Traversal Prevention via PSPathInjectionGuard (CWE-22, #707)")
class PSProcessDaemonPathInjectionTest {

  @TempDir File pathRoot;

  private static final Unsafe UNSAFE;
  private static final Method VALIDATE_PATH_METHOD;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      Class<?> rhClass = Class.forName("com.percussion.process.PSProcessDaemon$RequestHandler");
      VALIDATE_PATH_METHOD = rhClass.getDeclaredMethod("validatePath", byte[].class);
      VALIDATE_PATH_METHOD.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private Object requestHandler;

  @BeforeEach
  void setupDaemonShell() throws Exception {
    // Minimal proc def file so ctor would succeed if it reached file read (we bypass ctor).
    File procDef = new File(pathRoot, "rw_processes.xml");
    try (FileWriter w = new FileWriter(procDef)) {
      w.write("<Processes/>");
    }

    // Allocate a shell PSProcessDaemon (bypasses ctor and thread start).
    Object daemon = UNSAFE.allocateInstance(PSProcessDaemon.class);
    setField(daemon, "m_pathRoot", pathRoot);

    // Allocate a shell RequestHandler and wire its synthetic enclosing instance (this$0).
    Class<?> rhClass = Class.forName("com.percussion.process.PSProcessDaemon$RequestHandler");
    Object rh = UNSAFE.allocateInstance(rhClass);
    setField(rh, "this$0", daemon);

    // m_sock is not used by validatePath; leave null.
    this.requestHandler = rh;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private File invokeValidatePath(byte[] source) throws Exception {
    try {
      return (File) VALIDATE_PATH_METHOD.invoke(requestHandler, (Object) source);
    } catch (java.lang.reflect.InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof Exception) throw (Exception) cause;
      if (cause instanceof Error) throw (Error) cause;
      throw new RuntimeException(cause);
    }
  }

  // ====================================================================
  // Legacy "../" rejections (still enforced before the guard)
  // ====================================================================

  @Test
  @DisplayName("validatePath: rejects exact '..' token (legacy check)")
  void rejectsExactDotDot() {
    assertThrows(
        Exception.class,
        () -> invokeValidatePath("..".getBytes(StandardCharsets.UTF_8)),
        "Exact '..' must be rejected");
  }

  @Test
  @DisplayName("validatePath: rejects '../' traversal (legacy check)")
  void rejectsDotDotSlash() {
    assertThrows(
        Exception.class,
        () -> invokeValidatePath("../evil".getBytes(StandardCharsets.UTF_8)),
        "'../' traversal must be rejected");
  }

  @Test
  @DisplayName("validatePath: rejects embedded '../' anywhere (legacy check)")
  void rejectsEmbeddedTraversal() {
    assertThrows(
        Exception.class,
        () -> invokeValidatePath("a/b/../c".getBytes(StandardCharsets.UTF_8)),
        "Any '../' substring must be rejected");
  }

  // ====================================================================
  // Guard-enforced cases (NUL, absolute escapes that slip the legacy strip)
  // ====================================================================

  @Test
  @DisplayName("validatePath: rejects NUL byte (PSPathInjectionGuard)")
  void rejectsNulByte() {
    String withNul = "good" + '\u0000' + "name.txt";
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeValidatePath(withNul.getBytes(StandardCharsets.UTF_8)),
        "NUL byte must be rejected by PSPathInjectionGuard.requireUnderBase");
  }

  @Test
  @DisplayName(
      "validatePath: rejects absolute path that survives the single leading-/ strip (guard)")
  void rejectsAbsoluteAfterSingleLeadingSlashStrip() throws Exception {
    // Construct an absolute path known to be outside the virtual root, then
    // prefix a single '/'. After the daemon's legacy single leading-/ strip
    // the input is still absolute (drive letter on Windows, or / on Unix).
    // Pre-fix: the legacy checks would not catch it and new File(m_pathRoot, abs)
    // would have produced a File outside the root. Post-fix: the guard rejects.
    Path outside = pathRoot.toPath().getParent().resolve("daemon-escape-target.txt");
    String outsideAbs = outside.toAbsolutePath().toString().replace('\\', '/');
    String payload = "/" + outsideAbs; // e.g. "/C:/.../daemon-escape-target.txt"
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeValidatePath(payload.getBytes(StandardCharsets.UTF_8)),
        "Absolute path that remains absolute after single leading-/ strip must be rejected by"
            + " requireUnderBase");
  }

  /**
   * Unix/macOS: a bare absolute path like {@code /tmp/.../outside.txt} is stripped of exactly one
   * leading {@code /} by legacy daemon logic and becomes a relative segment under {@code
   * pathRoot} (so it would NOT escape). To assert absolute-escape rejection, prefix an extra
   * {@code /} so after the single strip the payload is still absolute and outside the root.
   * Same technique as {@link #rejectsAbsoluteAfterSingleLeadingSlashStrip()}.
   */
  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  @DisplayName(
      "validatePath (Unix/macOS): rejects absolute path outside root after single leading-/ strip")
  void rejectsParentAbsolute_unix() throws Exception {
    File parent = pathRoot.getParentFile();
    String outsideAbs =
        parent.toPath().resolve("outside.txt").toAbsolutePath().toString().replace('\\', '/');
    // outsideAbs starts with '/'; payload is "//tmp/..." so after one strip still absolute.
    String payload = "/" + outsideAbs;
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeValidatePath(payload.getBytes(StandardCharsets.UTF_8)),
        "Unix absolute path that remains absolute after single leading-/ strip must be rejected");
  }

  /**
   * Windows: drive-letter absolute paths (e.g. {@code C:/...}) do not start with {@code /}, so
   * the legacy leading-/ strip is skipped. {@code requireUnderBase} must still reject them when
   * they resolve outside the virtual root.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  @DisplayName("validatePath (Windows): rejects drive-letter absolute path outside root")
  void rejectsParentAbsolute_windows() throws Exception {
    File parent = pathRoot.getParentFile();
    String outsideAbs = parent.toPath().resolve("outside.txt").toAbsolutePath().toString();
    // Keep native Windows form (or slash-normalized); either remains absolute without a leading /
    // after replace('\\','/') in validatePath.
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeValidatePath(outsideAbs.getBytes(StandardCharsets.UTF_8)),
        "Windows drive-letter absolute path outside root must be rejected by requireUnderBase");
  }

  // ====================================================================
  // Happy paths inside the root
  // ====================================================================

  @Test
  @DisplayName("validatePath: accepts simple relative filename")
  void acceptsSimpleName() throws Exception {
    File f = invokeValidatePath("readme.txt".getBytes(StandardCharsets.UTF_8));
    assertNotNull(f);
    String canon = f.getCanonicalPath().replace('\\', '/');
    String rootCanon = pathRoot.getCanonicalPath().replace('\\', '/');
    assertTrue(canon.startsWith(rootCanon), "Returned path must be under the virtual root");
  }

  @Test
  @DisplayName("validatePath: accepts nested relative path")
  void acceptsNestedRelative() throws Exception {
    File f = invokeValidatePath("sub/dir/file.bin".getBytes(StandardCharsets.UTF_8));
    assertNotNull(f);
    String canon = f.getCanonicalPath().replace('\\', '/');
    String rootCanon = pathRoot.getCanonicalPath().replace('\\', '/');
    assertTrue(canon.startsWith(rootCanon), "Nested path must resolve under the virtual root");
  }

  @Test
  @DisplayName("validatePath: accepts path with '..' that stays inside (e.g. sub/.. /x)")
  void acceptsSafeDotDot() throws Exception {
    // "sub/../x" contains "../" so the *legacy* check will reject it.
    // To test a safe ".." usage that the guard would allow, we must avoid
    // the literal "../" substring in the input. Use a name that contains ".."
    // but is not a traversal segment, or test after the legacy check.
    // Here we test a clean name that the guard accepts (the legacy check
    // already passed for names without "../").
    File f = invokeValidatePath("file..name.txt".getBytes(StandardCharsets.UTF_8));
    assertNotNull(f);
    assertTrue(
        f.getCanonicalPath()
            .replace('\\', '/')
            .startsWith(pathRoot.getCanonicalPath().replace('\\', '/')));
  }
}
