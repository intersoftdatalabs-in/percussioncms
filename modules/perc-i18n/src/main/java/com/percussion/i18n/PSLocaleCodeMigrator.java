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
package com.percussion.i18n;

import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.util.PSSqlHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One-time upgrade migrator that rewrites persisted locale codes (GH-1547).
 *
 * <p>Targets:
 *
 * <ul>
 *   <li>{@code PSX_PERSISTEDPROPERTYVALUES.PROPERTYVALUE} where {@code PROPERTYNAME='sys_lang'}
 *   <li>{@code CONTENTSTATUS.LOCALE} (issue text may call this CT_LOCALE) bare {@code es} → {@code
 *       es-es}
 * </ul>
 *
 * <p>Uses portable prepared statements against table names qualified via {@link PSSqlHelper} (same
 * idiom as {@code PSLocaleHandler} / tablefactory-style updates). Never deletes rows. Logs every
 * rewrite. Runs inside a single transaction; dry-run counts only and rolls back.
 */
public final class PSLocaleCodeMigrator {

  public static final String TABLE_PERSISTED_VALUES = "PSX_PERSISTEDPROPERTYVALUES";
  public static final String TABLE_CONTENT_STATUS = "CONTENTSTATUS";
  public static final String PROP_SYS_LANG = "sys_lang";

  private final Consumer<String> log;

  /**
   * @param log sink for operator-visible messages; must not be {@code null}
   */
  public PSLocaleCodeMigrator(Consumer<String> log) {
    this.log = Objects.requireNonNull(log, "log");
  }

  /**
   * Runs the migration on an open connection.
   *
   * @param conn open JDBC connection (caller owns lifecycle)
   * @param dbmsDef repository DBMS definition used for table qualification
   * @param dryRun when {@code true}, counts and logs would-be rewrites but does not commit
   * @return scan/rewrite counts
   * @throws SQLException on JDBC failure (connection left rolled back when possible)
   */
  public Result migrate(Connection conn, PSJdbcDbmsDef dbmsDef, boolean dryRun)
      throws SQLException {
    Objects.requireNonNull(conn, "conn");
    Objects.requireNonNull(dbmsDef, "dbmsDef");

    boolean previousAutoCommit = conn.getAutoCommit();
    conn.setAutoCommit(false);
    try {
      Result result = new Result();
      rewriteSysLangRows(conn, dbmsDef, dryRun, result);
      rewriteContentLocaleRows(conn, dbmsDef, dryRun, result);

      if (dryRun) {
        conn.rollback();
        log.accept(
            "i18n locale migration dry-run complete (rolled back): "
                + "sys_lang rewritten="
                + result.sysLangRewritten()
                + "/"
                + result.sysLangScanned()
                + ", CONTENTSTATUS.LOCALE rewritten="
                + result.contentLocaleRewritten()
                + "/"
                + result.contentLocaleScanned());
      } else {
        conn.commit();
        log.accept(
            "i18n locale migration committed: "
                + "sys_lang rewritten="
                + result.sysLangRewritten()
                + "/"
                + result.sysLangScanned()
                + ", CONTENTSTATUS.LOCALE rewritten="
                + result.contentLocaleRewritten()
                + "/"
                + result.contentLocaleScanned());
      }
      return result;
    } catch (SQLException | RuntimeException e) {
      try {
        conn.rollback();
      } catch (SQLException rollbackEx) {
        e.addSuppressed(rollbackEx);
      }
      log.accept("i18n locale migration failed and rolled back: " + e.getMessage());
      throw e;
    } finally {
      try {
        conn.setAutoCommit(previousAutoCommit);
      } catch (SQLException ignore) {
        // leave connection state as-is if restore fails
      }
    }
  }

