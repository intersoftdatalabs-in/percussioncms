/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.io.PathUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class PSValidateRepositoryConnectionTest {

  @TempDir Path tempDir;

  private String previousRxDeployDir;

  @BeforeEach
  void captureRxDeployDir() {
    previousRxDeployDir = System.getProperty(PathUtils.DEPLOY_DIR_PROP);
    PathUtils.clearRxDir();
  }

  @AfterEach
  void restoreRxDeployDir() {
    PathUtils.clearRxDir();
    if (previousRxDeployDir != null) {
      System.setProperty(PathUtils.DEPLOY_DIR_PROP, previousRxDeployDir);
    } else {
      System.clearProperty(PathUtils.DEPLOY_DIR_PROP);
    }
  }

  @Test
  void missingRepositoryPropertiesFails() {
    PSValidateRepositoryConnection task = new PSValidateRepositoryConnection();
    task.setRootDir(tempDir.toString());
    BuildException ex = assertThrows(BuildException.class, task::execute);
    assertTrue(ex.getMessage().toLowerCase().contains("not found"));
  }

  @Test
  void unreachableHostFailsWithoutLeakingPassword() throws Exception {
    Path installer = tempDir.resolve("rxconfig/Installer");
    Files.createDirectories(installer);
    Path props = installer.resolve("rxrepository.properties");
    String secret = "super-secret-password-do-not-leak";
    Files.writeString(
        props,
        """
        DB_BACKEND=MYSQL
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        DB_SERVER=//127.0.0.1:1/nosuchdb
        DB_NAME=nosuchdb
        DB_SCHEMA=
        UID=cmsuser
        PWD=%s
        PWD_ENCRYPTED=N
        """
            .formatted(secret),
        StandardCharsets.UTF_8);

    // Empty jdbc dir — InstallUtil may still attempt connect; either path must fail closed
    Files.createDirectories(tempDir.resolve("jetty/base/lib/jdbc"));

    // Simulate pollution left by PSAction.setRootDir (e.g. PSPkgConfigFileEmptyConditionTest)
    // pointing at a deleted temp directory.
    Path stale = tempDir.resolve("already-deleted");
    Files.createDirectories(stale);
    System.setProperty(PathUtils.DEPLOY_DIR_PROP, stale.toAbsolutePath().toString());
    Files.delete(stale);
    PathUtils.clearRxDir();

    PSValidateRepositoryConnection task = new PSValidateRepositoryConnection();
    task.setRootDir(tempDir.toString());
    task.setLoginTimeoutSeconds(2);
    BuildException ex = assertThrows(BuildException.class, task::execute);
    assertTrue(
        ex.getMessage().toLowerCase().contains("validation failed")
            || ex.getMessage().toLowerCase().contains("connection"),
        () -> "unexpected message: " + ex.getMessage());
    assertTrue(
        PSValidateRepositoryConnection.messageDoesNotContainSecret(ex.getMessage(), secret),
        () -> "password leaked in: " + ex.getMessage());
  }

  @Test
  void bindInstallRootPointsPathUtilsAtRoot() {
    PSValidateRepositoryConnection.bindInstallRoot(tempDir.toString());
    assertEquals(
        tempDir.toAbsolutePath().normalize().toString(), PathUtils.getRxDir().getAbsolutePath());
    assertEquals(
        tempDir.toAbsolutePath().normalize().toString(),
        System.getProperty(PathUtils.DEPLOY_DIR_PROP));
  }

  @Test
  void bindInstallRootRejectsMissingDirectory() {
    BuildException ex =
        assertThrows(
            BuildException.class,
            () ->
                PSValidateRepositoryConnection.bindInstallRoot(
                    tempDir.resolve("does-not-exist").toString()));
    assertTrue(ex.getMessage().toLowerCase().contains("does not exist"));
  }

  @Test
  void failureMessageAddsDriverGuidanceWhenMissingDriver() {
    String msg =
        PSValidateRepositoryConnection.failureMessage(
            "MYSQL",
            "//db:3306/p",
            "org.mariadb.jdbc.Driver",
            "Driver mysql is not supported by the current installer.");
    assertTrue(msg.contains("jetty/base/lib/jdbc"));
    assertTrue(msg.contains("org.mariadb.jdbc.Driver"));
    assertFalse(msg.contains("password"));
  }

  @Test
  void messageHelperRejectsSecretLeak() {
    assertFalse(
        PSValidateRepositoryConnection.messageDoesNotContainSecret(
            "failed for user with pass abc", "abc"));
    assertTrue(
        PSValidateRepositoryConnection.messageDoesNotContainSecret(
            "failed for backend=MYSQL", "abc"));
  }

  @Test
  void looksLikeMissingDriverDetectsCommonPatterns() {
    assertTrue(
        PSValidateRepositoryConnection.looksLikeMissingDriver(
            "no suitable driver found", "org.mariadb.jdbc.Driver"));
    assertTrue(
        PSValidateRepositoryConnection.looksLikeMissingDriver(
            "Driver mysql is not supported by the current installer.", null));
    assertTrue(
        PSValidateRepositoryConnection.looksLikeMissingDriver(
            "Connection returned null (driver may be missing or failed to load).", null));
    assertFalse(
        PSValidateRepositoryConnection.looksLikeMissingDriver(
            "Connection refused", "org.mariadb.jdbc.Driver"));
  }

  @Test
  void nullConnectionMessageTriggersDriverGuidance() {
    String msg =
        PSValidateRepositoryConnection.failureMessage(
            "MYSQL",
            "//db:3306/p",
            "org.mariadb.jdbc.Driver",
            "Connection returned null (driver may be missing or failed to load)."
                + " Check JDBC drivers under jetty/base/lib/jdbc and credentials.");
    assertTrue(msg.contains("jetty/base/lib/jdbc"));
    assertTrue(msg.toLowerCase().contains("driver"));
  }
}
