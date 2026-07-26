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

/**
 * Best-effort pre-install JDBC connectivity probe for the DTS interactive wizard (issue #1513).
 * Never includes password values in messages.
 */
public final class RepositoryConnectionProbe {

  /** Default login timeout seconds for the probe. */
  public static final int DEFAULT_LOGIN_TIMEOUT_SECONDS = 10;

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
    String jdbcUrl = systemProperties.get("perc.db.dts.jdbcUrl");
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      jdbcUrl = buildJdbcUrl(type, systemProperties);
    }
    String user = systemProperties.get("perc.db.user");
    String password = systemProperties.get("perc.db.password");

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

    int previous = DriverManager.getLoginTimeout();
    try {
      if (loginTimeoutSeconds > 0) {
        DriverManager.setLoginTimeout(loginTimeoutSeconds);
      }
      try (Connection conn =
          user != null
              ? DriverManager.getConnection(jdbcUrl, user, password == null ? "" : password)
              : DriverManager.getConnection(jdbcUrl)) {
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

  static String buildJdbcUrl(String type, Map<String, String> p) {
    String host = p.get("perc.db.host");
    String port = p.get("perc.db.port");
    String name = p.get("perc.db.name");
    if (host == null || port == null || name == null) {
      return null;
    }
    return switch (type) {
      case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + name;
      case "postgresql", "postgres" -> "jdbc:postgresql://" + host + ":" + port + "/" + name;
      case "sqlserver" -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name;
      default -> null;
    };
  }

  static String safeSqlMessage(SQLException e) {
    if (e == null) {
      return "unknown SQL error";
    }
    String msg = e.getMessage();
    if (msg == null) {
      return e.getClass().getSimpleName();
    }
    return msg.replaceAll("(?i)password\\s*=\\s*[^;\\s]+", "password=***");
  }
}
