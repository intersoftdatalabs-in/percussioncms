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

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSField;
import com.percussion.server.PSServer;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableFactory;
import com.percussion.util.PSSqlHelper;
import com.percussion.utils.jdbc.PSJdbcUtils;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JDBC create/drop of system-def backend columns. Identifiers are restricted to letter / digit /
 * underscore names already validated by {@link SystemDefAdaptor#validateFieldName(String)} and
 * {@link SystemDefAdaptor#columnNameForField(String)}.
 */
final class JdbcSystemDefColumnSchema implements SystemDefColumnSchema {

  private static final Logger log = LogManager.getLogger(JdbcSystemDefColumnSchema.class);

  private static final int DEFAULT_TEXT_SIZE = 50;

  private final Supplier<Connection> connectionFactory;

  JdbcSystemDefColumnSchema() {
    this(JdbcSystemDefColumnSchema::openRepositoryConnection);
  }

  JdbcSystemDefColumnSchema(Supplier<Connection> connectionFactory) {
    this.connectionFactory = connectionFactory != null ? connectionFactory : () -> null;
  }

  @Override
  public void ensureColumn(
      String tableName, String columnName, String fieldDataType, String dataFormat) {
    String table = requireIdent(tableName, "table");
    String column = requireIdent(columnName, "column");
    withConnection(
        conn -> {
          if (columnExists(conn, table, column)) {
            return;
          }
          String sql = addColumnSql(conn, table, column, fieldDataType, dataFormat);
          log.info("Adding system-def column: {}", sql);
          try (Statement st = conn.createStatement()) {
            st.execute(sql);
          }
        });
  }

  @Override
  public void dropColumnIfPresent(String tableName, String columnName) {
    String table = requireIdent(tableName, "table");
    String column = requireIdent(columnName, "column");
    withConnection(
        conn -> {
          if (!columnExists(conn, table, column)) {
            return;
          }
          String sql = "ALTER TABLE " + tableRef(conn, table) + " DROP COLUMN " + column;
          log.info("Dropping system-def column: {}", sql);
          try (Statement st = conn.createStatement()) {
            st.execute(sql);
          } catch (SQLException e) {
            if (SystemDefAdaptor.isMissingColumnFailure(e)) {
              log.warn("System-def column already absent, skipping drop: {}.{}", table, column);
              return;
            }
            throw e;
          }
        });
  }

  static boolean columnExists(Connection conn, String table, String column) throws SQLException {
    DatabaseMetaData md = conn.getMetaData();
    String catalog = conn.getCatalog();
    String schema = safeSchema(conn);
    if (findColumn(md, catalog, schema, table, column)) {
      return true;
    }
    String upperTable = table.toUpperCase(Locale.ROOT);
    String upperColumn = column.toUpperCase(Locale.ROOT);
    if (!upperTable.equals(table) || !upperColumn.equals(column)) {
      if (findColumn(md, catalog, schema, upperTable, upperColumn)) {
        return true;
      }
    }
    if (schema != null && !schema.equals(schema.toUpperCase(Locale.ROOT))) {
      return findColumn(
          md, catalog, schema.toUpperCase(Locale.ROOT), upperTable, upperColumn);
    }
    return false;
  }

  private static boolean findColumn(
      DatabaseMetaData md, String catalog, String schema, String table, String column)
      throws SQLException {
    try (ResultSet rs = md.getColumns(catalog, schema, table, column)) {
      return rs.next();
    }
  }

  static String addColumnSql(
      Connection conn, String table, String column, String fieldDataType, String dataFormat)
      throws SQLException {
    String type = nativeType(conn, fieldDataType, dataFormat);
    String tableSql = tableRef(conn, table);
    String product = databaseProduct(conn);
    String nullClause = nullClause(conn);
    if (isOracle(product)) {
      return "ALTER TABLE " + tableSql + " ADD (" + column + " " + type + ")";
    }
    if (isSqlServer(product, conn)) {
      return "ALTER TABLE " + tableSql + " ADD " + column + " " + type + nullClause;
    }
    return "ALTER TABLE " + tableSql + " ADD COLUMN " + column + " " + type + nullClause;
  }

  static String nativeType(Connection conn, String fieldDataType, String dataFormat)
      throws SQLException {
    String product = databaseProduct(conn);
    String dt = fieldDataType == null ? PSField.DT_TEXT : fieldDataType.trim().toLowerCase(Locale.ROOT);
    if (PSField.DT_INTEGER.equals(dt)) {
      return isOracle(product) ? "NUMBER(10)" : "INTEGER";
    }
    if (PSField.DT_DATETIME.equals(dt) || PSField.DT_DATE.equals(dt)) {
      if (isSqlServer(product, conn)) {
        return "DATETIME";
      }
      return "TIMESTAMP";
    }
    int size = parseTextSize(dataFormat);
    if (isSqlServer(product, conn)) {
      return "NVARCHAR(" + size + ")";
    }
    if (isOracle(product)) {
      return "VARCHAR2(" + size + ")";
    }
    return "VARCHAR(" + size + ")";
  }

  static int parseTextSize(String dataFormat) {
    if (StringUtils.isBlank(dataFormat)) {
      return DEFAULT_TEXT_SIZE;
    }
    try {
      int parsed = Integer.parseInt(dataFormat.trim());
      return parsed > 0 ? parsed : DEFAULT_TEXT_SIZE;
    } catch (NumberFormatException e) {
      return DEFAULT_TEXT_SIZE;
    }
  }

  private static String nullClause(Connection conn) throws SQLException {
    String driver = driverName(conn);
    if (StringUtils.isBlank(driver)) {
      return "";
    }
    try {
      return PSSqlHelper.getNullColumnSpecifier(driver);
    } catch (RuntimeException e) {
      return "";
    }
  }

  static String tableRef(Connection conn, String table) throws SQLException {
    try {
      return PSSqlHelper.qualifyTableName(table);
    } catch (Exception e) {
      String schema = safeSchema(conn);
      if (StringUtils.isNotBlank(schema)) {
        return schema + "." + table;
      }
      return table;
    }
  }

  private static String safeSchema(Connection conn) {
    try {
      return conn.getSchema();
    } catch (SQLException e) {
      return null;
    }
  }

  private static String databaseProduct(Connection conn) throws SQLException {
    String product = conn.getMetaData().getDatabaseProductName();
    return product == null ? "" : product;
  }

  private static String driverName(Connection conn) throws SQLException {
    String url = conn.getMetaData().getURL();
    if (StringUtils.isBlank(url)) {
      return "";
    }
    return PSJdbcUtils.getDriverFromUrl(url);
  }

  private static boolean isOracle(String product) {
    return product.toLowerCase(Locale.ROOT).contains("oracle");
  }

  private static boolean isSqlServer(String product, Connection conn) throws SQLException {
    String lower = product.toLowerCase(Locale.ROOT);
    if (lower.contains("microsoft") || lower.contains("sql server")) {
      return true;
    }
    String driver = driverName(conn);
    return PSJdbcUtils.JTDS_DRIVER.equals(driver)
        || PSJdbcUtils.MICROSOFT_DRIVER.equals(driver)
        || PSJdbcUtils.SPRINTA.equals(driver);
  }

  static String requireIdent(String raw, String label) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException(label + " is required");
    }
    String trimmed = raw.trim();
    if (trimmed.length() > 128) {
      throw new IllegalArgumentException(label + " exceeds maximum length");
    }
    char first = trimmed.charAt(0);
    if (!Character.isLetter(first)) {
      throw new IllegalArgumentException(label + " must start with a letter");
    }
    for (int i = 1; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        throw new IllegalArgumentException(label + " must be letters, digits, or underscore");
      }
    }
    return trimmed;
  }

  private void withConnection(SqlConsumer consumer) {
    Connection conn = connectionFactory.get();
    if (conn == null) {
      throw new IllegalStateException("No repository connection for system-def column DDL");
    }
    try {
      consumer.accept(conn);
    } catch (SQLException e) {
      throw new IllegalStateException("System-def column DDL failed", e);
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        log.debug("Closing system-def column connection: {}", e.getMessage());
      }
    }
  }

  static Connection openRepositoryConnection() {
    try {
      Properties props =
          PSJdbcDbmsDef.loadRxRepositoryProperties(PSServer.getRxDir().getAbsolutePath());
      PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
      return PSJdbcTableFactory.getConnection(dbmsDef);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to open repository connection for system-def DDL", e);
    }
  }

  @FunctionalInterface
  private interface SqlConsumer {
    void accept(Connection conn) throws SQLException;
  }
}
