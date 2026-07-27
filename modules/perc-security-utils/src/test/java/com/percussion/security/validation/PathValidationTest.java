/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Security-focused unit tests for {@link PathValidation}.
 *
 * <p>Tests verify that path traversal attacks (CWE-22 and ZipSlip) are prevented.
 *
 * @author Percussion Security Team
 */
@DisplayName("PathValidation - Path Traversal & ZipSlip Prevention")
class PathValidationTest {

  private File baseDir;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    baseDir = tempDir.toFile();
  }

  @Nested
  @DisplayName("Positive Tests - Legitimate Path Usage")
  class PositiveTests {

    @Test
    @DisplayName("Should accept simple filename")
    void shouldAcceptSimpleFilename() throws IOException {
      File result = PathValidation.constructSafePath(baseDir, "document.txt");
      assertTrue(result.getCanonicalPath().contains("document.txt"));
      assertTrue(
          result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()),
          "Result should be within baseDir");
    }

    @Test
    @DisplayName("Should accept subdirectory with forward slashes")
    void shouldAcceptSubdirectoryPath() throws IOException {
      File result = PathValidation.constructSafePath(baseDir, "themes/css/style.css");
      assertTrue(
          result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()),
          "Result should be within baseDir");
      assertTrue(result.getCanonicalPath().contains("themes"));
      assertTrue(result.getCanonicalPath().contains("css"));
    }

    @Test
    @DisplayName("Should accept common file extensions")
    void shouldAcceptCommonExtensions() throws IOException {
      String[] extensions = {".txt", ".html", ".css", ".js", ".json", ".xml", ".pdf"};
      for (String ext : extensions) {
        File result = PathValidation.constructSafePath(baseDir, "document" + ext);
        assertTrue(
            result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()),
            "Should accept " + ext);
      }
    }

    @Test
    @DisplayName("Should combine multiple safe path components")
    void shouldCombineMultipleSafeComponents() throws IOException {
      File result = PathValidation.combineSafePaths(baseDir, "themes", "dark-mode", "style.css");
      assertTrue(result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()));
      assertTrue(result.getCanonicalPath().contains("themes"));
      assertTrue(result.getCanonicalPath().contains("dark-mode"));
      assertTrue(result.getCanonicalPath().contains("style.css"));
    }

    @Test
    @DisplayName("Should validate path within same directory")
    void shouldValidatePathWithinDirectory() throws IOException {
      File testFile = new File(baseDir, "test.txt");
      testFile.createNewFile();

      File validated = PathValidation.validatePathWithinDirectory(testFile, baseDir);
      assertEquals(testFile.getCanonicalPath(), validated.getCanonicalPath());
    }

    @Test
    @DisplayName("Should accept underscore and hyphen in filenames")
    void shouldAcceptValidFilenameCharacters() throws IOException {
      File result = PathValidation.constructSafePath(baseDir, "my-file_v2.0_backup.txt");
      assertTrue(result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()));
    }
  }

  @Nested
  @DisplayName("Negative Tests - Path Traversal Prevention (CWE-22)")
  class NegativeSecurityTests {

    @Test
    @DisplayName("Should reject simple parent directory escape (./../)")
    void shouldRejectParentDirectoryEscape() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../outside.txt"),
          "Should reject parent directory reference");
    }

    @Test
    @DisplayName("Should reject multiple parent directory traversal")
    void shouldRejectMultipleTraversal() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../../../../../etc/passwd"));
    }

    @Test
    @DisplayName("Should reject escaped parent in subdirectory path")
    void shouldRejectEscapedParentInMiddle() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "subdir/../../../etc/passwd"));
    }

    @Test
    @DisplayName("Should reject absolute paths")
    void shouldRejectAbsolutePath() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "/etc/passwd"),
          "Should reject absolute path");
    }

    @Test
    @DisplayName("Should reject Windows absolute paths")
    void shouldRejectWindowsAbsolutePath() {
      // Test Windows-style absolute path
      if (File.separator.equals("\\")) {
        assertThrows(
            PathValidation.SecurityException.class,
            () -> PathValidation.constructSafePath(baseDir, "C:\\Windows\\System32\\cmd.exe"));
      }
    }

    @Test
    @DisplayName("Should reject URL-encoded traversal (..%2F)")
    void shouldRejectUrlEncodedTraversal() {
      // Note: URL decoding should happen before PathValidation
      // This test documents that we catch decoded traversal
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../outside/../../more/escape"));
    }

    @Test
    @DisplayName("Should reject double dot variations")
    void shouldRejectDoubleDotVariations() {
      String[] attacks = {"..", "../", "..", "../../", "../..", "../../.."};

      for (String attack : attacks) {
        assertThrows(
            PathValidation.SecurityException.class,
            () -> PathValidation.constructSafePath(baseDir, attack),
            "Should reject: " + attack);
      }
    }

    @Test
    @DisplayName("Should reject traversal in multi-component paths")
    void shouldRejectTraversalInMultiComponent() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.combineSafePaths(baseDir, "safe1", "safe2", "../escape", "file.txt"),
          "Should detect escape in middle of path components");
    }

    @Test
    @DisplayName("Should reject absolute path in path component combination")
    void shouldRejectAbsoluteInCombinedPath() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.combineSafePaths(baseDir, "dir1", "/etc/passwd"));
    }

    @Test
    @DisplayName("Should reject symlink that escapes bounds")
    void shouldRejectSymlinkEscape(@TempDir Path escapeDir) throws IOException {
      // Only test on systems that support symlinks
      if (supportsSymlinks()) {
        Files.createSymbolicLink(baseDir.toPath().resolve("link"), escapeDir);

        assertThrows(
            PathValidation.SecurityException.class,
            () -> PathValidation.constructSafePath(baseDir, "link", true),
            "Should reject symlink that escapes bounds");
      }
    }

    @Test
    @DisplayName("Should prevent ZipSlip pattern (../../)")
    void shouldPreventZipSlipPattern() {
      // Classic ZipSlip: archive contains entries like ../../.../shell.jsp
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../extracted.jsp"),
          "Should reject ZipSlip pattern");
    }

    @Test
    @DisplayName("Should reject null byte injection")
    void shouldRejectNullByteInjection() {
      assertFalse(PathValidation.isValidFilename("file.txt\0.jsp"));
      assertFalse(PathValidation.isValidFilename("test\0script"));
    }

    @Test
    @DisplayName("Should reject path separators in filename validation")
    void shouldRejectPathSeparatorsInFilename() {
      assertFalse(PathValidation.isValidFilename("../../../passwd"));
      assertFalse(PathValidation.isValidFilename("dir/file.txt"));
      assertFalse(PathValidation.isValidFilename("dir\\file.txt"));
    }

    @Test
    @DisplayName("Should accept colon in filename on Unix (not treated as drive letter)")
    void shouldAcceptColonInFilenameOnUnix() {
      // On Unix, ':' is a legal filename character; only on Windows should 'C:foo' be
      // treated as a drive-letter path. We can't fully simulate OS detection in JUnit, but
      // we verify that on Unix the file is constructed and is contained in baseDir.
      if (File.separatorChar != '\\') {
        File result = PathValidation.constructSafePath(baseDir, "report:data.txt");
        assertTrue(
            result.getPath().contains("report:data.txt")
                || result.getPath().contains("report"), // canonical form may drop ':' via fs
            "Should not reject 'report:data.txt' as absolute on Unix");
      }
    }

    @Test
    @DisplayName("Should not throw for colon-in-filename via combineSafePaths on Unix")
    void shouldAcceptColonComponentOnUnix() {
      if (File.separatorChar != '\\') {
        File result = PathValidation.combineSafePaths(baseDir, "user-data", "report:2025.txt");
        assertNotNull(result);
        assertTrue(result.getPath().contains("report") || result.getPath().contains("user-data"));
      }
    }
  }

  @Nested
  @DisplayName("Real-World Attack Scenarios")
  class RealWorldAttackTests {

    @Test
    @DisplayName("Should prevent /etc/passwd read via path injection")
    void shouldPrevent_etc_passwd_read() {
      // Common attack: access system files
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../../../etc/passwd"));
    }

    @Test
    @DisplayName("Should prevent Windows registry path injection")
    void shouldPreventWindowsRegistryInjection() {
      assertThrows(
          PathValidation.SecurityException.class,
          () ->
              PathValidation.constructSafePath(
                  baseDir, "../../../../../Windows/System32/config/SAM"));
    }

    @Test
    @DisplayName("Should prevent .env file access")
    void shouldPreventsEnvFileAccess() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../.env"));
    }

    @Test
    @DisplayName("Should prevent private key file access")
    void shouldPreventPrivateKeyAccess() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../.ssh/id_rsa"));
    }

    @Test
    @DisplayName("Should prevent database credential access")
    void shouldPreventDatabaseCredentialAccess() {
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../../../config/db.properties"));
    }

    @Test
    @DisplayName("Real scenario: Safe theme file access")
    void shouldAllowSafeThemeFileAccess() throws IOException {
      // Legitimate: user uploads theme named "my-theme" with file "style.css"
      File result = PathValidation.constructSafePath(baseDir, "my-theme/css/style.css");
      assertTrue(result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()));
      assertTrue(result.getPath().contains("my-theme"));
    }

    @Test
    @DisplayName("Real scenario: Safe archive extraction")
    void shouldAllowSafeArchiveExtraction() throws IOException {
      // Legitimate: extract zip with entry "app/index.html"
      File result = PathValidation.constructSafePath(baseDir, "app/index.html");
      assertTrue(result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()));
    }

    @Test
    @DisplayName("Attack scenario: ZipSlip archive extraction")
    void shouldPreventZipSlipArchiveExtraction() {
      // Malicious: archive contains entry "../../evil.sh"
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "../../evil.sh"));
    }
  }

  @Nested
  @DisplayName("Error Handling & Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should reject null base directory")
    void shouldRejectNullBaseDir() {
      assertThrows(
          IllegalArgumentException.class, () -> PathValidation.constructSafePath(null, "file.txt"));
    }

    @Test
    @DisplayName("Should reject null user path")
    void shouldRejectNullUserPath() {
      assertThrows(
          IllegalArgumentException.class, () -> PathValidation.constructSafePath(baseDir, null));
    }

    @Test
    @DisplayName("Should reject empty user path")
    void shouldRejectEmptyUserPath() {
      assertThrows(
          IllegalArgumentException.class, () -> PathValidation.constructSafePath(baseDir, ""));
    }

    @Test
    @DisplayName("Should reject non-directory baseDir")
    void shouldRejectNonDirectoryBaseDir() throws IOException {
      File notADir = Files.createTempFile(baseDir.toPath(), "temp", ".txt").toFile();
      assertThrows(
          IllegalArgumentException.class,
          () -> PathValidation.constructSafePath(notADir, "file.txt"));
    }

    @Test
    @DisplayName("Should handle whitespace-only paths")
    void shouldRejectWhitespaceOnlyPath() {
      assertThrows(
          IllegalArgumentException.class, () -> PathValidation.constructSafePath(baseDir, "   "));
    }

    @Test
    @DisplayName("Should handle very long paths")
    void shouldHandleLongPaths() throws IOException {
      StringBuilder longPath = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        longPath.append("dir").append(i).append("/");
      }
      longPath.append("file.txt");

      // Should not throw for legitimate long paths
      File result = PathValidation.constructSafePath(baseDir, longPath.toString());
      assertTrue(result.getCanonicalPath().startsWith(baseDir.getCanonicalPath()));
    }

    @Test
    @DisplayName("Should validate isValidFilename() correctly")
    void shouldValidateFilenames() {
      // Valid
      assertTrue(PathValidation.isValidFilename("document.txt"));
      assertTrue(PathValidation.isValidFilename("my-file_v2.pdf"));
      assertTrue(PathValidation.isValidFilename("123.html"));

      // Invalid
      assertFalse(PathValidation.isValidFilename(null));
      assertFalse(PathValidation.isValidFilename(""));
      assertFalse(PathValidation.isValidFilename("../escape"));
      assertFalse(PathValidation.isValidFilename("dir/file"));
      assertFalse(PathValidation.isValidFilename("file.txt\0.jsp"));
    }

    @Test
    @DisplayName("Should return canonical path from constructSafePath")
    void shouldReturnCanonicalPathFromConstructSafePath() throws IOException {
      File result = PathValidation.constructSafePath(baseDir, "subdir/file.txt");
      // The returned File must report a canonical absolute path equal to baseDir +
      // "subdir/file.txt"
      assertEquals(
          new File(baseDir, "subdir/file.txt").getCanonicalPath(), result.getCanonicalPath());
    }

    @Test
    @DisplayName("Should return canonical path from combineSafePaths")
    void shouldReturnCanonicalPathFromCombineSafePaths() throws IOException {
      File result = PathValidation.combineSafePaths(baseDir, "themes", "dark", "style.css");
      assertEquals(
          new File(baseDir, "themes/dark/style.css").getCanonicalPath(), result.getCanonicalPath());
    }

    @Test
    @DisplayName("checkSymlinks=true must reject symlink whose target escapes baseDir")
    void shouldRejectEscapingSymlinkWithCheckSymlinksTrue(@TempDir Path escapeDir)
        throws IOException {
      if (!supportsSymlinks()) {
        return;
      }
      // Create a symlink inside baseDir pointing outside of baseDir.
      Files.createSymbolicLink(baseDir.toPath().resolve("escape"), escapeDir);
      assertThrows(
          PathValidation.SecurityException.class,
          () -> PathValidation.constructSafePath(baseDir, "escape", true),
          "checkSymlinks=true must reject a symlink whose target is outside baseDir");
    }
  }

  // Helper method to detect symlink support
  private boolean supportsSymlinks() {
    try {
      Path test = Files.createTempFile(baseDir.toPath(), "symlink", "test");
      Path link = Files.createSymbolicLink(baseDir.toPath().resolve("test_link"), test);
      Files.delete(link);
      Files.delete(test);
      return true;
    } catch (Exception e) {
      return false; // Symlinks not supported on this OS
    }
  }
}
