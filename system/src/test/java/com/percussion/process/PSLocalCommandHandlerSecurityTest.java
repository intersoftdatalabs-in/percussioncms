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
package com.percussion.process;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Security test suite for {@link PSLocalCommandHandler} focusing on CWE-22 (Path Traversal)
 * prevention.
 *
 * <p>Tests verify that file operations properly validate paths to prevent attackers from accessing
 * files outside the intended directory via relative path traversal attacks.
 *
 * <p><strong>Key Test Strategy:</strong>
 *
 * <ul>
 *   <li>Tests the static validatePath() method behavior indirectly through doGetTextFile()
 *   <li>Covers both path traversal patterns (..) and absolute path attempts
 *   <li>Real-world attack scenarios: /etc/passwd, Windows registry, .env files
 * </ul>
 */
@DisplayName("PSLocalCommandHandler - Path Traversal Prevention (CWE-22)")
class PSLocalCommandHandlerSecurityTest {

  @Nested
  @DisplayName("doGetTextFile() - Path Traversal Prevention")
  class GetTextFileSecurityTests {

    @Test
    @DisplayName("Should reject path traversal pattern '..' in doGetTextFile")
    void shouldRejectPathTraversalPattern() {
      File pathWithTraversal = new File("../../etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(pathWithTraversal),
          "Should reject path traversal pattern '..' in doGetTextFile");
    }

    @Test
    @DisplayName("Should reject './' followed by '..' pattern")
    void shouldRejectDotSlashDotDotPattern() {
      File pathWithTraversal = new File("./../../escape");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(pathWithTraversal),
          "Should reject './' followed by '..' pattern");
    }

    @Test
    @DisplayName("Should reject '../' prefix patterns")
    void shouldRejectDotDotSlashPrefix() {
      File pathWithTraversal = new File("../../../etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(pathWithTraversal),
          "Should reject '../' prefix patterns");
    }

    @Test
    @DisplayName("Should reject absolute paths like /etc/passwd")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldRejectAbsoluteEtcPasswd() {
      File absolutePath = new File("/etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(absolutePath),
          "Should reject absolute path /etc/passwd");
    }

    @Test
    @DisplayName("Should reject absolute system paths")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldRejectAbsoluteSystemPath() {
      File absolutePath = new File("/usr/bin/bash");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(absolutePath),
          "Should reject absolute path /usr/bin/bash");
    }

    @Test
    @DisplayName("Should throw SecurityException for path traversal in temp files")
    void shouldRejectTraversalInTempPath(@TempDir Path tempDir) {
      // Create a path like /tmp/junit.../../../etc/passwd
      File traversalPath = new File(tempDir.toFile(), "../../etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(traversalPath),
          "Should reject path traversal even in temp directory");
    }
  }

  @Nested
  @DisplayName("Real-World Attack Scenarios")
  class RealWorldAttackScenarios {

    @Test
    @DisplayName("Should prevent /etc/passwd access attempt")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldPreventEtcPasswdAccess() {
      File etcPasswd = new File("/etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(etcPasswd),
          "Should prevent access to /etc/passwd - CWE-22 protection");
    }

    @Test
    @DisplayName("Should prevent traversal to HOME/.ssh/id_rsa")
    void shouldPreventSSHKeyAccess() {
      File sshKey = new File("../.ssh/id_rsa");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(sshKey),
          "Should prevent traversal to SSH private key");
    }

    @Test
    @DisplayName("Should prevent access to .env file via traversal")
    void shouldPreventEnvFileAccess() {
      File envFile = new File("../../.env");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(envFile),
          "Should prevent access to .env file");
    }

    @Test
    @DisplayName("Should prevent database config access via traversal")
    void shouldPreventDatabaseConfigAccess() {
      File dbConfig = new File("../../../config/database.properties");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(dbConfig),
          "Should prevent access to database configuration");
    }

    @Test
    @DisplayName("Should prevent Windows registry access pattern")
    void shouldPreventWindowsRegistryAccess() {
      File registryPath = new File("../../../../../../WINDOWS/System32/config/SAM");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(registryPath),
          "Should prevent Windows registry access");
    }

    @Test
    @DisplayName("Should prevent application.properties access via traversal")
    void shouldPreventApplicationPropertiesAccess() {
      File appProps = new File("../../application.properties");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(appProps),
          "Should prevent access to application.properties");
    }

    @Test
    @DisplayName("Should prevent ZipSlip-style escape attempts")
    void shouldPreventZipSlipEscape() {
      File zipSlipPath = new File("../../../escape/shell.jsp");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(zipSlipPath),
          "Should prevent ZipSlip-style directory escape");
    }
  }

  @Nested
  @DisplayName("Path Validation Edge Cases")
  class PathValidationEdgeCases {

    @Test
    @DisplayName("Should reject null path")
    void shouldRejectNullPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSLocalCommandHandler.doGetTextFile(null),
          "Should reject null path parameter");
    }

    @Test
    @DisplayName("Should reject complex traversal patterns")
    void shouldRejectComplexTraversalPatterns() {
      File complexPath = new File("src/../../../../../../etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(complexPath),
          "Should reject complex traversal patterns");
    }

    @Test
    @DisplayName("Should reject path with multiple consecutive '..' components")
    void shouldRejectMultipleDotDotComponents() {
      File path = new File("../../../../../../../../etc/passwd");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(path),
          "Should reject multiple consecutive '..' components");
    }

    @Test
    @DisplayName("Should reject symlink-style traversal attempts")
    void shouldRejectSymlinkTraversalAttempts() {
      File symlinkPath = new File("../../../proc/self/environ");

      assertThrows(
          SecurityException.class,
          () -> PSLocalCommandHandler.doGetTextFile(symlinkPath),
          "Should reject symlink-style traversal attempts");
    }
  }

  @Nested
  @DisplayName("Additional Entry Points Security Tests")
  class AdditionalEntryPointsSecurityTests {
    @Test
    @DisplayName("Should reject path traversal pattern in doRemoveFileSystemObject")
    void shouldRejectPathTraversalInRemove() {
      File path = new File("../../etc/passwd");
      assertThrows(
          PSProcessException.class, () -> PSLocalCommandHandler.doRemoveFileSystemObject(path));
    }

    @Test
    @DisplayName("Should reject path traversal pattern in doMakeDirectories")
    void shouldRejectPathTraversalInMakeDirs() {
      File path = new File("../../etc/passwd");
      assertThrows(PSProcessException.class, () -> PSLocalCommandHandler.doMakeDirectories(path));
    }

    @Test
    @DisplayName("Should reject path traversal pattern in doSaveTextFile")
    void shouldRejectPathTraversalInSaveText() {
      File path = new File("../../etc/passwd");
      assertThrows(
          SecurityException.class, () -> PSLocalCommandHandler.doSaveTextFile(path, "test"));
    }

    @Test
    @DisplayName("Should reject path traversal pattern in doSaveBinaryFile")
    void shouldRejectPathTraversalInSaveBinary() {
      File path = new File("../../etc/passwd");
      assertThrows(
          SecurityException.class, () -> PSLocalCommandHandler.doSaveBinaryFile(path, null));
    }
  }
}