  private void rewriteSysLangRows(
      Connection conn, PSJdbcDbmsDef dbmsDef, boolean dryRun, Result result) throws SQLException {
    String table =
        PSSqlHelper.qualifyTableName(
            TABLE_PERSISTED_VALUES,
            dbmsDef.getDataBase(),
            dbmsDef.getSchema(),
            dbmsDef.getDriver());

    String selectSql =
        "SELECT USERNAME, PROPERTYNAME, CATEGORY, CONTEXT, PROPERTYVALUE FROM "
            + table
            + " WHERE PROPERTYNAME = ?";
    String updateSql =
        "UPDATE "
            + table
            + " SET PROPERTYVALUE = ? WHERE USERNAME = ? AND PROPERTYNAME = ? AND CATEGORY = ?"
            + " AND CONTEXT = ?";

    List<SysLangRow> pending = new ArrayList<>();
    try (PreparedStatement select = conn.prepareStatement(selectSql)) {
      select.setString(1, PROP_SYS_LANG);
      try (ResultSet rs = select.executeQuery()) {
        while (rs.next()) {
          result.sysLangScanned++;
          String user = rs.getString(1);
          String propName = rs.getString(2);
          String category = rs.getString(3);
          String context = rs.getString(4);
          String value = rs.getString(5);
          if (!PSLocaleCodeRewrite.sysLangNeedsRewrite(value)) {
            continue;
          }
          String rewritten = PSLocaleCodeRewrite.rewriteSysLang(value);
          pending.add(new SysLangRow(user, propName, category, context, value, rewritten));
        }
      }
    }

    if (pending.isEmpty()) {
      log.accept("sys_lang: no rows require rewrite (scanned=" + result.sysLangScanned + ")");
      return;
    }

    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
      for (SysLangRow row : pending) {
        log.accept(
            "sys_lang rewrite user="
                + row.user
                + " context="
                + row.context
                + " "
                + row.from
                + " -> "
                + row.to
                + (dryRun ? " [dry-run]" : ""));
        if (!dryRun) {
          update.setString(1, row.to);
          update.setString(2, row.user);
          update.setString(3, row.propName);
          update.setString(4, row.category);
          update.setString(5, row.context);
          update.executeUpdate();
        }
        result.sysLangRewritten++;
      }
    }
  }

  private void rewriteContentLocaleRows(
      Connection conn, PSJdbcDbmsDef dbmsDef, boolean dryRun, Result result) throws SQLException {
    String table =
        PSSqlHelper.qualifyTableName(
            TABLE_CONTENT_STATUS, dbmsDef.getDataBase(), dbmsDef.getSchema(), dbmsDef.getDriver());

    // Scan all non-null locales so underscore/case forms are normalized; promote es → es-es.
    String selectSql = "SELECT CONTENTID, LOCALE FROM " + table + " WHERE LOCALE IS NOT NULL";
    String updateSql = "UPDATE " + table + " SET LOCALE = ? WHERE CONTENTID = ?";

    List<ContentLocaleRow> pending = new ArrayList<>();
    try (PreparedStatement select = conn.prepareStatement(selectSql);
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        result.contentLocaleScanned++;
        int contentId = rs.getInt(1);
        String locale = rs.getString(2);
        if (!PSLocaleCodeRewrite.contentLocaleNeedsRewrite(locale)) {
          continue;
        }
        String rewritten = PSLocaleCodeRewrite.rewriteContentLocale(locale);
        pending.add(new ContentLocaleRow(contentId, locale, rewritten));
      }
    }

    if (pending.isEmpty()) {
      log.accept(
          "CONTENTSTATUS.LOCALE: no rows require rewrite (scanned="
              + result.contentLocaleScanned
              + ")");
      return;
    }

    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
      for (ContentLocaleRow row : pending) {
        log.accept(
            "CONTENTSTATUS.LOCALE rewrite contentId="
                + row.contentId
                + " "
                + row.from
                + " -> "
                + row.to
                + (dryRun ? " [dry-run]" : ""));
        if (!dryRun) {
          update.setString(1, row.to);
          update.setInt(2, row.contentId);
          update.executeUpdate();
        }
        result.contentLocaleRewritten++;
      }
    }
  }

  /** Mutable counters returned to callers (also exposed as immutable accessors). */
  public static final class Result {
    private int sysLangScanned;
    private int sysLangRewritten;
    private int contentLocaleScanned;
    private int contentLocaleRewritten;

    public int sysLangScanned() {
      return sysLangScanned;
    }

    public int sysLangRewritten() {
      return sysLangRewritten;
    }

    public int contentLocaleScanned() {
      return contentLocaleScanned;
    }

    public int contentLocaleRewritten() {
      return contentLocaleRewritten;
    }
  }

  private record SysLangRow(
      String user, String propName, String category, String context, String from, String to) {}

  private record ContentLocaleRow(int contentId, String from, String to) {}
}
