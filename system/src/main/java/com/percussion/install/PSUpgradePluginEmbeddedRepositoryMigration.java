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
package com.percussion.install;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Properties;
import org.w3c.dom.Element;

/**
 * Upgrade plugin that migrates product-managed CMS Derby repository to H2 via TableFactory export →
 * import (#548 T063).
 *
 * <p>Intended to run as a <strong>pre-upgrade</strong> plugin (server stopped) so subsequent
 * upgrade plugins and SQL steps see H2 as the live repository when migration succeeds.
 *
 * <p>Outcomes map to {@link PSPluginResponse}:
 *
 * <ul>
 *   <li>{@link PSMigrationOutcome#SUCCESS}, {@link PSMigrationOutcome#ALREADY_MIGRATED}, {@link
 *       PSMigrationOutcome#SKIPPED_NON_DERBY} → SUCCESS
 *   <li>{@link PSMigrationOutcome#BLOCKED_BACKUP_GATE}, {@link PSMigrationOutcome#FAILED} →
 *       EXCEPTION (abort upgrade)
 * </ul>
 *
 * <p>Backup gate: product offline backup is attempted when possible; operators may also set {@code
 * perc.migration.externalBackupConfirmed=true} (FR-018b).
 */
public class PSUpgradePluginEmbeddedRepositoryMigration implements IPSUpgradePlugin {

  @Override
  public PSPluginResponse process(IPSUpgradeModule config, Element elemData) {
    PrintStream log = config != null ? config.getLogStream() : System.out;
    try {
      String root = RxUpgrade.getRxRoot();
      if (root == null || root.isBlank()) {
        return new PSPluginResponse(
            PSPluginResponse.EXCEPTION,
            "CMS Derby→H2 migration aborted: install root is not set (RxUpgrade.getRxRoot).");
      }
      Path installRoot = Path.of(root);

      if (InstallUtil.checkServerRunning(root)) {
        return new PSPluginResponse(
            PSPluginResponse.EXCEPTION,
            "CMS Derby→H2 migration aborted: server appears to be running. Stop the CMS before"
                + " upgrade (offline migration only).");
      }

      Properties systemProps = System.getProperties();
      // Attempt product offline backup when gate not already satisfied via system property
      boolean productBackupAlready = false;
      PSEmbeddedRepositoryMigrator migrator =
          new PSEmbeddedRepositoryMigrator(
              installRoot, systemProps, productBackupAlready, null, true);

      log.println("Starting CMS embedded repository migration (Derby→H2, #548)...");
      PSMigrationOutcome outcome = migrator.migrate();
      log.println("CMS embedded repository migration outcome=" + outcome);

      return mapOutcome(outcome);
    } catch (Exception e) {
      String msg =
          PSMigrationSecretsRedactor.redact(
              "CMS Derby→H2 migration failed: "
                  + e.getClass().getSimpleName()
                  + ": "
                  + e.getMessage());
      if (log != null) {
        log.println(msg);
      }
      return new PSPluginResponse(PSPluginResponse.EXCEPTION, msg);
    }
  }

  /**
   * Map migrator outcome to plugin response (package-visible for unit tests).
   *
   * @param outcome migration outcome
   * @return plugin response; never null
   */
  public static PSPluginResponse mapOutcome(PSMigrationOutcome outcome) {
    if (outcome == null) {
      return new PSPluginResponse(
          PSPluginResponse.EXCEPTION, "CMS Derby→H2 migration returned null outcome");
    }
    return switch (outcome) {
      case SUCCESS ->
          new PSPluginResponse(
              PSPluginResponse.SUCCESS,
              "Derby→H2 migration completed successfully. Derby residue retained until operator"
                  + " cleanup (FR-019).");
      case ALREADY_MIGRATED ->
          new PSPluginResponse(
              PSPluginResponse.SUCCESS, "Repository already on H2; migration skipped.");
      case SKIPPED_NON_DERBY ->
          new PSPluginResponse(
              PSPluginResponse.SUCCESS,
              "Repository is not product-managed Derby; migration skipped (FR-009).");
      case BLOCKED_BACKUP_GATE ->
          new PSPluginResponse(
              PSPluginResponse.EXCEPTION,
              "Derby→H2 migration blocked: backup gate not satisfied. When the CMS is stopped,"
                  + " upgrade automatically attempts product offline backup (FR-018a). If that"
                  + " failed, see rxconfig/Installer/migration-report-CMS.properties. Or set "
                  + PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY
                  + "=true after verifying an external backup (FR-018b).");
      case FAILED ->
          new PSPluginResponse(
              PSPluginResponse.EXCEPTION,
              "Derby→H2 migration failed. Source Derby repository left intact; see"
                  + " rxconfig/Installer/migration-report-CMS.properties and upgrade logs.");
    };
  }
}
