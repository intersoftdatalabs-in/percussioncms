/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

  private static final Pattern SAFE_HOST =
      Pattern.compile("^[A-Za-z0-9._\\-:\\[\\]]{1,253}$");
  private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._\\-]{1,128}$");
  private static final Object LOGIN_TIMEOUT_LOCK = new Object();

  private RepositoryConnectionProbe() {}

  /** Probe outcome status. */
  public enum ProbeStatus {
    SKIPPED_EMBEDDED,
    SKIPPED,
    SUCCESS,
    FAILED
  }

  /**
   * Result of a connectivity probe.
   *
   * @param status outcome
   * @param message operator-facing detail (never contains password)
   */
  public record ProbeResult(ProbeStatus status, String message) {
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
    validateHostPortName(host, port, name);
    return switch (type) {
      case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + name;
      case "postgresql", "postgres" -> "jdbc:postgresql://" + host + ":" + port + "/" + name;
      case "sqlserver" -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name;
      default -> null;
    };
  }

  static void validateHostPortName(String host, String port, String name) {
    if (host == null || !SAFE_HOST.matcher(host.trim()).matches() || host.contains(";")) {
      throw new IllegalArgumentException(
          "Invalid database host for connection probe (disallowed characters).");
    }
    if (port == null || !isSafePort(port.trim())) {
      throw new IllegalArgumentException(
          "Invalid database port for connection probe (must be 1-65535).");
    }
    if (name == null || !SAFE_NAME.matcher(name.trim()).matches()) {
      throw new IllegalArgumentException(
          "Invalid database name for connection probe (disallowed characters).");
    }
  }

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
    String redacted =
        msg.replaceAll("(?i)(password|passwd|pwd)\\s*[=:]\\s*[^;\\s,]+", "$1=***");
    redacted = redacted.replaceAll("(?i)(://[^:/\\s]+):([^@/\\s]+)@", "$1:***@");
    return redacted;
  }
}
