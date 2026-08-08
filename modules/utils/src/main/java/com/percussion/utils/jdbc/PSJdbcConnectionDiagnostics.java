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
package com.percussion.utils.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.sql.DataSource;

/**
 * Diagnostics for live JDBC connections — used to prove whether H2 {@code NON_KEYWORDS=VALUE} is
 * active on the connection used by Hibernate / package install.
 *
 * <p><strong>Why this exists:</strong> H2's {@link java.sql.DatabaseMetaData#getURL()} strips
 * connection settings such as {@code NON_KEYWORDS}. Logging only that URL falsely suggests the pool
 * lacks the keyword override even when it is present. These helpers query {@code
 * INFORMATION_SCHEMA.SETTINGS} and probe unquoted {@code VALUE} identifiers instead.
 *
 * <p>Never logs passwords. Safe to call at INFO during startup / package install.
 */
public final class PSJdbcConnectionDiagnostics {

  private PSJdbcConnectionDiagnostics() {}

  /**
   * Describe a live connection: product, metadata URL, H2 NON_KEYWORDS setting, and whether an
   * unquoted {@code VALUE} column reference parses.
   *
   * @param conn open connection; may be {@code null}
   * @return single-line diagnostic summary, never {@code null}
   */
  public static String describeConnection(Connection conn) {
    if (conn == null) {
      return "connection=<null>";
    }
    StringBuilder sb = new StringBuilder(256);
    String product = null;
    String metaUrl = null;
    try {
      product = conn.getMetaData().getDatabaseProductName();
      metaUrl = conn.getMetaData().getURL();
    } catch (SQLException e) {
      sb.append("metadataError=").append(safeMsg(e));
      return sb.toString();
    }
    sb.append("product=").append(nullToDash(product));
    sb.append(" metaUrl=").append(nullToDash(metaUrl));
    sb.append(" metaUrlHasNON_KEYWORDS=").append(urlContainsNonKeywords(metaUrl));

    if (isH2(product, metaUrl)) {
      String nonKeywords = queryH2Setting(conn, "NON_KEYWORDS");
      sb.append(" h2.NON_KEYWORDS=")
          .append(nonKeywords != null && !nonKeywords.isBlank() ? nonKeywords : "<absent>");
      sb.append(" h2.unquotedVALUE_identifier_ok=").append(probeUnquotedValueIdentifier(conn));
    }
    return sb.toString();
  }

  /**
   * Describe a DataSource: optional Hikari configured JDBC URL (via reflection, no hard dependency)
   * plus {@link #describeConnection(Connection)} on a borrowed connection.
   *
   * @param ds datasource; may be {@code null}
   * @return single-line diagnostic summary, never {@code null}
   */
  public static String describeDataSource(DataSource ds) {
    if (ds == null) {
      return "dataSource=<null>";
    }
    StringBuilder sb = new StringBuilder(320);
    String poolUrl = tryReadHikariJdbcUrl(ds);
    if (poolUrl != null) {
      sb.append("poolJdbcUrl=").append(poolUrl);
      sb.append(" poolUrlHasNON_KEYWORDS=").append(urlContainsNonKeywords(poolUrl));
      sb.append(' ');
    } else {
      sb.append("poolJdbcUrl=<unavailable> ");
    }
    try (Connection conn = ds.getConnection()) {
      sb.append(describeConnection(conn));
    } catch (SQLException e) {
      sb.append("borrowConnectionError=").append(safeMsg(e));
    }
    return sb.toString();
  }

  /** True if the URL string (case-insensitive) contains {@code NON_KEYWORDS}. */
  public static boolean urlContainsNonKeywords(String jdbcUrl) {
    return jdbcUrl != null && jdbcUrl.toUpperCase(Locale.ROOT).contains("NON_KEYWORDS");
  }

  /**
   * Probe whether H2 accepts an unquoted {@code VALUE} column name on this connection (the same
   * class of failure as package install / Hibernate {@code PSX_*_PARAM.VALUE} selects).
   *
   * @return {@code true} if the probe statement prepares/executes; {@code false} on syntax error or
   *     other SQLException
   */
  public static boolean probeUnquotedValueIdentifier(Connection conn) {
    if (conn == null) {
      return false;
    }
    // Temporary table name unique enough to avoid collision; dropped in finally when possible.
    final String table = "PSX_H2_VALUE_PROBE_" + Long.toHexString(System.nanoTime());
    try (Statement st = conn.createStatement()) {
      st.execute("CREATE LOCAL TEMPORARY TABLE " + table + "(VALUE VARCHAR(1))");
      try {
        // executeQuery validates the unquoted VALUE identifier; result rows are not needed.
        st.executeQuery("SELECT VALUE FROM " + table + " WHERE 1=0").close();
        return true;
      } finally {
        try {
          st.execute("DROP TABLE " + table);
        } catch (SQLException ignore) {
          // best-effort cleanup
        }
      }
    } catch (SQLException e) {
      return false;
    }
  }

  /**
   * Read an H2 connection setting from {@code INFORMATION_SCHEMA.SETTINGS}.
   *
   * @return setting value, or {@code null} if not H2 / not found / error
   */
  public static String queryH2Setting(Connection conn, String settingName) {
    if (conn == null || settingName == null || settingName.isBlank()) {
      return null;
    }
    final String sql = "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME=?";
    try (var ps = conn.prepareStatement(sql)) {
      ps.setString(1, settingName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
      }
    } catch (SQLException e) {
      return null;
    }
    return null;
  }

  /**
   * Best-effort read of HikariCP configured JDBC URL without a compile dependency on Hikari.
   *
   * @return configured URL, or {@code null} if not Hikari / unwrap failed
   */
  public static String tryReadHikariJdbcUrl(DataSource ds) {
    if (ds == null) {
      return null;
    }
    try {
      Class<?> hikariClass = Class.forName("com.zaxxer.hikari.HikariDataSource");
      if (ds.isWrapperFor(hikariClass)) {
        Object hikari = ds.unwrap(hikariClass);
        Method getJdbcUrl = hikariClass.getMethod("getJdbcUrl");
        Object url = getJdbcUrl.invoke(hikari);
        return url != null ? url.toString() : null;
      }
    } catch (ReflectiveOperationException | SQLException | RuntimeException e) {
      // not Hikari or unwrap denied — ignore
    }
    // Some containers wrap further; try getClass name match
    try {
      if (ds.getClass().getName().contains("HikariDataSource")) {
        Method getJdbcUrl = ds.getClass().getMethod("getJdbcUrl");
        Object url = getJdbcUrl.invoke(ds);
        return url != null ? url.toString() : null;
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      // ignore
    }
    return null;
  }

  static boolean isH2(String product, String metaUrl) {
    if (product != null && product.toUpperCase(Locale.ROOT).contains("H2")) {
      return true;
    }
    return metaUrl != null && metaUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:");
  }

  private static String nullToDash(String s) {
    return s == null || s.isBlank() ? "<null>" : s;
  }

  private static String safeMsg(Exception e) {
    String m = e.getMessage();
    if (m == null) {
      return e.getClass().getSimpleName();
    }
    // never emit password-like fragments if a URL ever included them
    return m.replaceAll("(?i)(password|pwd)=[^;\\s]+", "$1=***");
  }
}
