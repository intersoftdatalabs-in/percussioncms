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
 * Best-effort pre-install JDBC connectivity probe for the interactive wizard (issue #1513).
 *
 * <p>Uses {@link DriverManager} and optional {@link Class#forName(String)} when a driver class is
 * known from resolved {@code perc.db.*} properties. When the driver is not on the preinstall
 * classpath (typical until distribution JDBC jars are extracted), returns {@link
 * ProbeStatus#SKIPPED} so the operator can continue and rely on ANT {@code
 * PSValidateRepositoryConnection} after files are written.
 *
 * <p>Never includes password values in messages. JDBC URLs are composed only from validated
 * host/port/name components (no operator-controlled query/parameter injection via {@code ;} /
 * {@code ?} in host or name).
 *
 * <p><strong>Threading:</strong> not safe for concurrent use. {@link DriverManager} login timeout
 * is process-global; this class serializes timeout set/restore on an internal lock and is intended
 * for the single-threaded preinstall path only.
 */
public final class RepositoryConnectionProbe {

  /** Default login timeout seconds for the probe. */
  public static final int DEFAULT_LOGIN_TIMEOUT_SECONDS = 10;

  /**
   * Host allowlist: letters, digits, dots, hyphens, underscores, colons/brackets (IPv6), no JDBC
   * separators.
   */
  private static final Pattern SAFE_HOST = Pattern.compile("^[A-Za-z0-9._\\-:\\[\\]]{1,253}$");

  /** Database / service name allowlist (no JDBC parameter separators). */
  private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._\\-]{1,128}$");

  /** Serializes mutations of {@link DriverManager#setLoginTimeout(int)}. */
  private static final Object LOGIN_TIMEOUT_LOCK = new Object();

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
    /**
     * Returns whether the probe counts as a successful outcome.
     *
     * @return {@code true} when {@link #status} is {@link ProbeStatus#SUCCESS} or {@link
     *     ProbeStatus#SKIPPED_EMBEDDED}.
     */
    public boolean isSuccess() {
      return status == ProbeStatus.SUCCESS || status == ProbeStatus.SKIPPED_EMBEDDED;
    }

    /**
     * Returns whether the probe may be retried.
     *
     * @return {@code true} when {@link #status} is {@link ProbeStatus#FAILED}.
     */
    public boolean mayRetry() {
      return status == ProbeStatus.FAILED;
    }
  }

  /**
   * Probe connectivity using resolved installer system properties ({@code perc.db.*}).
   *
   * @param systemProperties from {@link
   *     DbInstallConfigResolver.ResolvedDbConfig#systemProperties()}
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

    String driverClass =
        firstNonBlank(
            systemProperties.get("perc.db.cms.driverClass"),
            systemProperties.get("perc.db.dts.jdbcDriver"));
    String jdbcUrl;
    try {
      jdbcUrl = buildJdbcUrl(type, systemProperties);
    } catch (IllegalArgumentException invalid) {
      return new ProbeResult(ProbeStatus.FAILED, invalid.getMessage());
    }
    String user = systemProperties.get("perc.db.user");
    // Preserve null vs empty: null means "no password property"; empty means explicit empty.
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
                + "). Connection will be validated during install after drivers are staged under"
                + " jetty/base/lib/jdbc. Target URL (no secrets): "
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

  /**
   * Opens a connection without coercing a missing password to {@code ""}.
   *
   * @param jdbcUrl validated URL
   * @param user user id or null
   * @param password password or null when absent
   */
  static Connection openConnection(String jdbcUrl, String user, String password)
      throws SQLException {
    if (user == null) {
      return DriverManager.getConnection(jdbcUrl);
    }
    Properties props = new Properties();
    props.setProperty("user", user);
    if (password != null) {
      // Only set password when present; omit property entirely when null (not empty string).
      props.setProperty("password", password);
    }
    return DriverManager.getConnection(jdbcUrl, props);
  }

  /**
   * Compose a JDBC URL from structured perc.db properties for known backends.
   *
   * @param type normalized db type
   * @param p system properties
   * @return jdbc url or null
   * @throws IllegalArgumentException when components fail safety validation
   */
  static String buildJdbcUrl(String type, Map<String, String> p) {
    // Prefer product-composed DTS URL only when it was built from structured fields we can
    // re-validate via host/port/name. Fall through to validated component composition.
    String host = p.get("perc.db.host");
    String port = p.get("perc.db.port");
    String name = firstNonBlank(p.get("perc.db.name"), p.get("perc.db.cms.name"));

    if (host != null && port != null && name != null) {
      ValidatedEndpoint ep = validateHostPortName(host, port, name);
      return switch (type) {
        case "mysql" -> "jdbc:mysql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
        case "postgresql" -> "jdbc:postgresql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
        case "sqlserver" ->
            "jdbc:sqlserver://" + ep.host() + ":" + ep.port() + ";databaseName=" + ep.name();
        // Easy Connect service form (same as DbInstallConfigResolver structured oracle).
        case "oracle" ->
            "jdbc:oracle:thin:@//" + ep.host() + ":" + ep.port() + "/" + ep.name();
        default -> null;
      };
    }

    // Fall back to cms.server composition when only dbprops path was used
    String cmsServer = p.get("perc.db.cms.server");
    String driverName = p.get("perc.db.cms.driverName");
    if (cmsServer != null && driverName != null) {
      return composeFromCmsServer(type, driverName, cmsServer, name);
    }
    return null;
  }

  /**
   * Validate and normalize host, port, and database/service name for JDBC URL composition.
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

  private static String composeFromCmsServer(
      String type, String driverName, String cmsServer, String name) {
    // Reject obvious injection / credential embedding before any composition.
    if (cmsServer == null || cmsServer.isBlank()) {
      return null;
    }
    if (cmsServer.indexOf('@') >= 0 || cmsServer.contains("..")) {
      throw new IllegalArgumentException(
          "Invalid DB_SERVER for connection probe (credentials or path traversal rejected).");
    }

    ParsedServer parsed = parseCmsServer(type, driverName, cmsServer, name);
    if (parsed == null) {
      throw new IllegalArgumentException(
          "Unable to parse DB_SERVER into safe host/port/name for connection probe.");
    }
    ValidatedEndpoint ep = validateHostPortName(parsed.host(), parsed.port(), parsed.name());
    return switch (parsed.type()) {
      case "mysql" -> "jdbc:mysql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
      case "postgresql" -> "jdbc:postgresql://" + ep.host() + ":" + ep.port() + "/" + ep.name();
      case "sqlserver" ->
          "jdbc:sqlserver://" + ep.host() + ":" + ep.port() + ";databaseName=" + ep.name();
      // Prefer Easy Connect service form; pure SID (@host:port:sid) is still parseable below.
      case "oracle" ->
          "jdbc:oracle:thin:@//" + ep.host() + ":" + ep.port() + "/" + ep.name();
      default -> null;
    };
  }

  /**
   * Best-effort parse of product {@code DB_SERVER} / {@code perc.db.cms.server} shapes into host,
   * port, and name without carrying query strings or extra properties.
   */
  static ParsedServer parseCmsServer(
      String type, String driverName, String cmsServer, String fallbackName) {
    String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
    if (t.isEmpty() && driverName != null) {
      String d = driverName.toLowerCase(Locale.ROOT);
      if (d.contains("mysql")) {
        t = "mysql";
      } else if (d.contains("postgres")) {
        t = "postgresql";
      } else if (d.contains("sqlserver") || d.contains("mssql")) {
        t = "sqlserver";
      } else if (d.contains("oracle")) {
        t = "oracle";
      }
    }

    String s = cmsServer.trim();
    // Strip query string
    int q = s.indexOf('?');
    if (q >= 0) {
      s = s.substring(0, q);
    }
    // SQL Server often embeds ;databaseName= — take only host:port portion
    int semi = s.indexOf(';');
    if (semi >= 0) {
      s = s.substring(0, semi);
    }
    // Oracle Easy Connect may be @//host:port/service — strip @ before //.
    if (s.startsWith("@")) {
      s = s.substring(1);
    }
    if (s.startsWith("//")) {
      s = s.substring(2);
    }

    // forms: host:port/name  or  host:port:sid  or  host:port
    String host;
    String port;
    String dbName = fallbackName == null ? null : fallbackName.trim();

    int slash = s.indexOf('/');
    if (slash >= 0) {
      String hp = s.substring(0, slash).trim();
      dbName = s.substring(slash + 1).trim();
      int colon = hp.lastIndexOf(':');
      if (colon <= 0) {
        return null;
      }
      host = hp.substring(0, colon).trim();
      port = hp.substring(colon + 1).trim();
    } else {
      // host:port:sid (oracle) or host:port
      String[] parts = s.split(":");
      if (parts.length == 2) {
        host = parts[0].trim();
        port = parts[1].trim();
      } else if (parts.length == 3) {
        host = parts[0].trim();
        port = parts[1].trim();
        dbName = parts[2].trim();
        if (t.isEmpty()) {
          t = "oracle";
        }
      } else {
        return null;
      }
    }

    if (dbName == null || dbName.isBlank()) {
      return null;
    }
    if (t.isEmpty()) {
      t = "mysql";
    }
    return new ParsedServer(t, host, port, dbName);
  }

  record ParsedServer(String type, String host, String port, String name) {}

  static String safeSqlMessage(SQLException e) {
    if (e == null) {
      return "unknown SQL error";
    }
    String msg = e.getMessage();
    if (msg == null) {
      return e.getClass().getSimpleName();
    }
    String redacted = msg;
    // password= / password: / pwd= / passwd=
    redacted = redacted.replaceAll("(?i)(password|passwd|pwd)\\s*[=:]\\s*[^;\\s,]+", "$1=***");
    // user:secret@host in URL-like fragments
    redacted = redacted.replaceAll("(?i)(://[^:/\\s]+):([^@/\\s]+)@", "$1:***@");
    return redacted;
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
