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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.sitemanage.importer.helpers.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

/**
 * Regression tests for {@link PSImportThemeHelper} focused on the
 * {@code java/path-injection} finding closed at
 * {@code PSImportThemeHelper.java:216} (CodeQL alert #1054, T043).
 *
 * <p>The pre-fix code in {@code removeIfExists(Map<String, String>)}
 * called {@code new File(cssFile)} with paths derived from CSS link
 * URLs extracted from the imported HTML header, without verifying the
 * resolved path was contained within the theme root. A malicious HTML
 * header with a {@code <link>} URL pointing outside the theme root
 * could escape the base directory.
 *
 * <p>The fix calls {@link com.percussion.security.io.PSPathInjectionGuard#requireUnderBase}
 * on every cssFile value BEFORE any File construction. Tests use
 * {@link sun.misc.Unsafe#allocateInstance} to bypass the
 * {@link PSImportThemeHelper} constructor (Spring-managed {@code @Lazy}
 * bean with many dependencies) and reflectively invoke the private
 * {@code removeIfExists} method. The same harness is used by
 * {@code PSCSSParserPathInjectionTest} and
 * {@code PSSiteDataServicePathInjectionTest} (T043).
 */
public class PSImportThemeHelperPathInjectionTest {

  @TempDir File themeRoot;

  private static final Unsafe UNSAFE;
  private static final Method REMOVE_IF_EXISTS_METHOD;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      REMOVE_IF_EXISTS_METHOD =
          PSImportThemeHelper.class.getDeclaredMethod(
              "removeIfExists", Map.class);
      REMOVE_IF_EXISTS_METHOD.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Builds an uninitialized PSImportThemeHelper instance and sets
   * {@code themeRootDirectory} via reflection so the validator has a
   * real base directory to check against. The Spring-injected
   * dependencies (themeService, headerImporter, etc.) are unused by the
   * {@code removeIfExists} method being tested.
   */
  private PSImportThemeHelper helper() throws Exception {
    PSImportThemeHelper h =
        (PSImportThemeHelper) UNSAFE.allocateInstance(PSImportThemeHelper.class);
    setField(h, "themeRootDirectory", themeRoot.getAbsolutePath());
    return h;
  }

  private static void setField(Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field f = PSImportThemeHelper.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Object invokeRemoveIfExists(PSImportThemeHelper h, Map<String, String> linkPaths)
      throws Throwable {
    try {
      return REMOVE_IF_EXISTS_METHOD.invoke(h, linkPaths);
    } catch (InvocationTargetException ite) {
      throw ite.getCause();
    }
  }

  // ====================================================================
  // Fail-then-pass tests on malicious paths
  // ====================================================================

  @Test
  @DisplayName(
      "removeIfExists: rejects a '..' traversal payload before new File(path).exists() —"
          + " CodeQL java/path-injection #1054")
  void testRemoveIfExistsRejectsTraversal() throws Throwable {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new HashMap<>();
    String traversal = themeRoot.getAbsolutePath() + "/../escape.css";
    linkPaths.put("http://attacker.example/x.css", traversal);
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths),
        "A '..' traversal payload must be rejected by"
            + " PSPathInjectionGuard.requireUnderBase before new File(path).exists() runs."
            + " Pre-fix: silently checks (or reads) the escape target. Post-fix:"
            + " IllegalArgumentException.");
  }

  @Test
  @DisplayName(
      "removeIfExists: rejects a relative traversal payload reaching /etc/passwd")
  void testRemoveIfExistsRejectsEtcPasswdTraversal() throws Throwable {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new HashMap<>();
    String traversal =
        themeRoot.getAbsolutePath() + "/sub1/../../../../etc/passwd";
    linkPaths.put("http://attacker.example/y.css", traversal);
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths),
        "A traversal payload reaching /etc/passwd must be rejected.");
  }

  @Test
  @DisplayName("removeIfExists: rejects a NUL byte payload")
  void testRemoveIfExistsRejectsNul() throws Throwable {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new HashMap<>();
    linkPaths.put("http://attacker.example/z.css", "good.css\0../../etc/passwd");
    assertThrows(
        IllegalArgumentException.class, () -> invokeRemoveIfExists(h, linkPaths));
  }

  // ====================================================================
  // Behavior parity: paths INSIDE the theme root still work
  // ====================================================================

  @Test
  @DisplayName(
      "removeIfExists: a missing CSS file under theme root leaves the link in the map"
          + " (validator passed; File construction completed; file absent)")
  void testRemoveIfExistsAcceptsMissingFileInsideRoot() throws Throwable {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new HashMap<>();
    String insideRoot = new File(themeRoot, "missing.css").getAbsolutePath();
    linkPaths.put("http://ok.example/missing.css", insideRoot);
    assertDoesNotThrow(() -> invokeRemoveIfExists(h, linkPaths));
    assertTrue(
        linkPaths.containsKey("http://ok.example/missing.css"),
        "Missing file under theme root must leave the link entry untouched");
  }

  @Test
  @DisplayName(
      "removeIfExists: an existing CSS file under theme root is removed from the map")
  void testRemoveIfExistsRemovesExistingFileInsideRoot() throws Throwable {
    PSImportThemeHelper h = helper();
    File existing = new File(themeRoot, "real.css");
    assertTrue(existing.createNewFile(), "Pre-condition: test fixture file must be creatable");
    Map<String, String> linkPaths = new HashMap<>();
    linkPaths.put("http://ok.example/real.css", existing.getAbsolutePath());
    assertDoesNotThrow(() -> invokeRemoveIfExists(h, linkPaths));
    assertTrue(
        !linkPaths.containsKey("http://ok.example/real.css"),
        "Existing file under theme root must be removed from the link map");
  }

  // ====================================================================
  // End-to-end: realistic malicious HTML header attack
  // ====================================================================

  @Test
  @DisplayName(
      "End-to-end: a malicious HTML <link href='../../../etc/passwd'> payload is rejected"
          + " before any File construction (the actual alert scenario)")
  void testMaliciousHtmlHeaderPayloadIsRejected() throws Throwable {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new HashMap<>();
    // Realistic attack vector: the HTML header contains a <link> tag whose
    // href attribute is a path-traversal payload. PSHTMLHeaderImporter
    // converts that to a filesystem path under themeRoot, which
    // removeIfExists then attempts to stat.
    String resolved =
        new File(themeRoot, "../../../etc/passwd").getAbsolutePath();
    linkPaths.put("http://attacker.example/passwd.css", resolved);
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths),
        "End-to-end attack scenario: a malicious <link> with traversal must be"
            + " rejected by the validator BEFORE File construction.");
  }
}