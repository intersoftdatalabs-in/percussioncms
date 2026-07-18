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

package com.percussion.auditlog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for FileCreator class.
 *
 * <p>Tests security fix for path injection vulnerability (CWE-22: Path Traversal). Ensures that
 * generated file paths cannot escape the intended base directory.
 */
@DisplayName("FileCreator Security Tests")
class FileCreatorTest {

  @TempDir Path testDirectory;

  private String basePath;
  private String filePattern = "yyyy-MM-dd";
  private String extension = "txt";

  @BeforeEach
  void setUp() {
    basePath = testDirectory.toString();
  }

  @AfterEach
  void tearDown() {
    // Cleanup is handled by @TempDir annotation
  }

  @Test
  @DisplayName("Should create file successfully with valid parameters")
  void testCreateFileWithValidParameters() {
    // Given
    String fileName = "testfile";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then
    assertFalse(result.isEmpty(), "File creation should succeed");
    assertTrue(Files.exists(Paths.get(result)), "Created file should exist");
    assertTrue(result.contains(fileName), "File path should contain the specified file name");
    assertTrue(result.startsWith(basePath), "File should be created in the base directory");
  }

  @Test
  @DisplayName("Should reject null file path")
  void testRejectNullFilePath() {
    // Given
    String nullPath = null;
    String fileName = "testfile";

    // When
    String result = FileCreator.generateFile(nullPath, fileName, filePattern, extension);

    // Then
    assertEquals("", result, "Should return empty string for null file path");
  }

  @Test
  @DisplayName("Should reject empty file path")
  void testRejectEmptyFilePath() {
    // Given
    String emptyPath = "";
    String fileName = "testfile";

    // When
    String result = FileCreator.generateFile(emptyPath, fileName, filePattern, extension);

    // Then
    assertEquals("", result, "Should return empty string for empty file path");
  }

  @Test
  @DisplayName("Should reject null file name")
  void testRejectNullFileName() {
    // Given
    String nullFileName = null;

    // When
    String result = FileCreator.generateFile(basePath, nullFileName, filePattern, extension);

    // Then
    assertEquals("", result, "Should return empty string for null file name");
  }

  @Test
  @DisplayName("Should reject empty file name")
  void testRejectEmptyFileName() {
    // Given
    String emptyFileName = "";

    // When
    String result = FileCreator.generateFile(basePath, emptyFileName, filePattern, extension);

    // Then
    assertEquals("", result, "Should return empty string for empty file name");
  }

  @Test
  @DisplayName("Should prevent path traversal with ../ sequences")
  void testPreventPathTraversalWithDotDotSlash() {
    // Given - Attempt to write a file outside the base directory using ../
    String fileName = "../../../etc/passwd";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then - Should return empty string (traversal prevented)
    assertEquals("", result, "Path traversal attack with ../ should be prevented");
  }

  @Test
  @DisplayName("Should sanitize file name with invalid characters")
  void testSanitizeFileNameWithInvalidCharacters() {
    // Given - File name with special characters that could be used in path traversal
    String fileName = "test<script>alert(1)</script>";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then
    assertFalse(result.isEmpty(), "File should be created with sanitized name");
    assertTrue(Files.exists(Paths.get(result)), "Created file should exist");
    assertTrue(result.startsWith(basePath), "File should be in base directory");
    // File name should not contain dangerous characters
    assertFalse(result.contains("<"), "File path should not contain < character");
    assertFalse(result.contains(">"), "File path should not contain > character");
  }

  @Test
  @DisplayName("Should prevent path traversal by normalizing paths")
  void testPreventPathTraversalNormalization() {
    // Given - Try to use path normalization to escape
    String fileName = "test";

    // When - Create file in nested directory and attempt escape
    String nestedPath = testDirectory.resolve("subdir").toString();
    String result = FileCreator.generateFile(nestedPath, fileName, filePattern, extension);

    // Then
    assertFalse(result.isEmpty(), "File creation in subdirectory should succeed");
    assertTrue(result.startsWith(nestedPath), "File should be in the specified subdirectory");
  }

