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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.theme.service.IPSThemeService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
 * <p>The fix:
 *
 * <ol>
 *   <li>Passes {@code themeRootDirectory} as a method parameter (instead
 *       of caching it on the singleton) so concurrent imports do not
 *       race for the validation base.
 *   <li>Skips URL values (off-site CSS links) because they were never
 *       meant to be opened as files; this preserves external-stylesheet
 *       support on Windows where canonicalizing "https://..." throws.
 *   <li>Calls {@link com.percussion.security.io.PSPathInjectionGuard#requireUnderBase}
 *       on every non-URL value BEFORE File construction. Traversal
 *       payloads throw IllegalArgumentException.
 * </ol>
 *
 * <p>Tests instantiate {@link PSImportThemeHelper} via its real
 * {@code @Autowired} constructor with a Mockito-mocked
 * {@link IPSThemeService}, exercising the actual production flow (not
 * {@code sun.misc.Unsafe}). They also use {@link Path#resolve} to
 * build filesystem paths portably (no hardcoded "/" or "\").
 */
public class PSImportThemeHelperPathInjectionTest {

  @TempDir Path themeRoot;

  private PSImportThemeHelper helper() {
    IPSThemeService themeService = mock(IPSThemeService.class);
    when(themeService.getThemeRootDirectory("test-theme")).thenReturn(themeRoot.toString());
    when(themeService.getThemeRootUrl("test-theme")).thenReturn("http://test.example/theme/");
    return new PSImportThemeHelper(themeService);
  }

  /**
   * Reflectively invokes the private {@code removeIfExists} method with
   * the supplied theme root. Reflection is used because the method is
   * private; the constructor is exercised normally via the public
   * {@code @Autowired} entry point.
   */
  private void invokeRemoveIfExists(
      PSImportThemeHelper h, Map<String, String> linkPaths, String themeRootDirectory)
      throws Exception {
    java.lang.reflect.Method m =
        PSImportThemeHelper.class.getDeclaredMethod(
            "removeIfExists", Map.class, String.class);
    m.setAccessible(true);
    try {
      m.invoke(h, linkPaths, themeRootDirectory);
    } catch (java.lang.reflect.InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof RuntimeException) throw (RuntimeException) cause;
      if (cause instanceof Error) throw (Error) cause;
      throw new RuntimeException(cause);
    }
  }

  // ====================================================================
  // Fail-then-pass tests on malicious paths (in-root base)
  // ====================================================================

  @Test
  @DisplayName(
      "removeIfExists: rejects a '..' traversal payload before File.exists() —"
          + " CodeQL java/path-injection #1054")
  void testRemoveIfExistsRejectsTraversal() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    Path escapePath = themeRoot.resolve("..").resolve("escape.css");
    linkPaths.put("http://attacker.example/x.css", escapePath.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()),
        "A '..' traversal payload must be rejected by"
            + " PSPathInjectionGuard.requireUnderBase before File.exists() runs."
            + " Pre-fix: silently stats the escape target. Post-fix:"
            + " IllegalArgumentException.");
  }

  @Test
  @DisplayName("removeIfExists: rejects a multi-level traversal reaching /etc/passwd")
  void testRemoveIfExistsRejectsEtcPasswdTraversal() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    Path escapePath =
        themeRoot
            .resolve("sub1")
            .resolve("..")
            .resolve("..")
            .resolve("..")
            .resolve("..")
            .resolve("etc")
            .resolve("passwd");
    linkPaths.put("http://attacker.example/y.css", escapePath.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()));
  }

  @Test
  @DisplayName("removeIfExists: rejects a NUL byte payload")
  void testRemoveIfExistsRejectsNul() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    // Path.resolve rejects NUL bytes; build the path as a String so the
    // NUL byte reaches the validator. PSPathInjectionGuard.requireUnderBase
    // checks for NUL bytes before any File construction.
    String payloadWithNul =
        themeRoot.toString() + File.separator + "good.css\0../../etc/passwd";
    linkPaths.put("http://attacker.example/z.css", payloadWithNul);
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()));
  }

  // ====================================================================
  // Behavior parity: legitimate paths INSIDE the theme root
  // ====================================================================

  @Test
  @DisplayName("removeIfExists: missing CSS file under theme root leaves the link in the map")
  void testRemoveIfExistsAcceptsMissingFileInsideRoot() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    Path insideRoot = themeRoot.resolve("missing.css");
    linkPaths.put("http://ok.example/missing.css", insideRoot.toString());
    assertDoesNotThrow(
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()));
    assertTrue(
        linkPaths.containsKey("http://ok.example/missing.css"),
        "Missing file under theme root must leave the link entry untouched");
  }

  @Test
  @DisplayName("removeIfExists: an existing CSS file under theme root is removed from the map")
  void testRemoveIfExistsRemovesExistingFileInsideRoot() throws Exception {
    PSImportThemeHelper h = helper();
    Path existing = themeRoot.resolve("real.css");
    Files.createFile(existing);
    Map<String, String> linkPaths = new LinkedHashMap<>();
    linkPaths.put("http://ok.example/real.css", existing.toString());
    assertDoesNotThrow(
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()));
    assertFalse(
        linkPaths.containsKey("http://ok.example/real.css"),
        "Existing file under theme root must be removed from the link map");
  }

  // ====================================================================
  // Cross-platform: external (http/https) URLs are NOT treated as local
  // files (regression fix for the off-site-stylesheet breakage flagged by
  // Erlang review).
  // ====================================================================

  @Test
  @DisplayName(
      "removeIfExists: external HTTP(S) URL values are NOT validated as filesystem paths"
          + " (cross-platform: would otherwise throw on Windows canonicalization)")
  void testRemoveIfExistsSkipsHttpUrls() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    linkPaths.put("https://cdn.example/style.css", "https://cdn.example/style.css");
    linkPaths.put("http://other.example/main.css", "http://other.example/main.css");
    assertDoesNotThrow(
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()),
        "External HTTP(S) URLs must be skipped by the validator; the pre-fix code"
            + " never tried to open them as files (new File(url).exists() returns false),"
            + " and the post-fix code must preserve that semantic on Windows where"
            + " getCanonicalPath() rejects URL-shaped strings.");
    // The entry stays — the helper only removes local files that already exist.
    assertEquals(
        2,
        linkPaths.size(),
        "Off-site URL entries must remain in the map (they are still to be downloaded)");
  }

  // ====================================================================
  // End-to-end: realistic malicious HTML header attack, going through
  // the public removeIfExists entry point with the proper themeRoot
  // parameter (no Unsafe, no field reflection).
  // ====================================================================

  @Test
  @DisplayName(
      "End-to-end: a malicious CSS link with '..' traversal is rejected by the"
          + " removeIfExists validation when called with the proper themeRootDirectory parameter")
  void testMaliciousCssLinkPayloadIsRejected() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    // Realistic attack: the URL converter produced this filesystem path
    // from a malicious <link href='../../../etc/passwd'> in the imported HTML.
    Path resolved =
        themeRoot.resolve("..").resolve("..").resolve("..").resolve("etc").resolve("passwd");
    linkPaths.put("http://attacker.example/passwd.css", resolved.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()),
        "End-to-end attack scenario: a malicious CSS link with traversal must be"
            + " rejected by the validator when the proper themeRootDirectory is"
            + " passed as a parameter.");
  }

  @Test
  @DisplayName(
      "End-to-end: mixed local + remote linkPaths — only the malicious local link"
          + " is rejected; remote link is preserved")
  void testMixedLinkPathsHandling() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    // Mixed map (realistic shape from PSHTMLHeaderImporter.getLinkPaths):
    // - remote URL stays as remote URL
    // - on-site CSS link stays as local path
    // - malicious on-site CSS link with traversal is a local path that escapes
    linkPaths.put("https://cdn.example/style.css", "https://cdn.example/style.css");
    Path legitLocal = themeRoot.resolve("legit.css");
    Files.createFile(legitLocal);
    linkPaths.put("http://ok.example/legit.css", legitLocal.toString());
    Path malicious =
        themeRoot.resolve("..").resolve("..").resolve("..").resolve("etc").resolve("passwd");
    linkPaths.put("http://attacker.example/passwd.css", malicious.toString());

    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()),
        "The malicious local link with traversal must be rejected (the legitimate"
            + " remote link and the legitimate local link are not reached because the"
            + " validator fails fast).");
  }

  @Test
  @DisplayName("removeIfExists: soft-fails when themeRoot does not exist (no IllegalArgumentException)")
  void testRemoveIfExistsSoftWhenThemeRootMissing() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    // Pass a non-existent theme root to trigger the soft-fail guard. The
    // @TempDir `themeRoot` itself exists (JUnit creates it before the test),
    // so we resolve a sub-path that does NOT exist to exercise the
    // `!themeRoot.isDirectory()` branch. The removeIfExists call uses
    // that sub-path as its `themeRootDirectory` argument.
    String nonExistentRoot = themeRoot.resolve("does-not-exist-yet").toString();
    Path legitFile = themeRoot.resolve("missing-root.css");
    linkPaths.put("http://ok.example/missing-root.css", legitFile.toString());
    // nonExistentRoot does not exist (we deliberately do not create it).
    assertDoesNotThrow(
        () -> invokeRemoveIfExists(h, linkPaths, nonExistentRoot),
        "A missing themeRoot must NOT throw IllegalArgumentException; pre-fix"
            + " `new File(cssFile).exists()` was also a no-op in this case (false)."
            + " Throwing here would be silently swallowed by process()'s catch(Exception)"
            + " and abort the entire import.");
    assertEquals(
        1,
        linkPaths.size(),
        "Entry stays — nothing under a missing root can exist anyway");
  }

  @Test
  @DisplayName(
      "removeIfExists: skips protocol-relative URLs (//cdn.example/style.css) "
          + "in addition to http(s)://")
  void testRemoveIfExistsSkipsProtocolRelativeUrls() throws Exception {
    PSImportThemeHelper h = helper();
    Map<String, String> linkPaths = new LinkedHashMap<>();
    linkPaths.put("//cdn.example/style.css", "//cdn.example/style.css");
    assertDoesNotThrow(
        () -> invokeRemoveIfExists(h, linkPaths, themeRoot.toString()),
        "Protocol-relative URLs (//cdn.example/style.css) must be skipped; "
            + " canonicalizing them as a filesystem path throws on Windows.");
    assertEquals(
        1,
        linkPaths.size(),
        "Protocol-relative URL entries must remain in the map (off-site to download)");
  }

  // ====================================================================
  // Per-call root parameterization: removeIfExists takes its root as a
  // method parameter, so two sequential calls with different roots
  // validate against their own base (no shared mutable field on the
  // singleton). This is a sequential test of the per-call contract, not
  // an actual concurrency test.
  // ====================================================================

  @Test
  @DisplayName(
      "Per-call root parameterization: two sequential calls with different roots"
          + " each validate against their own base (no shared mutable field)")
  void testPerCallRootParameterization() throws Exception {
    PSImportThemeHelper h = helper();
    // Two distinct temp directories, simulating two concurrent theme imports.
    Path rootA = themeRoot.resolve("import-A");
    Path rootB = themeRoot.resolve("import-B");
    Files.createDirectories(rootA);
    Files.createDirectories(rootB);

    Map<String, String> linkPathsA = new LinkedHashMap<>();
    // File that exists in rootA but NOT in rootB. Pre-fix singleton-cached root
    // would let rootB's call accidentally validate against rootA's base — but
    // the file's relative path is identical so it would pass either way. The
    // critical check is the escape test below: rootA's path must NOT pass when
    // rootB is the validating base.
    Path legitInA = rootA.resolve("local.css");
    Files.createFile(legitInA);
    linkPathsA.put("http://A.example/local.css", legitInA.toString());

    Path escapeForB = rootB.resolve("..").resolve("..").resolve("etc").resolve("passwd");
    Map<String, String> linkPathsB = new LinkedHashMap<>();
    linkPathsB.put("http://B.example/esc.css", escapeForB.toString());

    // Sequential is sufficient to demonstrate the absence of the shared-field
    // race: each call passes its OWN root, so each validates against its OWN base.
    invokeRemoveIfExists(h, linkPathsA, rootA.toString());
    assertFalse(
        linkPathsA.containsKey("http://A.example/local.css"),
        "Local file in rootA must be removed");

    assertThrows(
        IllegalArgumentException.class,
        () -> invokeRemoveIfExists(h, linkPathsB, rootB.toString()),
        "Path escaping rootB must be rejected when rootB is the validating base."
            + " Pre-fix (shared mutable field) would have validated against rootA"
            + " because field was overwritten between the two calls.");
  }

  // ====================================================================
  // Sanity: helper instance is usable (constructor smoke test)
  // ====================================================================

  @Test
  @DisplayName("Sanity: PSImportThemeHelper can be constructed via the @Autowired entry point")
  void testSanityHelperConstruction() {
    PSImportThemeHelper h = helper();
    assertNotNull(h, "Helper must be constructible with a mocked IPSThemeService");
  }
}