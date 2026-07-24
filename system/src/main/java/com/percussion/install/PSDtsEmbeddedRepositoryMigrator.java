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
package com.percussion.install;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-service DTS Derby → H2 migration via TableFactory export/import (#548 T064, FR-006).
 *
 * <p>Each product-managed service (metadata, comments, forms, …) has its own embedded database
 * under {@code Deployment/Server/derbydata/&lt;service&gt;}. Migration creates {@code
 * h2data/&lt;service&gt;}, copies data with TableFactory, then rewrites datasource property files
 * that still point at Derby.
 *
 * <p>Independent of CMS migration; mixed estates only migrate Derby services.
 */
public class PSDtsEmbeddedRepositoryMigrator {

  private static final Logger LOG =
      Logger.getLogger(PSDtsEmbeddedRepositoryMigrator.class.getName());

  /** Default product-managed DTS service database directory names. */
  public static final List<String> DEFAULT_SERVICES =
      List.of(
          "percmetadata",
          "perccomments",
          "percfeeds",
          "percforms",
          "percmembership",
          "percpolls",
          "percakamaiqueuedata");

  private final Path dtsInstallRoot;
  private final Properties systemProperties;
  private final boolean performProductBackupIfNeeded;

  /**
   * @param dtsInstallRoot DTS install root (contains {@code Deployment/Server})
   * @param systemProperties for external backup confirm property
   * @param performProductBackupIfNeeded attempt offline copy of derbydata when gate not confirmed
   */
  public PSDtsEmbeddedRepositoryMigrator(
      Path dtsInstallRoot,
      Properties systemProperties,
      boolean performProductBackupIfNeeded) {
    this.dtsInstallRoot =
        Objects.requireNonNull(dtsInstallRoot, "dtsInstallRoot").toAbsolutePath().normalize();
    this.systemProperties = Objects.requireNonNull(systemProperties, "systemProperties");
    this.performProductBackupIfNeeded = performProductBackupIfNeeded;
  }

  /**
   * Migrate all default services; returns per-service outcomes.
   *
   * @return map service → outcome
   */
  public Map<String, PSMigrationOutcome> migrateAllDefaultServices() {
    Map<String, PSMigrationOutcome> results = new LinkedHashMap<>();
    for (String service : DEFAULT_SERVICES) {
      results.put(service, migrateService(service));
    }
    return results;
  }

  /**
   * Migrate one service database if product-managed Derby is present.
   *
   * @param serviceName e.g. {@code percmetadata}
   * @return terminal outcome for this service
   */
  public PSMigrationOutcome migrateService(String serviceName) {
    Objects.requireNonNull(serviceName, "serviceName");
    String component = "DTS_" + serviceName;
    Path serverRoot = dtsInstallRoot.resolve("Deployment").resolve("Server");
    Path derbyDir = serverRoot.resolve("derbydata").resolve(serviceName);
    Path h2Base = serverRoot.resolve("h2data").resolve(serviceName);

    String sourceBackend = "UNKNOWN";
    String targetBackend = PSJdbcUtils.H2_DB_BACKEND;
    PSBackupGateKind gateKind = null;
    PSMigrationOutcome outcome;
    String failureReason = null;

    try {
      // Detect from live datasource props when present
      Detection detection = detect(serverRoot, serviceName, derbyDir);
      sourceBackend = detection.sourceLabel;

      if (detection.classification == DetectionClass.ALREADY_H2) {
        outcome = PSMigrationOutcome.ALREADY_MIGRATED;
        writeReport(component, outcome, null, sourceBackend, targetBackend, null);
        log(component, outcome, "already H2");
        return outcome;
      }
      if (detection.classification == DetectionClass.NON_DERBY) {
        outcome = PSMigrationOutcome.SKIPPED_NON_DERBY;
        writeReport(component, outcome, null, sourceBackend, targetBackend, null);
        log(component, outcome, "non-Derby backend");
        return outcome;
      }
      if (detection.classification == DetectionClass.NO_SOURCE) {
        outcome = PSMigrationOutcome.SKIPPED_NON_DERBY;
        writeReport(
            component,
            outcome,
            null,
            sourceBackend,
            targetBackend,
            "No Derby data directory and no Derby jdbcUrl for " + serviceName);
        log(component, outcome, "no Derby source");
        return outcome;
      }

      boolean backupOk = false;
      gateKind = PSRepositoryBackupGate.evaluate(backupOk, systemProperties);
      if (!PSRepositoryBackupGate.isSatisfied(gateKind) && performProductBackupIfNeeded) {
        try {
          Path backupRoot =
              dtsInstallRoot
                  .resolve("PreInstall")
                  .resolve("dts-migration-backup")
                  .resolve(serviceName + "-" + System.currentTimeMillis());
          if (Files.isDirectory(derbyDir)) {
            PSRepositoryOfflineBackup.copyRepositoryTree(derbyDir, backupRoot);
          }
          backupOk = true;
          gateKind = PSBackupGateKind.PRODUCT_BACKUP;
        } catch (Exception e) {
          failureReason =
              "Product offline backup failed: "
                  + PSMigrationSecretsRedactor.redact(e.getMessage());
          outcome = PSMigrationOutcome.BLOCKED_BACKUP_GATE;
          writeReport(
              component, outcome, PSBackupGateKind.PRODUCT_BACKUP, sourceBackend, targetBackend,
              failureReason);
          log(component, outcome, failureReason);
          return outcome;
        }
      }
      gateKind = PSRepositoryBackupGate.evaluate(backupOk, systemProperties);
      if (!PSRepositoryBackupGate.isSatisfied(gateKind)) {
        outcome = PSMigrationOutcome.BLOCKED_BACKUP_GATE;
        failureReason =
            "Backup gate not satisfied for DTS service "
                + serviceName
                + ". Set "
                + PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY
                + "=true or allow product offline backup.";
        writeReport(component, outcome, gateKind, sourceBackend, targetBackend, failureReason);
        log(component, outcome, failureReason);
        return outcome;
      }

      try (PSMigratorLock lock = PSMigratorLock.tryAcquire(dtsInstallRoot)) {
        Path staging =
            dtsInstallRoot
                .resolve("PreInstall")
                .resolve("tablefactory-dts-migration")
                .resolve(serviceName + "-" + System.currentTimeMillis());

        Properties sourceProps = buildDerbySourceProps(derbyDir);
        Properties targetProps = buildH2TargetProps(h2Base);
        Files.createDirectories(
            h2Base.getParent() != null ? h2Base.getParent() : serverRoot);

        PSTableFactoryMigrationTransfer.Result transfer =
            PSTableFactoryMigrationTransfer.exportThenImport(sourceProps, targetProps, staging);

        cutoverServiceConfigs(serverRoot, serviceName, h2Base);

        outcome = PSMigrationOutcome.SUCCESS;
        writeReport(
            component,
            outcome,
            gateKind,
            sourceBackend,
            targetBackend,
            "tablesExported="
                + transfer.tablesExported()
                + "; tablesImported="
                + transfer.tablesImported()
                + "; derby residue retained under derbydata/"
                + serviceName);
        log(component, outcome, "SUCCESS via TableFactory");
        return outcome;
      } catch (PSMigratorLock.MigratorLockException e) {
        outcome = PSMigrationOutcome.FAILED;
        failureReason = PSMigrationSecretsRedactor.redact(e.getMessage());
        writeReport(component, outcome, gateKind, sourceBackend, targetBackend, failureReason);
        log(component, outcome, failureReason);
        return outcome;
      }
    } catch (Exception e) {
      outcome = PSMigrationOutcome.FAILED;
      failureReason =
          PSMigrationSecretsRedactor.redact(e.getClass().getSimpleName() + ": " + e.getMessage());
      try {
        writeReport(component, outcome, gateKind, sourceBackend, targetBackend, failureReason);
      } catch (Exception writeEx) {
        LOG.log(Level.SEVERE, "Failed to write DTS migration report", writeEx);
      }
      log(component, outcome, failureReason);
      return outcome;
    }
  }

  enum DetectionClass {
    PRODUCT_MANAGED_DERBY,
    ALREADY_H2,
    NON_DERBY,
    NO_SOURCE
  }

  record Detection(DetectionClass classification, String sourceLabel) {}

  static Detection detect(Path serverRoot, String serviceName, Path derbyDir) throws IOException {
    List<Path> propFiles = findDatasourcePropertyFiles(serverRoot);
    boolean sawDerby = false;
    boolean sawH2 = false;
    boolean sawExternal = false;
    for (Path propsPath : propFiles) {
      Properties p = loadProps(propsPath);
      String url = firstNonBlank(p.getProperty("jdbcUrl"), p.getProperty("jdbc.url"));
      String driver = firstNonBlank(p.getProperty("jdbcDriver"), p.getProperty("jdbc.driver"));
      if (url == null && driver == null) {
        continue;
      }
      String combined = ((url == null ? "" : url) + " " + (driver == null ? "" : driver)).toLowerCase(Locale.ROOT);
      if (combined.contains("h2")) {
        sawH2 = true;
      } else if (combined.contains("derby")) {
        sawDerby = true;
      } else if (url != null && url.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
        sawExternal = true;
      }
    }
    if (sawDerby || Files.isDirectory(derbyDir)) {
      return new Detection(DetectionClass.PRODUCT_MANAGED_DERBY, PSJdbcUtils.DERBY_DB_BACKEND);
    }
    if (sawH2) {
      return new Detection(DetectionClass.ALREADY_H2, PSJdbcUtils.H2_DB_BACKEND);
    }
    if (sawExternal) {
      return new Detection(DetectionClass.NON_DERBY, "EXTERNAL");
    }
    return new Detection(DetectionClass.NO_SOURCE, "NONE");
  }

  static List<Path> findDatasourcePropertyFiles(Path serverRoot) throws IOException {
    List<Path> found = new ArrayList<>();
    if (!Files.isDirectory(serverRoot)) {
      return found;
    }
    try (var walk = Files.walk(serverRoot, 8)) {
      walk.filter(Files::isRegularFile)
          .filter(
              p -> {
                String n = p.getFileName().toString();
                return n.equals("perc-datasources.properties")
                    || n.endsWith("-services.properties")
                    || n.equals("perc-datasources.properties");
              })
          .forEach(found::add);
    }
    Path confPerc = serverRoot.resolve("conf").resolve("perc").resolve("perc-datasources.properties");
    if (Files.isRegularFile(confPerc) && !found.contains(confPerc)) {
      found.add(confPerc);
    }
    return found;
  }

  static void cutoverServiceConfigs(Path serverRoot, String serviceName, Path h2Base)
      throws IOException {
    // Portable JDBC path with forward slashes
    String abs = h2Base.toAbsolutePath().normalize().toString().replace('\\', '/');
    String h2Url = "jdbc:h2:file:" + abs + ";DB_CLOSE_ON_EXIT=FALSE";
    // Also write catalina-relative form for product defaults
    String relativeUrl =
        "jdbc:h2:file:${catalina.home}/h2data/" + serviceName + ";DB_CLOSE_ON_EXIT=FALSE";

    for (Path propsPath : findDatasourcePropertyFiles(serverRoot)) {
      Properties p = loadProps(propsPath);
      String url = firstNonBlank(p.getProperty("jdbcUrl"), p.getProperty("jdbc.url"));
      if (url == null) {
        continue;
      }
      String lower = url.toLowerCase(Locale.ROOT);
      // Only rewrite files that reference this service's derby path or generic derby for service
      boolean forService =
          lower.contains(serviceName.toLowerCase(Locale.ROOT))
              || lower.contains("derbydata")
              || (lower.contains("jdbc:derby") && lower.contains(serviceName.toLowerCase(Locale.ROOT)));
      if (!forService && !lower.contains("jdbc:derby")) {
        continue;
      }
      if (!lower.contains("derby") && !lower.contains(serviceName.toLowerCase(Locale.ROOT))) {
        continue;
      }
      if (p.containsKey("jdbcUrl")) {
        p.setProperty("jdbcUrl", relativeUrl);
      }
      if (p.containsKey("jdbc.url")) {
        p.setProperty("jdbc.url", relativeUrl);
      }
      if (p.containsKey("jdbcDriver")) {
        p.setProperty("jdbcDriver", PSJdbcUtils.H2_DRIVER_CLASS);
      }
      if (p.containsKey("jdbc.driver")) {
        p.setProperty("jdbc.driver", PSJdbcUtils.H2_DRIVER_CLASS);
      }
      if (p.containsKey("hibernate.dialect")) {
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
      }
      if (p.containsKey("db.schema")) {
        p.setProperty("db.schema", "PUBLIC");
      }
      // Clear Derby T/F substitutions when present
      if (p.containsKey("hibernate.query.substitutions")) {
        String sub = p.getProperty("hibernate.query.substitutions", "");
        if (sub.contains("true") && sub.contains("T")) {
          p.setProperty("hibernate.query.substitutions", "");
        }
      }
      writeProps(propsPath, p);
      LOG.info(() -> "DTS cutover wrote " + propsPath + " -> " + relativeUrl + " (abs=" + h2Url + ")");
    }
  }

  static Properties buildDerbySourceProps(Path derbyDir) {
    String path = derbyDir.toAbsolutePath().normalize().toString().replace('\\', '/');
    Properties p = new Properties();
    p.setProperty("DB_BACKEND", PSJdbcUtils.DERBY_DB_BACKEND);
    p.setProperty("DB_DRIVER_NAME", PSJdbcUtils.DERBY_DRIVER);
    p.setProperty("DB_DRIVER_CLASS_NAME", "org.apache.derby.jdbc.EmbeddedDriver");
    p.setProperty("DB_SERVER", path);
    p.setProperty("DB_SCHEMA", "APP");
    p.setProperty("DB_NAME", "");
    p.setProperty("UID", "APP");
    p.setProperty("PWD", "test");
    p.setProperty("PWD_ENCRYPTED", "N");
    return p;
  }

  static Properties buildH2TargetProps(Path h2Base) {
    String path = h2Base.toAbsolutePath().normalize().toString().replace('\\', '/');
    Properties p = new Properties();
    p.setProperty("DB_BACKEND", PSJdbcUtils.H2_DB_BACKEND);
    p.setProperty("DB_DRIVER_NAME", PSJdbcUtils.H2_DRIVER);
    p.setProperty("DB_DRIVER_CLASS_NAME", PSJdbcUtils.H2_DRIVER_CLASS);
    p.setProperty("DB_SERVER", "file:" + path + ";DB_CLOSE_ON_EXIT=FALSE");
    p.setProperty("DB_SCHEMA", "PUBLIC");
    p.setProperty("DB_NAME", "");
    p.setProperty("UID", "sa");
    p.setProperty("PWD", "");
    p.setProperty("PWD_ENCRYPTED", "N");
    return p;
  }

  private void writeReport(
      String component,
      PSMigrationOutcome outcome,
      PSBackupGateKind gate,
      String sourceBackend,
      String targetBackend,
      String failureReason)
      throws IOException {
    Path path = PSMigrationReportWriter.reportPath(dtsInstallRoot, component);
    PSMigrationReportWriter.write(
        path,
        new PSMigrationReportWriter.Report(
            component,
            outcome,
            gate,
            sourceBackend,
            targetBackend,
            failureReason,
            Instant.now()));
  }

  private static void log(String component, PSMigrationOutcome outcome, String detail) {
    String safe = PSMigrationSecretsRedactor.redact(detail);
    LOG.info(() -> "component=" + component + " outcome=" + outcome + " detail=" + safe);
  }

  private static Properties loadProps(Path path) throws IOException {
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      p.load(in);
    }
    return p;
  }

  private static void writeProps(Path path, Properties p) throws IOException {
    Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
    try (OutputStream out = Files.newOutputStream(tmp)) {
      p.store(out, "DTS datasource cutover to H2 (#548) — no secrets in comments");
    }
    try {
      Files.move(
          tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
          java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.trim();
    }
    if (b != null && !b.isBlank()) {
      return b.trim();
    }
    return null;
  }
}