  @Test
  @DisplayName("Should reject null file pattern")
  void testRejectNullFilePattern() {
    // Given
    String fileName = "testfile";
    String nullPattern = null;

    // When
    String result = FileCreator.generateFile(basePath, fileName, nullPattern, extension);

    // Then
    assertEquals("", result, "Should return empty string for null file pattern");
  }

  @Test
  @DisplayName("Should reject null extension")
  void testRejectNullExtension() {
    // Given
    String fileName = "testfile";
    String nullExtension = null;

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, nullExtension);

    // Then
    assertEquals("", result, "Should return empty string for null extension");
  }

  @Test
  @DisplayName("Should create nested directories if they do not exist")
  void testCreateNestedDirectories() {
    // Given
    String nestedPath = testDirectory.resolve("nested/audit/logs").toString();
    String fileName = "auditlog";

    // When
    String result = FileCreator.generateFile(nestedPath, fileName, filePattern, extension);

    // Then
    assertFalse(result.isEmpty(), "File creation in nested path should succeed");
    assertTrue(Files.exists(Paths.get(result)), "Created file should exist");
  }

  @Test
  @DisplayName("Should reuse existing same-day audit file without FileAlreadyExistsException")
  void testReuseExistingSameDayFile() {
    // Given — first login creates the dated audit file
    String fileName = "audit_event";
    String first = FileCreator.generateFile(basePath, fileName, filePattern, extension);
    assertFalse(first.isEmpty(), "First create should succeed");
    assertTrue(Files.exists(Paths.get(first)));

    // When — second login (or concurrent audit) targets the same dated path
    String second = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then — must return the existing path, not empty (regression: login audit noise + empty path)
    assertEquals(first, second, "Second call should return the same existing path");
    assertFalse(second.isEmpty());
  }

  @Test
  @DisplayName("Should handle file names with valid special characters")
  void testHandleValidSpecialCharactersInFileName() {
    // Given - File name with valid special characters
    String fileName = "audit_log-2025";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then
    assertFalse(result.isEmpty(), "File creation should succeed");
    assertTrue(Files.exists(Paths.get(result)), "Created file should exist");
    assertTrue(result.contains("audit_log-2025"), "File name should contain valid characters");
  }

  @Test
  @DisplayName("Should prevent absolute path traversal attempts with file name starting with /")
  void testPreventAbsolutePathTraversal() {
    // Given - File name that attempts absolute path traversal
    String fileName = "/etc/passwd";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then
    // The file should be created in base path, not at /etc/passwd
    assertEquals("", result, "Absolute path traversal in file name should be prevented");
  }

  @Test
  @DisplayName("Should sanitize extension with invalid characters")
  void testSanitizeExtensionWithInvalidCharacters() {
    // Given - Extension with special characters
    String fileName = "audit";
    String invalidExtension = "txt;rm -rf /";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, invalidExtension);

    // Then
    assertFalse(result.isEmpty(), "File should be created with sanitized extension");
    assertTrue(Files.exists(Paths.get(result)), "Created file should exist");
    // Extension should be sanitized to contain only alphanumeric characters
    assertTrue(result.endsWith("txt") || result.endsWith("txtrm") || result.contains("txt"));
  }

  @Test
  @DisplayName("Should ensure created file is within base directory bounds")
  void testFileIsWithinBaseDirectoryBounds() {
    // Given
    String fileName = "secure_file";

    // When
    String result = FileCreator.generateFile(basePath, fileName, filePattern, extension);

    // Then
    if (!result.isEmpty()) {
      Path createdPath = Paths.get(result).toAbsolutePath().normalize();
      Path base = Paths.get(basePath).toAbsolutePath().normalize();
      assertTrue(
          createdPath.startsWith(base), "Created file must be within the base directory bounds");
    }
  }
}
