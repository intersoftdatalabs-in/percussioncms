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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** H2 schema persist for local-field ALTER (do not stub past the column create). */
@Tag("UnitTest")
class JdbcContentTypeLocalFieldColumnSchemaTest {

  @Test
  void ensureColumn_createsAndIsIdempotentOnH2() throws Exception {
    String url = "jdbc:h2:mem:ctlocalcol" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    try (Connection setup = connections.get();
        Statement st = setup.createStatement()) {
      st.execute("CREATE TABLE PERCPAGE (CONTENTID INT PRIMARY KEY)");
    }
    JdbcContentTypeLocalFieldColumnSchema schema =
        new JdbcContentTypeLocalFieldColumnSchema(connections);

    schema.ensureColumn("PERCPAGE", "RXCD03PROBE", "text", "50");
    schema.ensureColumn("PERCPAGE", "RXCD03PROBE", "text", "50");

    try (Connection check = connections.get()) {
      assertTrue(
          JdbcContentTypeLocalFieldColumnSchema.columnExists(check, "PERCPAGE", "RXCD03PROBE"));
    }
  }

  @Test
  void columnExists_upperInputFindsLowercaseStoredNames() throws Exception {
    String url =
        "jdbc:h2:mem:ctlocalcase" + System.nanoTime() + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    try (Connection setup = connections.get();
        Statement st = setup.createStatement()) {
      st.execute("CREATE TABLE percpage (contentid INT PRIMARY KEY, rxcd03probe VARCHAR(50))");
    }
    try (Connection check = connections.get()) {
      assertTrue(
          JdbcContentTypeLocalFieldColumnSchema.columnExists(check, "PERCPAGE", "RXCD03PROBE"));
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
                      && "percpage".equals(table)
                      && "rxcd03probe".equals(column);
              when(rs.next()).thenReturn(hit);
              return rs;
            });

    assertTrue(
        JdbcContentTypeLocalFieldColumnSchema.columnExists(conn, "PERCPAGE", "RXCD03PROBE"));
  }

  @Test
  void identCases_alwaysIncludesLowerWhenInputIsUpper() {
    List<String> cases = JdbcContentTypeLocalFieldColumnSchema.identCases("PERCPAGE");
    assertTrue(cases.contains("PERCPAGE"));
    assertTrue(cases.contains("percpage"));
  }

  @Test
  void requireIdent_rejectsUnsafeNames() {
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcContentTypeLocalFieldColumnSchema.requireIdent("PERC PAGE", "table"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcContentTypeLocalFieldColumnSchema.requireIdent("x;drop", "column"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcContentTypeLocalFieldColumnSchema.requireIdent("1col", "column"));
    assertThrows(
        IllegalArgumentException.class,
        () -> JdbcContentTypeLocalFieldColumnSchema.requireIdent("SELECT", "column"));
    assertEquals(
        "PERCPAGE", JdbcContentTypeLocalFieldColumnSchema.requireIdent("PERCPAGE", "table"));
  }

  @Test
  void parseTextSize_defaultsWhenBlankOrInvalid() {
    assertEquals(50, JdbcContentTypeLocalFieldColumnSchema.parseTextSize(null));
    assertEquals(50, JdbcContentTypeLocalFieldColumnSchema.parseTextSize("abc"));
    assertEquals(80, JdbcContentTypeLocalFieldColumnSchema.parseTextSize("80"));
  }

  @Test
  void missingColumn_isFalseOnEmptyTable() throws Exception {
    String url = "jdbc:h2:mem:ctlocalmiss" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    try (Connection conn = open(url);
        Statement st = conn.createStatement()) {
      st.execute("CREATE TABLE PERCPAGE (CONTENTID INT PRIMARY KEY)");
      assertFalse(
          JdbcContentTypeLocalFieldColumnSchema.columnExists(conn, "PERCPAGE", "RXCD03PROBE"));
    }
  }

  private static Connection open(String url) {
    try {
      return DriverManager.getConnection(url);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
