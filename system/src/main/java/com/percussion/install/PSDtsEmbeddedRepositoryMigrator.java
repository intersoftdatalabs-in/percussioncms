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
import java.nio.file.StandardCopyOption;
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
          // Gate not satisfied — PRODUCT_BACKUP is reserved for successful product offline backup
          writeReport(
              component, outcome, PSBackupGateKind.NOT_SATISFIED, sourceBackend, targetBackend,
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
    // Only inspect property files that belong to this service (mixed-backend safe)
    List<Path> propFiles = findDatasourcePropertyFilesForService(serverRoot, serviceName);
    boolean sawDerby = false;
    boolean sawH2 = false;
    boolean sawExternal = false;
    String svc = serviceName.toLowerCase(Locale.ROOT);
    for (Path propsPath : propFiles) {
      Properties p = loadProps(propsPath);
      String url = firstNonBlank(p.getProperty("jdbcUrl"), p.getProperty("jdbc.url"));
      String driver = firstNonBlank(p.getProperty("jdbcDriver"), p.getProperty("jdbc.driver"));
      if (url == null && driver == null) {
        continue;
      }
      String combined =
          ((url == null ? "" : url) + " " + (driver == null ? "" : driver)).toLowerCase(Locale.ROOT);
      // Require service name in path or URL so global conf/perc is only used when it targets us
      if (!pathOrUrlMentionsService(propsPath, url, svc)) {
        continue;
      }
      if (combined.contains("h2")) {
        sawH2 = true;
      } else if (combined.contains("derby")) {
        sawDerby = true;
      } else if (url != null && url.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
        sawExternal = true;
      }
    }
    // Local derbydata/<service> is authoritative for product-managed Derby
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

  /**
   * Datasource property files that belong to {@code serviceName} (path and/or jdbc URL mention the
   * service token). Scans candidates then filters so mixed estates are not conflated.
   */
  static List<Path> findDatasourcePropertyFilesForService(Path serverRoot, String serviceName)
      throws IOException {
    String svc = serviceName.toLowerCase(Locale.ROOT);
    List<Path> found = new ArrayList<>();
    if (!Files.isDirectory(serverRoot)) {
      return found;
    }
    List<Path> candidates = new ArrayList<>();
    try (var walk = Files.walk(serverRoot, 8)) {
      walk.filter(Files::isRegularFile)
          .filter(
              p -> {
                String n = p.getFileName().toString();
                return n.equals("perc-datasources.properties") || n.endsWith("-services.properties");
              })
          .forEach(candidates::add);
    }
    for (Path p : candidates) {
      Properties props = loadProps(p);
      String url = firstNonBlank(props.getProperty("jdbcUrl"), props.getProperty("jdbc.url"));
      if (pathOrUrlMentionsService(p, url, svc)) {
        found.add(p);
      }
    }
    return found;
  }

  /** Map service db dir names to common webapp path fragments. */
  static String serviceWebappHint(String serviceLower) {
    // percmetadata → metadata; perccomments → comments; etc.
    if (serviceLower.startsWith("perc")) {
      return serviceLower.substring(4);
    }
    return serviceLower;
  }

  static boolean pathOrUrlMentionsService(Path propsPath, String url, String serviceLower) {
    String pathLower = propsPath.toString().toLowerCase(Locale.ROOT).replace('\\', '/');
    if (pathLower.contains(serviceLower) || pathLower.contains(serviceWebappHint(serviceLower))) {
      return true;
    }
    if (url != null) {
      String u = normalizeJdbcPath(url);
      return u.contains(serviceLower)
          || u.contains("derbydata/" + serviceLower)
          || u.contains("h2data/" + serviceLower);
    }
    return false;
  }

  /** Lowercase and normalize path separators in JDBC URLs for cross-platform matching. */
  static String normalizeJdbcPath(String url) {
    if (url == null) {
      return "";
    }
    return url.toLowerCase(Locale.ROOT).replace('\\', '/');
  }

  static void cutoverServiceConfigs(Path serverRoot, String serviceName, Path h2Base)
      throws IOException {
    String abs = h2Base.toAbsolutePath().normalize().toString().replace('\\', '/');
    String h2Url = "jdbc:h2:file:" + abs + ";DB_CLOSE_ON_EXIT=FALSE";
    String relativeUrl =
        "jdbc:h2:file:${catalina.home}/h2data/" + serviceName + ";DB_CLOSE_ON_EXIT=FALSE";
    String svc = serviceName.toLowerCase(Locale.ROOT);

    Map<Path, Path> backups = new LinkedHashMap<>();
    Path backupDir =
        serverRoot
            .resolve("PreInstall")
            .resolve("dts-cutover-backup")
            .resolve(serviceName + "-" + System.currentTimeMillis());
    try {
      // Only rewrite property files that clearly target this service
      for (Path propsPath : findDatasourcePropertyFilesForService(serverRoot, serviceName)) {
        Properties p = loadProps(propsPath);
        String url = firstNonBlank(p.getProperty("jdbcUrl"), p.getProperty("jdbc.url"));
        if (url == null) {
          continue;
        }
        if (!pathOrUrlMentionsService(propsPath, url, svc)) {
          continue;
        }
        String lower = normalizeJdbcPath(url);
        // Only cut over live service data URLs (not backup/temp paths that merely contain the name)
        if (!isLiveServiceDataUrl(lower, svc)) {
          continue;
        }
        // Never rewrite a URL that points at a *different* service data dir
        if (pointsAtDifferentService(lower, svc)) {
          continue;
        }
        Files.createDirectories(backupDir);
        // Collision-resistant basename (same approach as PSConfigCutover.shortPathDigest)
        String baseName =
            propsPath.getFileName() != null
                ? propsPath.getFileName().toString()
                : "datasource.properties";
        String digest =
            PSConfigCutover.shortPathDigest(propsPath.toAbsolutePath().normalize().toString());
        Path bak = backupDir.resolve(baseName + "." + digest + ".bak");
        Files.copy(propsPath, bak, StandardCopyOption.REPLACE_EXISTING);
        backups.put(propsPath, bak);

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
        if (p.containsKey("hibernate.query.substitutions")) {
          String sub = p.getProperty("hibernate.query.substitutions", "");
          if (sub.contains("true") && sub.contains("T")) {
            p.setProperty("hibernate.query.substitutions", "");
          }
        }
        writeProps(propsPath, p);
        LOG.info(
            () -> "DTS cutover wrote " + propsPath + " -> " + relativeUrl + " (abs=" + h2Url + ")");
      }
    } catch (IOException e) {
      PSConfigCutover.rollback(backups);
      throw new IOException(
          "DTS cutover failed for service "
              + serviceName
              + "; restored previous configs: "
              + e.getMessage(),
          e);
    }
  }

  /**
   * True if JDBC URL clearly targets another known service's live data directory.
   */
  static boolean pointsAtDifferentService(String urlLower, String thisServiceLower) {
    String u = normalizeJdbcPath(urlLower);
    for (String other : DEFAULT_SERVICES) {
      String o = other.toLowerCase(Locale.ROOT);
      if (o.equals(thisServiceLower)) {
        continue;
      }
      if (isLiveServiceDataUrl(u, o)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Live product data paths only: {@code derbydata/&lt;svc&gt;}, {@code h2data/&lt;svc&gt;}, or a
   * path segment ending in {@code /&lt;svc&gt;} (optional trailing JDBC params). Excludes backup /
   * temp paths that only contain the service name as a substring (e.g. {@code
   * /backup/perccomments_backup}).
   */
  static boolean isLiveServiceDataUrl(String urlLower, String serviceLower) {
    String u = normalizeJdbcPath(urlLower);
    if (u.isEmpty() || serviceLower == null || serviceLower.isBlank()) {
      return false;
    }
    String svc = serviceLower.toLowerCase(Locale.ROOT);
    if (u.contains("derbydata/" + svc) || u.contains("h2data/" + svc)) {
      return true;
    }
    int idx = u.lastIndexOf('/' + svc);
    if (idx < 0) {
      return false;
    }
    int after = idx + 1 + svc.length();
    if (after == u.length()) {
      return true;
    }
    char c = u.charAt(after);
    return c == ';' || c == '?' || c == '#';
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
