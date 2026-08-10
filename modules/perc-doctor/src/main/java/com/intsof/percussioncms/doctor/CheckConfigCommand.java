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
package com.intsof.percussioncms.doctor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Read-only deeper config value / misconfig checks for a CMS install ({@code check-config}).
 *
 * <p>Goes beyond file presence (covered by {@code diagnose} / {@code health}) into documented,
 * scoped property value checks. Never deletes or writes. All resolved paths are constrained under
 * the install root via {@link InstallRootGuard}.
 *
 * <p>Scoped files (relative to install root):
 *
 * <ul>
 *   <li>{@code rxconfig/Server/server.properties}
 *   <li>{@code rxconfig/Installer/rxrepository.properties}
 * </ul>
 *
 * <p>Global {@code --dry-run} is accepted for CLI parity and echoed on the report; it has no effect
 * because this command is always non-mutating.
 */
public final class CheckConfigCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "check-config";

  /** Relative path of CMS server.properties under the install root. */
  public static final String SERVER_PROPERTIES_REL = "rxconfig/Server/server.properties";

  /** Relative path of repository installer properties under the install root. */
  public static final String RXREPOSITORY_PROPERTIES_REL =
      "rxconfig/Installer/rxrepository.properties";

  /**
   * Required non-blank keys in {@code rxrepository.properties} for a usable CMS repository
   * connection (H2 embedded still needs these keys set).
   */
  static final String[] REQUIRED_REPO_KEYS = {
    "DB_BACKEND", "DB_DRIVER_NAME", "DB_DRIVER_CLASS_NAME", "DB_SERVER", "UID"
  };

  /** Weak / default password tokens that should not ship in production (case-insensitive). */
  static final Set<String> WEAK_PASSWORD_TOKENS =
      new HashSet<>(
          Arrays.asList(
              "password",
              "changeme",
              "demo",
              "admin",
              "root",
              "sa",
              "secret",
              "percussion",
              "cms",
              "123456",
              "password1"));

  /** Unresolved Ant / installer style placeholders in property values. */
  private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");

  private CheckConfigCommand() {}

  /**
   * Run value / misconfig checks under {@code installRoot}.
   *
   * @param installRoot CMS install root (must exist and be a directory)
   * @param dryRun echoed global flag only; never enables writes
   * @return checklist report
   * @throws IllegalArgumentException if install root is invalid
   * @throws IOException if a config file cannot be read after existence was confirmed (individual
   *     checks prefer FAIL rows when possible)
   */
  public static CheckConfigReport execute(Path installRoot, boolean dryRun) throws IOException {
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    CheckConfigReport report = new CheckConfigReport(COMMAND_NAME, root, dryRun);

    report.add(
        new CheckConfigReport.Check(
            "install-root",
            CheckConfigReport.CheckStatus.PASS,
            "Install root exists and is a directory",
            root));

    checkServerProperties(report, root);
    checkRxRepositoryProperties(report, root);

    return report;
  }

  static void checkServerProperties(CheckConfigReport report, Path root) {
    Path path = resolveScopedFile(report, root, SERVER_PROPERTIES_REL, "server.properties");
    if (path == null) {
      return;
    }
    if (!Files.isRegularFile(path)) {
      report.add(
          new CheckConfigReport.Check(
              "server.properties.present",
              CheckConfigReport.CheckStatus.WARN,
              "server.properties missing (cannot validate values): " + SERVER_PROPERTIES_REL,
              path));
      return;
    }
    report.add(
        new CheckConfigReport.Check(
            "server.properties.present",
            CheckConfigReport.CheckStatus.PASS,
            "server.properties present",
            path));

    Properties props = loadProperties(report, path, "server.properties");
    if (props == null) {
      return;
    }

    checkBooleanFlag(
        report,
        path,
        props,
        "enableDebugTools",
        true,
        CheckConfigReport.CheckStatus.WARN,
        "enableDebugTools=true is unsafe for production (debug tooling exposed)");
    checkBooleanFlag(
        report,
        path,
        props,
        "disableCrossSiteRequestForgeryCheck",
        true,
        CheckConfigReport.CheckStatus.WARN,
        "disableCrossSiteRequestForgeryCheck=true disables CSRF protection");

    String requireHttps = trimToEmpty(props.getProperty("requireHTTPS"));
    if (requireHttps.isEmpty()) {
      report.add(
          new CheckConfigReport.Check(
              "server.requireHTTPS",
              CheckConfigReport.CheckStatus.INFO,
              "requireHTTPS not set (product default may apply)",
              path));
    } else if (isTruthy(requireHttps)) {
      report.add(
          new CheckConfigReport.Check(
              "server.requireHTTPS",
              CheckConfigReport.CheckStatus.PASS,
              "requireHTTPS=true",
              path));
    } else {
      report.add(
          new CheckConfigReport.Check(
              "server.requireHTTPS",
              CheckConfigReport.CheckStatus.INFO,
              "requireHTTPS=false (acceptable for lab/dev; prefer true behind TLS termination"
                  + " when not terminated elsewhere)",
              path));
    }

    String bindPort = trimToEmpty(props.getProperty("bindPort"));
    if (bindPort.isEmpty()) {
      report.add(
          new CheckConfigReport.Check(
              "server.bindPort",
              CheckConfigReport.CheckStatus.WARN,
              "bindPort is empty or missing",
              path));
    } else if (containsUnresolvedPlaceholder(bindPort)) {
      report.add(
          new CheckConfigReport.Check(
              "server.bindPort",
              CheckConfigReport.CheckStatus.FAIL,
              "bindPort contains unresolved placeholder: " + bindPort,
              path));
    } else {
      try {
        int port = Integer.parseInt(bindPort);
        if (port <= 0 || port > 65535) {
          report.add(
              new CheckConfigReport.Check(
                  "server.bindPort",
                  CheckConfigReport.CheckStatus.FAIL,
                  "bindPort out of range (1-65535): " + bindPort,
                  path));
        } else {
          report.add(
              new CheckConfigReport.Check(
                  "server.bindPort",
                  CheckConfigReport.CheckStatus.PASS,
                  "bindPort is a valid TCP port: " + port,
                  path));
        }
      } catch (NumberFormatException e) {
        report.add(
            new CheckConfigReport.Check(
                "server.bindPort",
                CheckConfigReport.CheckStatus.FAIL,
                "bindPort is not an integer: " + bindPort,
                path));
      }
    }

    checkUnresolvedPlaceholders(report, path, props, "server");
  }

  static void checkRxRepositoryProperties(CheckConfigReport report, Path root) {
    Path path =
        resolveScopedFile(report, root, RXREPOSITORY_PROPERTIES_REL, "rxrepository.properties");
    if (path == null) {
      return;
    }
    if (!Files.isRegularFile(path)) {
      report.add(
          new CheckConfigReport.Check(
              "rxrepository.properties.present",
              CheckConfigReport.CheckStatus.WARN,
              "rxrepository.properties missing (cannot validate values): "
                  + RXREPOSITORY_PROPERTIES_REL,
              path));
      return;
    }
    report.add(
        new CheckConfigReport.Check(
            "rxrepository.properties.present",
            CheckConfigReport.CheckStatus.PASS,
            "rxrepository.properties present",
            path));

    Properties props = loadProperties(report, path, "rxrepository.properties");
    if (props == null) {
      return;
    }

    for (String key : REQUIRED_REPO_KEYS) {
      String value = trimToEmpty(props.getProperty(key));
      if (value.isEmpty()) {
        report.add(
            new CheckConfigReport.Check(
                "repo." + key,
                CheckConfigReport.CheckStatus.FAIL,
                "Required repository key missing or blank: " + key,
                path));
      } else if (containsUnresolvedPlaceholder(value)) {
        report.add(
            new CheckConfigReport.Check(
                "repo." + key,
                CheckConfigReport.CheckStatus.FAIL,
                "Required repository key has unresolved placeholder: " + key + "=" + value,
                path));
      } else {
        report.add(
            new CheckConfigReport.Check(
                "repo." + key,
                CheckConfigReport.CheckStatus.PASS,
                "Required repository key set: " + key,
                path));
      }
    }

    String backend = trimToEmpty(props.getProperty("DB_BACKEND")).toUpperCase(Locale.ROOT);
    String driverName = trimToEmpty(props.getProperty("DB_DRIVER_NAME")).toLowerCase(Locale.ROOT);
    String driverClass =
        trimToEmpty(props.getProperty("DB_DRIVER_CLASS_NAME")).toLowerCase(Locale.ROOT);
    if (!backend.isEmpty() && !driverName.isEmpty() && !driverClass.isEmpty()) {
      if (isH2Backend(backend)) {
        if (!driverName.contains("h2") && !driverClass.contains("h2")) {
          report.add(
              new CheckConfigReport.Check(
                  "repo.driver-backend-consistency",
                  CheckConfigReport.CheckStatus.WARN,
                  "DB_BACKEND looks like H2 but driver name/class do not mention h2 (driverName="
                      + props.getProperty("DB_DRIVER_NAME")
                      + ")",
                  path));
        } else {
          report.add(
              new CheckConfigReport.Check(
                  "repo.driver-backend-consistency",
                  CheckConfigReport.CheckStatus.PASS,
                  "H2 backend matches H2 driver settings",
                  path));
        }
      } else {
        // Non-H2: driver should not be the embedded H2 class.
        if (driverClass.contains("org.h2.") || "h2".equals(driverName)) {
          report.add(
              new CheckConfigReport.Check(
                  "repo.driver-backend-consistency",
                  CheckConfigReport.CheckStatus.WARN,
                  "DB_BACKEND="
                      + props.getProperty("DB_BACKEND")
                      + " but driver settings look like H2",
                  path));
        } else {
          report.add(
              new CheckConfigReport.Check(
                  "repo.driver-backend-consistency",
                  CheckConfigReport.CheckStatus.PASS,
                  "Non-H2 backend has non-H2 driver settings",
                  path));
        }
      }
    }

    String pwd = props.getProperty("PWD");
    String pwdTrimmed = pwd == null ? "" : pwd.trim();
    String pwdEncrypted = trimToEmpty(props.getProperty("PWD_ENCRYPTED")).toUpperCase(Locale.ROOT);

    if (pwdTrimmed.isEmpty()) {
      if (isH2Backend(backend)) {
        report.add(
            new CheckConfigReport.Check(
                "repo.PWD",
                CheckConfigReport.CheckStatus.INFO,
                "PWD empty (common for embedded H2 lab installs)",
                path));
      } else if (!backend.isEmpty()) {
        report.add(
            new CheckConfigReport.Check(
                "repo.PWD",
                CheckConfigReport.CheckStatus.WARN,
                "PWD is empty for non-H2 backend " + props.getProperty("DB_BACKEND"),
                path));
      }
    } else if (isWeakPassword(pwdTrimmed)) {
      report.add(
          new CheckConfigReport.Check(
              "repo.PWD",
              CheckConfigReport.CheckStatus.WARN,
              "PWD looks like a well-known weak/default password token",
              path));
    } else {
      report.add(
          new CheckConfigReport.Check(
              "repo.PWD",
              CheckConfigReport.CheckStatus.PASS,
              "PWD is set (value not printed)",
              path));
    }

    if (!pwdTrimmed.isEmpty() && ("N".equals(pwdEncrypted) || pwdEncrypted.isEmpty())) {
      report.add(
          new CheckConfigReport.Check(
              "repo.PWD_ENCRYPTED",
              CheckConfigReport.CheckStatus.WARN,
              "PWD is stored in plaintext (PWD_ENCRYPTED is not Y); prefer encrypted storage in"
                  + " production",
              path));
    } else if ("Y".equals(pwdEncrypted)) {
      report.add(
          new CheckConfigReport.Check(
              "repo.PWD_ENCRYPTED", CheckConfigReport.CheckStatus.PASS, "PWD_ENCRYPTED=Y", path));
    } else if (!pwdEncrypted.isEmpty()) {
      report.add(
          new CheckConfigReport.Check(
              "repo.PWD_ENCRYPTED",
              CheckConfigReport.CheckStatus.INFO,
              "PWD_ENCRYPTED=" + props.getProperty("PWD_ENCRYPTED"),
              path));
    }

    checkUnresolvedPlaceholders(report, path, props, "repo");
  }

  /**
   * Resolve a documented relative config path under root with containment. On path-guard failure
   * records FAIL and returns null.
   */
  static Path resolveScopedFile(
      CheckConfigReport report, Path root, String relativeSlashPath, String label) {
    Path resolved = InstallRootGuard.resolveRelativeUnderRoot(root, relativeSlashPath);
    if (resolved == null) {
      report.add(
          new CheckConfigReport.Check(
              label + ".path",
              CheckConfigReport.CheckStatus.FAIL,
              "Invalid relative config path (path guard rejected): " + relativeSlashPath,
              null));
      return null;
    }
    if (!InstallRootGuard.isUnderInstallRoot(root, resolved)) {
      report.add(
          new CheckConfigReport.Check(
              label + ".path",
              CheckConfigReport.CheckStatus.FAIL,
              "Resolved config path is outside install root: " + resolved,
              resolved));
      return null;
    }
    return resolved;
  }

  static Properties loadProperties(CheckConfigReport report, Path path, String label) {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
      return props;
    } catch (IOException e) {
      report.add(
          new CheckConfigReport.Check(
              label + ".readable",
              CheckConfigReport.CheckStatus.FAIL,
              "Failed to read " + label + ": " + e.getMessage(),
              path));
      return null;
    }
  }

  static void checkBooleanFlag(
      CheckConfigReport report,
      Path path,
      Properties props,
      String key,
      boolean badWhenTrue,
      CheckConfigReport.CheckStatus badStatus,
      String badMessage) {
    String raw = trimToEmpty(props.getProperty(key));
    String id = "server." + key;
    if (raw.isEmpty()) {
      report.add(
          new CheckConfigReport.Check(
              id,
              CheckConfigReport.CheckStatus.INFO,
              key + " not set (product default may apply)",
              path));
      return;
    }
    if (containsUnresolvedPlaceholder(raw)) {
      report.add(
          new CheckConfigReport.Check(
              id,
              CheckConfigReport.CheckStatus.FAIL,
              key + " contains unresolved placeholder: " + raw,
              path));
      return;
    }
    boolean truthy = isTruthy(raw);
    if (badWhenTrue && truthy) {
      report.add(new CheckConfigReport.Check(id, badStatus, badMessage, path));
    } else {
      report.add(
          new CheckConfigReport.Check(
              id, CheckConfigReport.CheckStatus.PASS, key + "=" + raw, path));
    }
  }

  /**
   * Scan property values for unresolved {@code ${...}} placeholders. Skips keys already checked
   * with dedicated FAIL rows for required repo keys / bindPort.
   */
  static void checkUnresolvedPlaceholders(
      CheckConfigReport report, Path path, Properties props, String idPrefix) {
    int found = 0;
    for (String name : props.stringPropertyNames()) {
      String value = props.getProperty(name);
      if (value == null || !containsUnresolvedPlaceholder(value)) {
        continue;
      }
      // Avoid double-reporting dedicated keys.
      if ("bindPort".equals(name) || isRequiredRepoKey(name)) {
        continue;
      }
      found++;
      if (found <= 5) {
        report.add(
            new CheckConfigReport.Check(
                idPrefix + ".placeholder." + name,
                CheckConfigReport.CheckStatus.WARN,
                "Unresolved placeholder in " + name + "=" + value,
                path));
      }
    }
    if (found == 0) {
      report.add(
          new CheckConfigReport.Check(
              idPrefix + ".placeholders",
              CheckConfigReport.CheckStatus.PASS,
              "No unresolved ${...} placeholders in scanned values",
              path));
    } else if (found > 5) {
      report.add(
          new CheckConfigReport.Check(
              idPrefix + ".placeholders.truncated",
              CheckConfigReport.CheckStatus.WARN,
              "Additional unresolved placeholders not listed (total=" + found + ")",
              path));
    }
  }

  static boolean isRequiredRepoKey(String name) {
    for (String key : REQUIRED_REPO_KEYS) {
      if (key.equals(name)) {
        return true;
      }
    }
    return false;
  }

  static boolean isH2Backend(String backendUpperOrEmpty) {
    if (backendUpperOrEmpty == null || backendUpperOrEmpty.isEmpty()) {
      return false;
    }
    String b = backendUpperOrEmpty.toUpperCase(Locale.ROOT);
    return "H2".equals(b) || b.contains("H2");
  }

  static boolean isWeakPassword(String password) {
    Objects.requireNonNull(password, "password");
    String lower = password.toLowerCase(Locale.ROOT);
    return WEAK_PASSWORD_TOKENS.contains(lower);
  }

  static boolean containsUnresolvedPlaceholder(String value) {
    return value != null && UNRESOLVED_PLACEHOLDER.matcher(value).find();
  }

  static boolean isTruthy(String raw) {
    if (raw == null) {
      return false;
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    return "true".equals(v) || "yes".equals(v) || "y".equals(v) || "1".equals(v);
  }

  static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
