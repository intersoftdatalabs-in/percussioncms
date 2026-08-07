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
package com.percussion.preinstall;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Best-effort pre-install JDBC connectivity probe for the DTS interactive wizard (issue #1513).
 * Never includes password values in messages. JDBC URLs are composed only from validated
 * host/port/name components.
 *
 * <p><strong>Threading:</strong> not safe for concurrent use. {@link DriverManager} login timeout
 * is process-global; mutations are serialized on an internal lock for the single-threaded
 * preinstall path only.
 */
public final class RepositoryConnectionProbe {

  /** Default login timeout seconds for the probe. */
  public static final int DEFAULT_LOGIN_TIMEOUT_SECONDS = 10;

  private static final Pattern SAFE_HOST = Pattern.compile("^[A-Za-z0-9._\\-:\\[\\]]{1,253}$");
  private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._\\-]{1,128}$");
  private static final Object LOGIN_TIMEOUT_LOCK = new Object();

  private RepositoryConnectionProbe() {}

  /** Probe outcome status. */
  public enum ProbeStatus {
    /** Probe was skipped because the target is an embedded engine (H2 / Derby). */
    SKIPPED_EMBEDDED,
    /** Probe was skipped because the JDBC driver class is not on the preinstall classpath. */
    SKIPPED,
    /** Probe successfully opened a JDBC connection. */
    SUCCESS,
    /** Probe attempted to open a JDBC connection and failed. */
    FAILED
  }

  /**
   * Result of a connectivity probe.
   *
   * @param status outcome category; never {@code null}
   * @param message operator-facing detail (never contains password); never {@code null}
   */
  public record ProbeResult(ProbeStatus status, String message) {
    /**
     * Whether the probe result is treated as a successful outcome (real success or embedded skip).
     *
     * @return {@code true} when {@link #status()} is {@link ProbeStatus#SUCCESS} or {@link
     *     ProbeStatus#SKIPPED_EMBEDDED}
     */
    public boolean isSuccess() {
      return status == ProbeStatus.SUCCESS || status == ProbeStatus.SKIPPED_EMBEDDED;
    }
  }

  /**
   * Probe connectivity using resolved installer system properties ({@code perc.db.*}).
   *
   * @param systemProperties from {@link MainDTSPreInstall.ResolvedDbConfig#systemProperties()}
   * @param loginTimeoutSeconds login timeout
   * @return probe result
   */
  public static ProbeResult probe(Map<String, String> systemProperties, int loginTimeoutSeconds) {
    Objects.requireNonNull(systemProperties, "systemProperties");
    String type =
        systemProperties
            .getOrDefault("perc.db.type", MainDTSPreInstall.DB_TYPE_DEFAULT)
            .trim()
            .toLowerCase(Locale.ROOT);

    if ("h2".equals(type) || "derby".equals(type)) {
      return new ProbeResult(
          ProbeStatus.SKIPPED_EMBEDDED,
          "Embedded " + type + " — connectivity is validated during install setup.");
    }

    String driverClass = systemProperties.get("perc.db.dts.jdbcDriver");
    String jdbcUrl;
    try {
      jdbcUrl = buildJdbcUrl(type, systemProperties);
    } catch (IllegalArgumentException invalid) {
      return new ProbeResult(ProbeStatus.FAILED, invalid.getMessage());
    }
    String user = systemProperties.get("perc.db.user");
    String password =
        systemProperties.containsKey("perc.db.password")
            ? systemProperties.get("perc.db.password")
            : null;

    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return new ProbeResult(
          ProbeStatus.FAILED,
          "Could not compose a JDBC URL for db.type=" + type + " (check host/port/name).");
    }

    if (driverClass != null && !driverClass.isBlank()) {
      try {
        Class.forName(driverClass);
      } catch (ClassNotFoundException e) {
        return new ProbeResult(
            ProbeStatus.SKIPPED,
            "JDBC driver class not on preinstall classpath ("
                + driverClass
                + "). Connection will be validated during install after drivers are staged. Target"
                + " URL (no secrets): "
                + jdbcUrl
                + (user != null ? " user=" + user : ""));
      }
    }

    synchronized (LOGIN_TIMEOUT_LOCK) {
      int previous = DriverManager.getLoginTimeout();
      try {
        if (loginTimeoutSeconds > 0) {
          DriverManager.setLoginTimeout(loginTimeoutSeconds);
        }
        try (Connection conn = openConnection(jdbcUrl, user, password)) {
          if (conn == null || conn.isClosed()) {
            return new ProbeResult(
                ProbeStatus.FAILED,
                "Connection failed for " + jdbcUrl + " (null or closed connection).");
          }
          return new ProbeResult(
              ProbeStatus.SUCCESS,
              "Connection succeeded for " + jdbcUrl + (user != null ? " user=" + user : "") + ".");
        }
      } catch (SQLException e) {
        return new ProbeResult(
            ProbeStatus.FAILED,
            "Connection failed for "
                + jdbcUrl
                + (user != null ? " user=" + user : "")
                + ": "
                + safeSqlMessage(e));
      } finally {
        DriverManager.setLoginTimeout(previous);
      }
    }
  }

  static Connection openConnection(String jdbcUrl, String user, String password)
      throws SQLException {
    if (user == null) {
      return DriverManager.getConnection(jdbcUrl);
    }
    Properties props = new Properties();
    props.setProperty("user", user);
    if (password != null) {
      props.setProperty("password", password);
    }
    return DriverManager.getConnection(jdbcUrl, props);
  }

  static String buildJdbcUrl(String type, Map<String, String> p) {
    String host = p.get("perc.db.host");
    String port = p.get("perc.db.port");
    String name = p.get("perc.db.name");
    if (host == null || port == null || name == null) {
      return null;
    }
    ValidatedEndpoint ep = validateHostPortName(host, port, name);
    return switch (type) {
      case "mysql" -> "jdbc:mysql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
      case "postgresql", "postgres" ->
          "jdbc:postgresql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
      case "sqlserver" ->
          "jdbc:sqlserver://" + ep.host() + ":" + ep.port() + ";databaseName=" + ep.name();
      // Easy Connect service form — same as MainDTSPreInstall.resolveDbConfig (issue #2338).
      case "oracle", "ora" ->
          "jdbc:oracle:thin:@//" + ep.host() + ":" + ep.port() + "/" + ep.name();
      default -> null;
    };
  }

  /**
   * Validate and normalize host, port, and database name for JDBC URL composition.
   *
   * @return trimmed components safe for concatenation
   * @throws IllegalArgumentException when any component is unsafe
   */
  static ValidatedEndpoint validateHostPortName(String host, String port, String name) {
    if (host == null || port == null || name == null) {
      throw new IllegalArgumentException(
          "Invalid database host/port/name for connection probe (null component).");
    }
    String h = host.trim();
    String p = port.trim();
    String n = name.trim();
    if (h.isEmpty() || !SAFE_HOST.matcher(h).matches()) {
      throw new IllegalArgumentException(
          "Invalid database host for connection probe (disallowed characters).");
    }
    if (!isSafePort(p)) {
      throw new IllegalArgumentException(
          "Invalid database port for connection probe (must be 1-65535).");
    }
    if (n.isEmpty() || !SAFE_NAME.matcher(n).matches()) {
      throw new IllegalArgumentException(
          "Invalid database name for connection probe (disallowed characters).");
    }
    return new ValidatedEndpoint(h, p, n);
  }

  /** Trimmed host/port/name after successful validation. */
  record ValidatedEndpoint(String host, String port, String name) {}

  static boolean isSafePort(String port) {
    try {
      int p = Integer.parseInt(port);
      return p >= 1 && p <= 65535;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  static String safeSqlMessage(SQLException e) {
    if (e == null) {
      return "unknown SQL error";
    }
    String msg = e.getMessage();
    if (msg == null) {
      return e.getClass().getSimpleName();
    }
    String redacted = msg.replaceAll("(?i)(password|passwd|pwd)\\s*[=:]\\s*[^;\\s,]+", "$1=***");
    redacted = redacted.replaceAll("(?i)(://[^:/\\s]+):([^@/\\s]+)@", "$1:***@");
    return redacted;
  }
}
