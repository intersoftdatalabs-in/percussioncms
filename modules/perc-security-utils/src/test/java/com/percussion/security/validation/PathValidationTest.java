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
    void shouldAcceptSimpleFilename() {
      File result = PathValidation.constructSafePath(baseDir, "document.txt");
      assertTrue(result.getAbsolutePath().contains("document.txt"));
      assertTrue(
          result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()),
          "Result should be within baseDir");
    }

    @Test
    @DisplayName("Should accept subdirectory with forward slashes")
    void shouldAcceptSubdirectoryPath() {
      File result = PathValidation.constructSafePath(baseDir, "themes/css/style.css");
      assertTrue(
          result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()),
          "Result should be within baseDir");
      assertTrue(result.getAbsolutePath().contains("themes"));
      assertTrue(result.getAbsolutePath().contains("css"));
    }

    @Test
    @DisplayName("Should accept common file extensions")
    void shouldAcceptCommonExtensions() {
      String[] extensions = {".txt", ".html", ".css", ".js", ".json", ".xml", ".pdf"};
      for (String ext : extensions) {
        File result =
            PathValidation.constructSafePath(baseDir, "document" + ext);
        assertTrue(
            result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()),
            "Should accept " + ext);
      }
    }

    @Test
    @DisplayName("Should combine multiple safe path components")
    void shouldCombineMultipleSafeComponents() {
      File result =
          PathValidation.combineSafePaths(baseDir, "themes", "dark-mode", "style.css");
      assertTrue(result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()));
      assertTrue(result.getAbsolutePath().contains("themes"));
      assertTrue(result.getAbsolutePath().contains("dark-mode"));
      assertTrue(result.getAbsolutePath().contains("style.css"));
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
    void shouldAcceptValidFilenameCharacters() {
      File result =
          PathValidation.constructSafePath(baseDir, "my-file_v2.0_backup.txt");
      assertTrue(result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()));
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
          () ->
              PathValidation.combineSafePaths(baseDir, "safe1", "safe2", "../escape", "file.txt"),
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
            () ->
                PathValidation.constructSafePath(baseDir, "link", true),
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
              PathValidation.constructSafePath(baseDir, "../../../../../Windows/System32/config/SAM"));
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
    void shouldAllowSafeThemeFileAccess() {
      // Legitimate: user uploads theme named "my-theme" with file "style.css"
      File result =
          PathValidation.constructSafePath(baseDir, "my-theme/css/style.css");
      assertTrue(result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()));
      assertTrue(result.getPath().contains("my-theme"));
    }

    @Test
    @DisplayName("Real scenario: Safe archive extraction")
    void shouldAllowSafeArchiveExtraction() {
      // Legitimate: extract zip with entry "app/index.html"
      File result = PathValidation.constructSafePath(baseDir, "app/index.html");
      assertTrue(result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()));
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
          IllegalArgumentException.class,
          () -> PathValidation.constructSafePath(null, "file.txt"));
    }

    @Test
    @DisplayName("Should reject null user path")
    void shouldRejectNullUserPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PathValidation.constructSafePath(baseDir, null));
    }

    @Test
    @DisplayName("Should reject empty user path")
    void shouldRejectEmptyUserPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PathValidation.constructSafePath(baseDir, ""));
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
          IllegalArgumentException.class,
          () -> PathValidation.constructSafePath(baseDir, "   "));
    }

    @Test
    @DisplayName("Should handle very long paths")
    void shouldHandleLongPaths() {
      StringBuilder longPath = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        longPath.append("dir").append(i).append("/");
      }
      longPath.append("file.txt");

      // Should not throw for legitimate long paths
      File result = PathValidation.constructSafePath(baseDir, longPath.toString());
      assertTrue(result.getAbsolutePath().startsWith(baseDir.getAbsolutePath()));
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
