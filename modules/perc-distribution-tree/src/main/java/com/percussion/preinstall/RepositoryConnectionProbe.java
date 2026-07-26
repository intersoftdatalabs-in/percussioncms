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
 * Best-effort pre-install JDBC connectivity probe for the interactive wizard (issue #1513).
 *
 * <p>Uses {@link DriverManager} and optional {@link Class#forName(String)} when a driver class is
 * known from resolved {@code perc.db.*} properties. When the driver is not on the preinstall
 * classpath (typical until distribution JDBC jars are extracted), returns {@link
 * ProbeStatus#SKIPPED} so the operator can continue and rely on ANT {@code
 * PSValidateRepositoryConnection} after files are written.
 *
 * <p>Never includes password values in messages.
 */
public final class RepositoryConnectionProbe {

  /** Default login timeout seconds for the probe. */
  public static final int DEFAULT_LOGIN_TIMEOUT_SECONDS = 10;

  private RepositoryConnectionProbe() {}

  /** Probe outcome status. */
  public enum ProbeStatus {
    /** Embedded engine — no network probe. */
    SKIPPED_EMBEDDED,
    /** Driver not loadable on this classpath. */
    SKIPPED,
    /** Connection succeeded. */
    SUCCESS,
    /** Connection or driver failure with operator message. */
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

    public boolean mayRetry() {
      return status == ProbeStatus.FAILED;
    }
  }

  /**
   * Probe connectivity using resolved installer system properties ({@code perc.db.*}).
   *
   * @param systemProperties from {@link DbInstallConfigResolver.ResolvedDbConfig#systemProperties()}
   * @param loginTimeoutSeconds login timeout; use {@link #DEFAULT_LOGIN_TIMEOUT_SECONDS} when
   *     unsure
   * @return probe result
   */
  public static ProbeResult probe(Map<String, String> systemProperties, int loginTimeoutSeconds) {
    Objects.requireNonNull(systemProperties, "systemProperties");
    String type =
        systemProperties
            .getOrDefault("perc.db.type", DbInstallConfigResolver.DB_TYPE_DEFAULT)
            .trim()
            .toLowerCase(Locale.ROOT);

    if ("h2".equals(type) || "derby".equals(type)) {
      return new ProbeResult(
          ProbeStatus.SKIPPED_EMBEDDED,
          "Embedded " + type + " — connectivity is validated during install setup.");
    }

    String driverClass = firstNonBlank(
        systemProperties.get("perc.db.cms.driverClass"),
        systemProperties.get("perc.db.dts.jdbcDriver"));
    String jdbcUrl = buildJdbcUrl(type, systemProperties);
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
                + "). Connection will be validated during install after drivers are staged under"
                + " jetty/base/lib/jdbc. Target URL (no secrets): "
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
            "Connection succeeded for "
                + jdbcUrl
                + (user != null ? " user=" + user : "")
                + ".");
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

  /**
   * Compose a JDBC URL from structured perc.db properties for known backends.
   *
   * @param type normalized db type
   * @param p system properties
   * @return jdbc url or null
   */
  static String buildJdbcUrl(String type, Map<String, String> p) {
    String dtsUrl = p.get("perc.db.dts.jdbcUrl");
    if (dtsUrl != null && !dtsUrl.isBlank()) {
      return dtsUrl.trim();
    }

    String host = p.get("perc.db.host");
    String port = p.get("perc.db.port");
    String name = firstNonBlank(p.get("perc.db.name"), p.get("perc.db.cms.name"));
    if (host == null || port == null || name == null) {
      // Fall back to cms.server composition when only dbprops path was used
      String cmsServer = p.get("perc.db.cms.server");
      String driverName = p.get("perc.db.cms.driverName");
      if (cmsServer != null && driverName != null) {
        return composeFromCmsServer(type, driverName, cmsServer, name);
      }
      return null;
    }

    return switch (type) {
      case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + name;
      case "postgresql" -> "jdbc:postgresql://" + host + ":" + port + "/" + name;
      case "sqlserver" ->
          "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name;
      case "oracle" -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + name;
      default -> null;
    };
  }

  private static String composeFromCmsServer(
      String type, String driverName, String cmsServer, String name) {
    // MySQL-style server already starts with //host:port/db?...
    if ("mysql".equals(type) || "mysql".equalsIgnoreCase(driverName)) {
      String s = cmsServer.startsWith("//") ? cmsServer : "//" + cmsServer;
      int q = s.indexOf('?');
      if (q > 0) {
        s = s.substring(0, q);
      }
      return "jdbc:mysql:" + s;
    }
    if ("sqlserver".equals(type) || "sqlserver".equalsIgnoreCase(driverName)) {
      String s = cmsServer.startsWith("//") ? cmsServer.substring(2) : cmsServer;
      return "jdbc:sqlserver:" + (s.startsWith("//") ? s : "//" + s);
    }
    if ("oracle".equals(type) || (driverName != null && driverName.contains("oracle"))) {
      String s = cmsServer.startsWith("@") ? cmsServer.substring(1) : cmsServer;
      return "jdbc:oracle:thin:@" + s;
    }
    if ("postgresql".equals(type) || "postgresql".equalsIgnoreCase(driverName)) {
      String s = cmsServer.startsWith("//") ? cmsServer : "//" + cmsServer;
      return "jdbc:postgresql:" + s;
    }
    return null;
  }

  static String safeSqlMessage(SQLException e) {
    if (e == null) {
      return "unknown SQL error";
    }
    String msg = e.getMessage();
    if (msg == null) {
      return e.getClass().getSimpleName();
    }
    // Strip common password-like substrings defensively
    return msg.replaceAll("(?i)password\\s*=\\s*[^;\\s]+", "password=***");
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
