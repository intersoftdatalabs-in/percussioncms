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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class JdbcSystemDefColumnSchemaTest {

  @Test
  void ensureColumn_createsAndIsIdempotentOnH2() throws Exception {
    String url = "jdbc:h2:mem:sysdefcol" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    try (Connection setup = connections.get();
        Statement st = setup.createStatement()) {
      st.execute("CREATE TABLE CONTENTSTATUS (CONTENTID INT PRIMARY KEY)");
    }
    JdbcSystemDefColumnSchema schema = new JdbcSystemDefColumnSchema(connections);

    schema.ensureColumn("CONTENTSTATUS", "QA4037PROBE", "text", "50");
    schema.ensureColumn("CONTENTSTATUS", "QA4037PROBE", "text", "50");

    try (Connection check = connections.get()) {
      assertTrue(JdbcSystemDefColumnSchema.columnExists(check, "CONTENTSTATUS", "QA4037PROBE"));
    }
  }

  @Test
  void dropColumnIfPresent_missingIsNoOpAndExistingDrops() throws Exception {
    String url = "jdbc:h2:mem:sysdefdrop" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    try (Connection setup = connections.get();
        Statement st = setup.createStatement()) {
      st.execute("CREATE TABLE CONTENTSTATUS (CONTENTID INT PRIMARY KEY, QA4037PROBE VARCHAR(50))");
    }
    JdbcSystemDefColumnSchema schema = new JdbcSystemDefColumnSchema(connections);

    schema.dropColumnIfPresent("CONTENTSTATUS", "MISSINGCOL");
    schema.dropColumnIfPresent("CONTENTSTATUS", "QA4037PROBE");

    try (Connection check = connections.get()) {
      assertFalse(JdbcSystemDefColumnSchema.columnExists(check, "CONTENTSTATUS", "QA4037PROBE"));
    }
  }

  @Test
  void columnExists_upperInputFindsLowercaseStoredNames() throws Exception {
    String url =
        "jdbc:h2:mem:sysdefcase"
            + System.nanoTime()
            + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    try (Connection setup = connections.get();
        Statement st = setup.createStatement()) {
      st.execute("CREATE TABLE contentstatus (contentid INT PRIMARY KEY, qa4037probe VARCHAR(50))");
    }
    try (Connection check = connections.get()) {
      assertTrue(JdbcSystemDefColumnSchema.columnExists(check, "CONTENTSTATUS", "QA4037PROBE"));
    }
  }

  @Test
  void columnExists_probesLowerCaseAndNullSchemaFallback() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(md);
    when(conn.getCatalog()).thenReturn(null);
    when(conn.getSchema()).thenReturn(null);
    when(md.getUserName()).thenReturn("sa");
    when(md.getColumns(any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              ResultSet rs = mock(ResultSet.class);
              String schema = inv.getArgument(1);
              String table = inv.getArgument(2);
              String column = inv.getArgument(3);
              boolean hit =
                  "sa".equals(schema)
                      && "contentstatus".equals(table)
                      && "qa4037probe".equals(column);
              when(rs.next()).thenReturn(hit);
              return rs;
            });

    assertTrue(JdbcSystemDefColumnSchema.columnExists(conn, "CONTENTSTATUS", "QA4037PROBE"));
  }

  @Test
  void identCases_alwaysIncludesLowerWhenInputIsUpper() {
    List<String> cases = JdbcSystemDefColumnSchema.identCases("CONTENTSTATUS");
    assertTrue(cases.contains("CONTENTSTATUS"));
    assertTrue(cases.contains("contentstatus"));
  }

  @Test
  void requireIdent_rejectsUnsafeNames() {
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcSystemDefColumnSchema.requireIdent("CONTENT STATUS", "table"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcSystemDefColumnSchema.requireIdent("x;drop", "column"));
    assertThrows(
        IllegalArgumentException.class, () -> JdbcSystemDefColumnSchema.requireIdent("1col", "column"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcSystemDefColumnSchema.requireIdent("SELECT", "column"));
    assertThrows(
        IllegalArgumentException.class, () -> JdbcSystemDefColumnSchema.requireIdent("user", "column"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcSystemDefColumnSchema.requireIdent("TABLE", "table"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcSystemDefColumnSchema.requireIdent("ORDER", "column"));
    org.junit.jupiter.api.Assertions.assertEquals(
        "CONTENTSTATUS", JdbcSystemDefColumnSchema.requireIdent("CONTENTSTATUS", "table"));
  }

  @Test
  void parseTextSize_defaultsWhenBlankOrInvalid() {
    org.junit.jupiter.api.Assertions.assertEquals(50, JdbcSystemDefColumnSchema.parseTextSize(null));
    org.junit.jupiter.api.Assertions.assertEquals(50, JdbcSystemDefColumnSchema.parseTextSize("abc"));
    org.junit.jupiter.api.Assertions.assertEquals(80, JdbcSystemDefColumnSchema.parseTextSize("80"));
  }

  private static Connection open(String url) {
    try {
      return DriverManager.getConnection(url);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
