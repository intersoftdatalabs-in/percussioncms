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
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.tablefactory.PSJdbcDbmsDef;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * In-memory H2 integration tests for {@link PSLocaleCodeMigrator}. Uses only relative/mem JDBC URLs
 * — no filesystem path assertions (cross-platform).
 */
@Tag("UnitTest")
class PSLocaleCodeMigratorTest {

  @Test
  void migrate_rewritesSysLangAndContentLocale_idempotentSecondPass() throws Exception {
    Properties props = memDbmsProps();
    PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
    List<String> logs = new ArrayList<>();

    try (Connection conn =
        DriverManager.getConnection(props.getProperty(PSJdbcDbmsDef.DB_SERVER_PROPERTY))) {
      createSchema(conn);
      seedData(conn);

      PSLocaleCodeMigrator migrator = new PSLocaleCodeMigrator(logs::add);
      PSLocaleCodeMigrator.Result first = migrator.migrate(conn, dbmsDef, false);

      assertEquals(3, first.sysLangScanned());
      assertEquals(2, first.sysLangRewritten()); // hi + en_US; es stays
      assertEquals(3, first.contentLocaleScanned());
      assertEquals(2, first.contentLocaleRewritten()); // es + en_US; en-us stays

      assertEquals("hi-in", querySysLang(conn, "alice"));
      assertEquals("es", querySysLang(conn, "bob"));
      assertEquals("en-us", querySysLang(conn, "carol"));
      assertEquals("es-es", queryContentLocale(conn, 1));
      assertEquals("en-us", queryContentLocale(conn, 2));
      assertEquals("en-us", queryContentLocale(conn, 3));

      assertTrue(logs.stream().anyMatch(l -> l.contains("hi -> hi-in")));
      assertTrue(logs.stream().anyMatch(l -> l.contains("es -> es-es")));
      assertTrue(logs.stream().noneMatch(l -> l.contains("delete")), "must never log deletes");

      logs.clear();
      PSLocaleCodeMigrator.Result second = migrator.migrate(conn, dbmsDef, false);
      assertEquals(0, second.sysLangRewritten());
      assertEquals(0, second.contentLocaleRewritten());
    }
  }

  @Test
  void dryRun_countsWithoutCommitting() throws Exception {
    Properties props = memDbmsProps();
    PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
    List<String> logs = new ArrayList<>();

    try (Connection conn =
        DriverManager.getConnection(props.getProperty(PSJdbcDbmsDef.DB_SERVER_PROPERTY))) {
      createSchema(conn);
      seedData(conn);

      PSLocaleCodeMigrator migrator = new PSLocaleCodeMigrator(logs::add);
      PSLocaleCodeMigrator.Result dry = migrator.migrate(conn, dbmsDef, true);

      assertEquals(2, dry.sysLangRewritten());
      assertEquals(2, dry.contentLocaleRewritten());
      // dry-run rolls back — original values remain
      assertEquals("hi", querySysLang(conn, "alice"));
      assertEquals("es", queryContentLocale(conn, 1));
      assertTrue(logs.stream().anyMatch(l -> l.contains("[dry-run]")));
      assertTrue(logs.stream().anyMatch(l -> l.contains("dry-run complete")));
    }
  }

  private static Properties memDbmsProps() {
    // Unique DB name per test method invocation to avoid H2 static cross-talk.
    String url = "jdbc:h2:mem:i18n_locale_mig_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    Properties props = new Properties();
    props.setProperty(PSJdbcDbmsDef.DB_DRIVER_NAME_PROPERTY, "h2");
    props.setProperty(PSJdbcDbmsDef.DB_DRIVER_CLASS_NAME_PROPERTY, "org.h2.Driver");
    props.setProperty(PSJdbcDbmsDef.DB_SERVER_PROPERTY, url);
    props.setProperty(PSJdbcDbmsDef.UID_PROPERTY, "sa");
    props.setProperty(PSJdbcDbmsDef.PWD_PROPERTY, "");
    props.setProperty(PSJdbcDbmsDef.DB_NAME_PROPERTY, "");
    props.setProperty(PSJdbcDbmsDef.DB_SCHEMA_PROPERTY, "PUBLIC");
    return props;
  }

  private static void createSchema(Connection conn) throws Exception {
    try (Statement st = conn.createStatement()) {
      st.execute(
          "CREATE TABLE PSX_PERSISTEDPROPERTYVALUES ("
              + "CONTEXT VARCHAR(25) NOT NULL,"
              + "USERNAME VARCHAR(100) NOT NULL,"
              + "CATEGORY VARCHAR(25) NOT NULL,"
              + "PROPERTYNAME VARCHAR(50) NOT NULL,"
              + "PROPERTYVALUE CLOB NOT NULL,"
              + "PRIMARY KEY (USERNAME, PROPERTYNAME, CATEGORY, CONTEXT))");
      st.execute(
          "CREATE TABLE CONTENTSTATUS ("
              + "CONTENTID INT NOT NULL PRIMARY KEY,"
              + "LOCALE VARCHAR(50))");
    }
  }

  private static void seedData(Connection conn) throws Exception {
    try (Statement st = conn.createStatement()) {
      st.execute(
          "INSERT INTO PSX_PERSISTEDPROPERTYVALUES"
              + " (CONTEXT, USERNAME, CATEGORY, PROPERTYNAME, PROPERTYVALUE)"
              + " VALUES ('sess', 'alice', 'sys', 'sys_lang', 'hi')");
      st.execute(
          "INSERT INTO PSX_PERSISTEDPROPERTYVALUES"
              + " (CONTEXT, USERNAME, CATEGORY, PROPERTYNAME, PROPERTYVALUE)"
              + " VALUES ('sess', 'bob', 'sys', 'sys_lang', 'es')");
      st.execute(
          "INSERT INTO PSX_PERSISTEDPROPERTYVALUES"
              + " (CONTEXT, USERNAME, CATEGORY, PROPERTYNAME, PROPERTYVALUE)"
              + " VALUES ('sess', 'carol', 'sys', 'sys_lang', 'en_US')");
      st.execute("INSERT INTO CONTENTSTATUS (CONTENTID, LOCALE) VALUES (1, 'es')");
      st.execute("INSERT INTO CONTENTSTATUS (CONTENTID, LOCALE) VALUES (2, 'en-us')");
      st.execute("INSERT INTO CONTENTSTATUS (CONTENTID, LOCALE) VALUES (3, 'en_US')");
    }
  }

  private static String querySysLang(Connection conn, String user) throws Exception {
    try (var ps =
        conn.prepareStatement(
            "SELECT PROPERTYVALUE FROM PSX_PERSISTEDPROPERTYVALUES WHERE USERNAME=?")) {
      ps.setString(1, user);
      try (var rs = ps.executeQuery()) {
        assertTrue(rs.next());
        return rs.getString(1);
      }
    }
  }

  private static String queryContentLocale(Connection conn, int contentId) throws Exception {
    try (var ps = conn.prepareStatement("SELECT LOCALE FROM CONTENTSTATUS WHERE CONTENTID=?")) {
      ps.setInt(1, contentId);
      try (var rs = ps.executeQuery()) {
        assertTrue(rs.next());
        return rs.getString(1);
      }
    }
  }
}
