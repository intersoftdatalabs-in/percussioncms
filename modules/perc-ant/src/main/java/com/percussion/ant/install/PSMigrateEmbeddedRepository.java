/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.ant.install;

import com.percussion.install.InstallUtil;
import com.percussion.install.PSEmbeddedRepositoryMigrator;
import com.percussion.install.PSLogger;
import com.percussion.install.PSMigrationOutcome;
import com.percussion.install.PSMigrationReportWriter;
import com.percussion.install.PSMigrationSecretsRedactor;
import com.percussion.install.PSPluginResponse;
import com.percussion.install.PSRepositoryBackupGate;
import com.percussion.install.PSUpgradePluginEmbeddedRepositoryMigration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.tools.ant.BuildException;

/**
 * ANT task that runs CMS Derby→H2 migration during upgrade and <strong>fails the install</strong>
 * on FAILED / BLOCKED_BACKUP_GATE (#548 T063).
 *
 * <p>Prefer running once early in the upgrade chain (server stopped). Idempotent for H2 and
 * non-Derby backends.
 *
 * <pre>{@code
 * <PSMigrateEmbeddedRepository rootDir="${install.dir}"
 *     performProductBackup="true"/>
 * }</pre>
 */
public class PSMigrateEmbeddedRepository extends PSAction {

  /** Default constructor. */
  public PSMigrateEmbeddedRepository() {
    super();
  }

  private boolean performProductBackup = true;
  private boolean failOnBlock = true;

  /**
   * Sets whether to perform a product backup before migrating the embedded CMS Derby database.
   *
   * @param performProductBackup <code>true</code> to back up the CMS database before migration
   */
  public void setPerformProductBackup(boolean performProductBackup) {
    this.performProductBackup = performProductBackup;
  }

  /**
   * Returns whether a product backup will be performed before migration.
   *
   * @return <code>true</code> if a product backup will be performed before migration
   */
  public boolean isPerformProductBackup() {
    return performProductBackup;
  }

  /**
   * When true (default), BLOCKED_BACKUP_GATE and FAILED throw {@link BuildException}. When false,
   * logs and continues (not recommended for production upgrades).
   *
   * @param failOnBlock <code>true</code> to fail the upgrade on blocked/errored migrations
   */
  public void setFailOnBlock(boolean failOnBlock) {
    this.failOnBlock = failOnBlock;
  }

  /**
   * Returns whether the upgrade should fail on blocked/errored migrations.
   *
   * @return <code>true</code> when the upgrade should fail on blocked/errored migrations
   */
  public boolean isFailOnBlock() {
    return failOnBlock;
  }

  @Override
  public void execute() throws BuildException {
    super.execute();
    String root = getRootDir();
    if (root == null || root.isBlank()) {
      throw new BuildException("PSMigrateEmbeddedRepository: rootDir is required");
    }

    if (InstallUtil.checkServerRunning(root)) {
      throw new BuildException(
          "PSMigrateEmbeddedRepository: CMS appears to be running. Stop the server before"
              + " upgrade (offline migration only).");
    }

    Path installRoot = Path.of(root);
    Properties systemProps = System.getProperties();
    PSLogger.logInfo("Starting CMS embedded repository migration (Derby→H2, #548)...");

    PSEmbeddedRepositoryMigrator migrator =
        new PSEmbeddedRepositoryMigrator(
            installRoot, systemProps, false, null, performProductBackup);
    PSMigrationOutcome outcome = migrator.migrate();
    PSLogger.logInfo("CMS embedded repository migration outcome=" + outcome);

    PSPluginResponse response = PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(outcome);
    if (response.getType() == PSPluginResponse.EXCEPTION) {
      String msg = PSMigrationSecretsRedactor.redact(response.getMessage());
      String detail = readReportFailureDetail(installRoot);
      if (detail != null && !detail.isBlank()) {
        msg = msg + " Detail: " + detail;
      }
      if (failOnBlock) {
        throw new BuildException(
            msg
                + " (also set -D"
                + PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY
                + "=true if using external backup)");
      }
      PSLogger.logError(msg);
    }
  }

  /**
   * Best-effort read of durable migration report failure reason for operator-visible Ant errors.
   *
   * @param installRoot CMS install root
   * @return redacted failure reason, or null if unavailable
   */
  static String readReportFailureDetail(Path installRoot) {
    try {
      Path reportPath =
          PSMigrationReportWriter.reportPath(
              installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS);
      if (!Files.isRegularFile(reportPath)) {
        return null;
      }
      PSMigrationReportWriter.Report report = PSMigrationReportWriter.read(reportPath);
      if (report == null || report.failureReason() == null || report.failureReason().isBlank()) {
        return null;
      }
      return PSMigrationSecretsRedactor.redact(report.failureReason());
    } catch (Exception e) {
      return null;
    }
  }
}
