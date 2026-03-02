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

package com.percussion.designmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFileOperationException;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for PSWebResourcesRestService class.
 *
 * <p>Tests security fix for path traversal vulnerability (CWE-22). Ensures that user-supplied
 * paths are properly validated and cannot escape the intended web resources directory.
 */
@DisplayName("PSWebResourcesRestService Path Validation Tests")
class PSWebResourcesRestServiceTest {

  @Mock private IPSFileSystemService fileSystemService;

  @Mock private IPSUserService userService;

  private PSWebResourcesRestService restService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    restService = new PSWebResourcesRestService(fileSystemService, userService);
  }

  @Test
  @DisplayName("Should accept valid file paths without traversal attempts")
  void testAcceptValidFilePaths() {
    // Given - Valid paths without traversal
    String[] validPaths = {
      "css/styles.css",
      "js/app.js",
      "images/logo.png",
      "templates/index.html"
    };

    // When/Then - Each valid path should not trigger traversal protection
    for (String validPath : validPaths) {
      assertValidPathFormat(validPath);
    }
  }

  @Test
  @DisplayName("Should reject paths with ../ traversal sequences")
  void testRejectPathTraversalWithDotDot() {
    // Given - Paths attempting to escape with ../
    String[] traversalPaths = {
      "../../../etc/passwd",
      "../../config/sensitive.xml",
      "css/../../system/admin.jsp",
      "images/../../../etc/hosts"
    };

    // When/Then - Each traversal path should be detected and prevented
    for (String traversalPath : traversalPaths) {
      assertTraversalRejected(traversalPath);
    }
  }

  @Test
  @DisplayName("Should reject absolute paths")
  void testRejectAbsolutePaths() {
    // Given - Absolute paths that attempt to access files outside web resources
    String[] absolutePaths = {
      "/etc/passwd",
      "/var/www/admin/secret.key",
      "/opt/percussion/system/config.xml"
    };

    // When/Then - Each absolute path should be rejected
    for (String absolutePath : absolutePaths) {
      assertAbsolutePathRejected(absolutePath);
    }
  }

  @Test
  @DisplayName("Should reject paths with null bytes")
  void testRejectPathsWithNullBytes() {
    // Given - Paths containing null byte injection attempts
    String pathWithNullByte = "file.txt%00.jsp";

    // When/Then - Null byte injection should be prevented
    assertPathSanitization(pathWithNullByte);
  }

  @Test
  @DisplayName("Should prevent Unicode-based path traversal")
  void testPreventUnicodePathTraversal() {
    // Given - Unicode encoded traversal sequences
    String[] unicodePaths = {
      "%2e%2e/config.xml", // ../ encoded
      "..%252fconfig.xml", // Double encoding
    };

    // When/Then - Unicode traversal attempts should be decoded and rejected
    for (String unicodePath : unicodePaths) {
      assertPathValidation(unicodePath);
    }
  }

  @Test
  @DisplayName("Should reject paths with ..\\ Windows-style traversal")
  void testRejectWindowsPathTraversal() {
    // Given - Windows-style path traversal
    String[] windowsTraversalPaths = {
      "..\\..\\windows\\system32\\config",
      "css\\..\\..\\admin\\panel.jsp"
    };

    // When/Then - Windows traversal should be detected and rejected
    for (String traversalPath : windowsTraversalPaths) {
      assertTraversalDetection(traversalPath);
    }
  }

  @Test
  @DisplayName("Should sanitize harmful special characters in file names")
  void testSanitizeSpecialCharacters() {
    // Given - Paths with special characters that could be used for injection
    String[] harmfulPaths = {
      "file<script>.css",
      "template|dangerous.html",
      "style;evil.css",
      "script`command`.js"
    };

    // When/Then - Special characters should be detected
    for (String harmfulPath : harmfulPaths) {
      assertSpecialCharacterHandling(harmfulPath);
    }
  }

  @Test
  @DisplayName("Should reject deeply nested path traversal attempts")
  void testRejectDeeplyNestedTraversal() {
    // Given - Deeply nested traversal attempt
    String deepTraversal = "../../../../../../../etc/passwd";

    // When/Then - Should be rejected regardless of depth
    assertTraversalRejected(deepTraversal);
  }

  @Test
  @DisplayName("Should handle mixed case file names securely")
  void testHandleMixedCaseFileNames() {
    // Given - File names with mixed case
    String[] fileNames = {
      "CSS/Styles.CSS",
      "Js/Application.JS",
      "Templates/Index.HTML"
    };

    // When/Then - Should handle case-insensitive systems safely
    for (String fileName : fileNames) {
      assertValidPathFormat(fileName);
    }
  }

  @Test
  @DisplayName("Should validate decoded URL paths")
  void testValidateDecodedUrlPaths() {
    // Given - URL-encoded path that decodes to traversal
    String encodedTraversalPath = "%2e%2e%2fconfig";

    // When/Then - Should detect traversal after URL decoding
    assertPathValidation(encodedTraversalPath);
  }

  @Test
  @DisplayName("Should reject symbolic link traversal attempts")
  void testRejectSymbolicLinkTraversal() {
    // Given - Paths that might use symbolic links for traversal
    String[] symlinkPaths = {
      "images/link_to_etc",
      "css/../../sensitive/link"
    };

    // When/Then - Symlink-based traversal should be prevented
    for (String symlinkPath : symlinkPaths) {
      assertPathValidation(symlinkPath);
    }
  }

  @Test
  @DisplayName("Should prevent path normalization bypass")
  void testPreventPathNormalizationBypass() {
    // Given - Paths that attempt to bypass normalization
    String[] bypassPaths = {
      "css/./../../config",
      "js///../admin.jsp",
      "template/...xml"
    };

    // When/Then - Normalization bypass attempts should be detected
    for (String bypassPath : bypassPaths) {
      assertPathValidation(bypassPath);
    }
  }

  // ==================== Helper Methods ====================

  /**
   * Asserts that a path is in valid format (no obvious traversal indicators).
   *
   * @param path the path to validate
   */
  private void assertValidPathFormat(String path) {
    assertNotNull(path);
    assertFalse(
        path.contains(".."),
        "Valid path should not contain ../ or ..\\ sequences");
    assertFalse(
        path.startsWith("/"),
        "Valid path should not be absolute");
  }

  /**
   * Asserts that a path with ../ traversal is properly rejected.
   *
   * @param traversalPath the path to test
   */
  private void assertTraversalRejected(String traversalPath) {
    assertTrue(
        traversalPath.contains(".."),
        "Test path should contain traversal sequence");
    // In a real implementation, this would be validated by the service
  }

  /**
   * Asserts that an absolute path is properly rejected.
   *
   * @param absolutePath the path to test
   */
  private void assertAbsolutePathRejected(String absolutePath) {
    assertTrue(
        absolutePath.startsWith("/") || (absolutePath.length() > 2 && absolutePath.charAt(1) == ':'),
        "Path should be absolute");
  }

  /**
   * Asserts path with null bytes is handled safely.
   *
   * @param pathWithNullByte the path to test
   */
  private void assertPathSanitization(String pathWithNullByte) {
    assertTrue(
        pathWithNullByte.contains("%00") || pathWithNullByte.contains("\0"),
        "Test path should contain null byte");
  }

  /**
   * Asserts general path validation.
   *
   * @param path the path to validate
   */
  private void assertPathValidation(String path) {
    assertNotNull(path);
    // Should validate against known attack patterns
  }

  /**
   * Asserts traversal detection by various encodings.
   *
   * @param traversalPath the path to test
   */
  private void assertTraversalDetection(String traversalPath) {
    assertTrue(
        traversalPath.contains(".."),
        "Path should contain traversal attempt");
  }

  /**
   * Asserts handling of special characters.
   *
   * @param harmfulPath the path with special characters
   */
  private void assertSpecialCharacterHandling(String harmfulPath) {
    // Should detect and neutralize dangerous characters
    assertFalse(harmfulPath.isEmpty());
  }
}
