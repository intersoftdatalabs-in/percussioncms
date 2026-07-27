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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pre-cutover validation probes for Derby → H2 migration (#548 T059, FR-007, SC-002).
 *
 * <p>Checks table-set equality, row counts, optional boolean/CLOB samples, and NEXTNUMBER presence.
 */
public final class PSMigrationValidator {

  private static final Logger LOG = Logger.getLogger(PSMigrationValidator.class.getName());

  private PSMigrationValidator() {}

  /**
   * Validation outcome.
   *
   * @param passed all hard probes passed
   * @param summary human-readable summary (no secrets)
   * @param tableCount source/target table count when equal
   * @param rowCountMismatches tables with differing counts
   */
  public record Result(
      boolean passed, String summary, int tableCount, List<String> rowCountMismatches) {}

  /**
   * Lightweight post-import check when TableFactory already performed schema/data load: ensure the
   * target is openable and has the expected minimum table count.
   *
   * @param target H2 (or target) connection
   * @param expectedMinTables minimum user tables (usually tables imported)
   * @return result
   */
  public static Result validateTargetOnly(Connection target, int expectedMinTables)
      throws SQLException {
    Objects.requireNonNull(target, "target");
    Set<String> tables = listUserTables(target);
    if (tables.size() < expectedMinTables) {
      return new Result(
          false,
          "Target table count "
              + tables.size()
              + " < expected min "
              + expectedMinTables
              + " after TableFactory import",
          tables.size(),
          List.of());
    }
    String nextProbe = null;
    if (tables.contains("NEXTNUMBER") || tables.contains("nextnumber")) {
      long n = countRows(target, "NEXTNUMBER");
      nextProbe = " NEXTNUMBER rows=" + n;
    }
    return new Result(
        true,
        "OK post-import tables=" + tables.size() + (nextProbe == null ? "" : nextProbe),
        tables.size(),
        List.of());
  }

  /**
   * Validate target against source before cutover (dual-connection probes).
   *
   * @param source Derby connection
   * @param target H2 connection
   * @return result; never null
   */
  public static Result validate(Connection source, Connection target) throws SQLException {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    Set<String> sourceTables = listUserTables(source);
    Set<String> targetTables = listUserTables(target);

    List<String> missingOnTarget = new ArrayList<>();
    for (String t : sourceTables) {
      if (!targetTables.contains(t)) {
        missingOnTarget.add(t);
      }
    }
    List<String> extraOnTarget = new ArrayList<>();
    for (String t : targetTables) {
      if (!sourceTables.contains(t)) {
        extraOnTarget.add(t);
      }
    }
    if (!missingOnTarget.isEmpty() || !extraOnTarget.isEmpty()) {
      return new Result(
          false,
          "Table-set mismatch. missingOnTarget="
              + missingOnTarget
              + " extraOnTarget="
              + extraOnTarget,
          0,
          List.of());
    }

    List<String> countMismatches = new ArrayList<>();
    Map<String, Long> sourceCounts = new HashMap<>();
    for (String table : sourceTables) {
      long sc = countRows(source, table);
      long tc = countRows(target, table);
      sourceCounts.put(table, sc);
      if (sc != tc) {
        countMismatches.add(table + "(src=" + sc + ",tgt=" + tc + ")");
      }
    }
    if (!countMismatches.isEmpty()) {
      return new Result(
          false,
          "Row count mismatches: " + countMismatches,
          sourceTables.size(),
          List.copyOf(countMismatches));
    }

    String booleanProbe = probeBooleanLike(source, target, sourceTables);
    if (booleanProbe != null) {
      return new Result(false, booleanProbe, sourceTables.size(), List.of());
    }

    String clobProbe = probeClobLike(source, target, sourceTables);
    if (clobProbe != null) {
      return new Result(false, clobProbe, sourceTables.size(), List.of());
    }

    String nextNumberProbe = probeNextNumber(source, target, sourceTables);
    if (nextNumberProbe != null) {
      return new Result(false, nextNumberProbe, sourceTables.size(), List.of());
    }

    long totalRows = sourceCounts.values().stream().mapToLong(Long::longValue).sum();
    String summary =
        "OK tables="
            + sourceTables.size()
            + " totalRows="
            + totalRows
            + " boolean/CLOB/NEXTNUMBER probes passed";
    LOG.info(summary);
    return new Result(true, summary, sourceTables.size(), List.of());
  }

  static Set<String> listUserTables(Connection conn) throws SQLException {
    Set<String> names = new HashSet<>();
    DatabaseMetaData md = conn.getMetaData();
    try (ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String schema = rs.getString("TABLE_SCHEM");
        String name = rs.getString("TABLE_NAME");
        if (name == null) {
          continue;
        }
        if (schema != null) {
          String s = schema.toUpperCase(Locale.ROOT);
          if (s.equals("SYS") || s.equals("SYSTEM") || s.equals("INFORMATION_SCHEMA")) {
            continue;
          }
        }
        String n = name.toUpperCase(Locale.ROOT);
        if (n.startsWith("SYS")) {
          continue;
        }
        names.add(n);
      }
    }
    return names;
  }

  private static long countRows(Connection conn, String tableUpper) throws SQLException {
    String quoted = findQuotedTableName(conn, tableUpper);
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + quoted)) {
      if (rs.next()) {
        return rs.getLong(1);
      }
    }
    return 0;
  }

  private static String findQuotedTableName(Connection conn, String tableUpper)
      throws SQLException {
    DatabaseMetaData md = conn.getMetaData();
    try (ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        if (name != null && name.equalsIgnoreCase(tableUpper)) {
          return "\"" + name.replace("\"", "\"\"") + "\"";
        }
      }
    }
    return "\"" + tableUpper + "\"";
  }

  /**
   * Compare first boolean/bit-like column values for one sample row when present.
   *
   * @return error message or null if OK / not applicable
   */
  private static String probeBooleanLike(Connection source, Connection target, Set<String> tables)
      throws SQLException {
    for (String table : tables) {
      ColumnRef col = findColumnOfTypes(source, table, Types.BOOLEAN, Types.BIT, Types.CHAR);
      if (col == null || (col.dataType == Types.CHAR && col.size > 1)) {
        continue;
      }
      if (col.dataType == Types.CHAR && col.size == 1) {
        // Derby T/F flags
      } else if (col.dataType != Types.BOOLEAN && col.dataType != Types.BIT) {
        continue;
      }
      Object sVal = firstValue(source, table, col.name);
      Object tVal = firstValue(target, table, col.name);
      if (sVal == null && tVal == null) {
        return null;
      }
      if (normalizeBool(sVal) != null && normalizeBool(tVal) != null) {
        if (!normalizeBool(sVal).equals(normalizeBool(tVal))) {
          return "Boolean probe mismatch on " + table + "." + col.name;
        }
        return null; // one successful probe is enough
      }
    }
    return null;
  }

  private static String probeClobLike(Connection source, Connection target, Set<String> tables)
      throws SQLException {
    for (String table : tables) {
      ColumnRef col = findColumnOfTypes(source, table, Types.CLOB, Types.LONGVARCHAR);
      if (col == null) {
        continue;
      }
      Object sVal = firstValue(source, table, col.name);
      Object tVal = firstValue(target, table, col.name);
      if (sVal == null && tVal == null) {
        return null;
      }
      String ss = sVal == null ? null : String.valueOf(sVal);
      String ts = tVal == null ? null : String.valueOf(tVal);
      if (!Objects.equals(ss, ts)) {
        return "CLOB probe mismatch on " + table + "." + col.name;
      }
      return null;
    }
    return null;
  }

  private static String probeNextNumber(Connection source, Connection target, Set<String> tables)
      throws SQLException {
    if (!tables.contains("NEXTNUMBER")) {
      return null;
    }
    long sc = countRows(source, "NEXTNUMBER");
    long tc = countRows(target, "NEXTNUMBER");
    if (sc != tc) {
      return "NEXTNUMBER row count mismatch src=" + sc + " tgt=" + tc;
    }
    return null;
  }

  private static Boolean normalizeBool(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Boolean b) {
      return b;
    }
    if (v instanceof Number n) {
      return n.intValue() != 0;
    }
    String s = String.valueOf(v).trim();
    if (s.equalsIgnoreCase("T")
        || s.equalsIgnoreCase("Y")
        || s.equals("1")
        || s.equalsIgnoreCase("true")) {
      return true;
    }
    if (s.equalsIgnoreCase("F")
        || s.equalsIgnoreCase("N")
        || s.equals("0")
        || s.equalsIgnoreCase("false")) {
      return false;
    }
    return null;
  }

  private static Object firstValue(Connection conn, String tableUpper, String column)
      throws SQLException {
    String t = findQuotedTableName(conn, tableUpper);
    String c = "\"" + column.replace("\"", "\"\"") + "\"";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT " + c + " FROM " + t + " FETCH FIRST 1 ROW ONLY")) {
      if (rs.next()) {
        return rs.getObject(1);
      }
    } catch (SQLException e) {
      // Derby may not support FETCH FIRST in older modes — try LIMIT-less with maxRows
      try (Statement st = conn.createStatement()) {
        st.setMaxRows(1);
        try (ResultSet rs = st.executeQuery("SELECT " + c + " FROM " + t)) {
          if (rs.next()) {
            return rs.getObject(1);
          }
        }
      }
    }
    return null;
  }

  private static ColumnRef findColumnOfTypes(Connection conn, String tableUpper, int... types)
      throws SQLException {
    DatabaseMetaData md = conn.getMetaData();
    Set<Integer> want = new HashSet<>();
    for (int t : types) {
      want.add(t);
    }
    try (ResultSet tables = md.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (tables.next()) {
        String name = tables.getString("TABLE_NAME");
        String schema = tables.getString("TABLE_SCHEM");
        if (name == null || !name.equalsIgnoreCase(tableUpper)) {
          continue;
        }
        try (ResultSet cols = md.getColumns(null, schema, name, "%")) {
          while (cols.next()) {
            int dt = cols.getInt("DATA_TYPE");
            if (want.contains(dt)) {
              return new ColumnRef(cols.getString("COLUMN_NAME"), dt, cols.getInt("COLUMN_SIZE"));
            }
          }
        }
      }
    }
    return null;
  }

  private record ColumnRef(String name, int dataType, int size) {}
}
