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
package com.percussion.ant.install;

import com.percussion.install.InstallUtil;
import com.percussion.install.PSDtsEmbeddedRepositoryMigrator;
import com.percussion.install.PSLogger;
import com.percussion.install.PSMigrationOutcome;
import com.percussion.install.PSMigrationSecretsRedactor;
import com.percussion.install.PSRepositoryBackupGate;
import java.nio.file.Path;
import java.util.Map;
import org.apache.tools.ant.BuildException;

/**
 * ANT task: migrate product-managed DTS service embedded Derby DBs to H2 (#548 T064).
 *
 * <p>Aborts when DTS Tomcat appears to be running under {@code rootDir} (offline migration only).
 *
 * <pre>{@code
 * <PSMigrateDtsEmbeddedRepository rootDir="${install.dir}${staging.dir}"
 *     performProductBackup="true" failOnBlock="true"/>
 * }</pre>
 */
public class PSMigrateDtsEmbeddedRepository extends PSAction {

  /** Default constructor. */
  public PSMigrateDtsEmbeddedRepository() {
    super();
  }

  private boolean performProductBackup = true;
  private boolean failOnBlock = true;
  private String services = "";

  /**
   * Sets whether to perform a product backup before migrating the embedded DTS Derby databases.
   *
   * @param performProductBackup <code>true</code> to back up the DTS databases before migration
   */
  public void setPerformProductBackup(boolean performProductBackup) {
    this.performProductBackup = performProductBackup;
  }

  /**
   * Sets whether to abort the upgrade when a DTS service reports FAILED or BLOCKED_BACKUP_GATE.
   *
   * @param failOnBlock <code>true</code> to fail the upgrade on blocked/errored migrations
   */
  public void setFailOnBlock(boolean failOnBlock) {
    this.failOnBlock = failOnBlock;
  }

  /**
   * Sets the optional comma-separated service names to migrate.
   *
   * @param services comma-separated service names; empty means all default services
   */
  public void setServices(String services) {
    this.services = services == null ? "" : services;
  }

  @Override
  public void execute() throws BuildException {
    super.execute();
    String root = getRootDir();
    if (root == null || root.isBlank()) {
      throw new BuildException("PSMigrateDtsEmbeddedRepository: rootDir is required");
    }

    if (InstallUtil.checkTomcatServerRunning(root)) {
      throw new BuildException(
          "PSMigrateDtsEmbeddedRepository: DTS appears to be running under "
              + root
              + ". Stop the DTS before upgrade (offline migration only).");
    }

    Path installRoot = Path.of(root);
    PSLogger.logInfo("Starting DTS embedded repository migration (Derby→H2, #548)...");

    PSDtsEmbeddedRepositoryMigrator migrator =
        new PSDtsEmbeddedRepositoryMigrator(
            installRoot, System.getProperties(), performProductBackup);

    Map<String, PSMigrationOutcome> results;
    if (services.isBlank()) {
      results = migrator.migrateAllDefaultServices();
    } else {
      results = new java.util.LinkedHashMap<>();
      for (String s : services.split(",")) {
        String name = s.trim();
        if (!name.isEmpty()) {
          results.put(name, migrator.migrateService(name));
        }
      }
    }

    boolean hardFail = false;
    for (Map.Entry<String, PSMigrationOutcome> e : results.entrySet()) {
      PSLogger.logInfo("DTS service=" + e.getKey() + " outcome=" + e.getValue());
      if (e.getValue() == PSMigrationOutcome.FAILED
          || e.getValue() == PSMigrationOutcome.BLOCKED_BACKUP_GATE) {
        hardFail = true;
      }
    }

    if (hardFail && failOnBlock) {
      throw new BuildException(
          PSMigrationSecretsRedactor.redact(
              "One or more DTS services failed Derby→H2 migration. See"
                  + " rxconfig/Installer/migration-report-DTS_*.properties. For external backup"
                  + " confirm set -D"
                  + PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY
                  + "=true"));
    }
  }
}
