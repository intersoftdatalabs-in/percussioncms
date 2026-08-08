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

import com.percussion.i18n.PSLocaleCodeMigrator;
import com.percussion.install.InstallUtil;
import com.percussion.install.PSLogger;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;
import org.apache.tools.ant.BuildException;

/**
 * ANT task that rewrites persisted locale codes during CMS upgrade (GH-1547).
 *
 * <p>Delegates to {@link PSLocaleCodeMigrator}. Idempotent; never deletes rows. Prefer running
 * after repository setup (server stopped) so {@code rxrepository.properties} is readable.
 *
 * <pre>{@code
 * <PSMigrateI18nLocaleCodes rootDir="${install.dir}" dryRun="false"/>
 * }</pre>
 *
 * <p>Set {@code dryRun="true"} (or {@code -Di18n.locale.migration.dryRun=true}) for staging
 * rehearsal that counts rewrites without committing.
 */
public class PSMigrateI18nLocaleCodes extends PSAction {

  /** Creates the migration task. */
  public PSMigrateI18nLocaleCodes() {
    super();
  }

  private boolean dryRun = false;
  private boolean failOnError = true;

  /**
   * Sets whether migration runs in dry-run mode (count/log only).
   *
   * @param dryRun when true, count and log only (rollback / no updates)
   */
  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  /**
   * Returns whether dry-run mode is enabled.
   *
   * @return whether dry-run mode is enabled
   */
  public boolean isDryRun() {
    return dryRun;
  }

  /**
   * Sets whether SQL/config failures abort the install task.
   *
   * @param failOnError when true (default), SQL/config failures throw {@link BuildException}
   */
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  /**
   * Returns whether failures abort the install.
   *
   * @return whether failures abort the install
   */
  public boolean isFailOnError() {
    return failOnError;
  }

  @Override
  public void execute() throws BuildException {
    super.execute();
    String root = getRootDir();
    if (root == null || root.isBlank()) {
      throw new BuildException("PSMigrateI18nLocaleCodes: rootDir is required");
    }

    // System property wins for staging rehearsals without editing ANT XML.
    String sysDry = System.getProperty("i18n.locale.migration.dryRun");
    if (sysDry != null && !sysDry.isBlank()) {
      dryRun =
          "true".equalsIgnoreCase(sysDry.trim())
              || "yes".equalsIgnoreCase(sysDry.trim())
              || "1".equals(sysDry.trim());
    }

    // Portable path join (no hardcoded separators).
    Path propPath = Path.of(root, "rxconfig", "Installer", "rxrepository.properties");
    File propFile = propPath.toFile();
    if (!propFile.isFile()) {
      PSLogger.logInfo(
          "PSMigrateI18nLocaleCodes: repository properties not found at "
              + propPath
              + " — skipping locale migration (new extract without DB config).");
      return;
    }

    if (InstallUtil.checkServerRunning(root)) {
      String msg =
          "PSMigrateI18nLocaleCodes: CMS appears to be running. Stop the server before"
              + " upgrade (offline migration only).";
      if (failOnError) {
        throw new BuildException(msg);
      }
      PSLogger.logInfo(msg);
      return;
    }

    try (FileInputStream in = new FileInputStream(propFile)) {
      Properties props = new Properties();
      props.load(in);
      PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
      if (getRootDir() != null && !getRootDir().isEmpty()) {
        InstallUtil.setRootDir(getRootDir());
      }

      String driver = dbmsDef.getDriver();
      String server = dbmsDef.getServer();
      String database = dbmsDef.getDataBase();
      String uid = dbmsDef.getUserId();
      String pw = dbmsDef.getPassword();

      PSLogger.logInfo(
          "PSMigrateI18nLocaleCodes: starting"
              + (dryRun ? " (dry-run)" : "")
              + " driver="
              + driver
              + " server="
              + server);

      try (Connection conn = InstallUtil.createConnection(driver, server, database, uid, pw)) {
        PSLocaleCodeMigrator migrator = new PSLocaleCodeMigrator(PSLogger::logInfo);
        PSLocaleCodeMigrator.Result result = migrator.migrate(conn, dbmsDef, dryRun);
        PSLogger.logInfo(
            "PSMigrateI18nLocaleCodes: done"
                + " sys_lang="
                + result.sysLangRewritten()
                + "/"
                + result.sysLangScanned()
                + " contentLocale="
                + result.contentLocaleRewritten()
                + "/"
                + result.contentLocaleScanned());
      }
    } catch (BuildException be) {
      throw be;
    } catch (Exception e) {
      String msg = "PSMigrateI18nLocaleCodes failed: " + e.getMessage();
      PSLogger.logError(msg);
      if (failOnError) {
        throw new BuildException(msg, e);
      }
    }
  }
}
