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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Dialect selection for install-time {@link PSExecSQLStmt} (#548 H2, #1500 PostgreSQL). Ensures H2
 * does not fall through to MSSQL {@code str()} SQL and that PostgreSQL has its own branch.
 */
@Tag("UnitTest")
class PSExecSQLStmtDialectTest {

  private static final String DEFAULT_SQL = "select ltrim(str(id)) from t";
  private static final String DERBY_SQL = "select rtrim(char(id)) from t";
  private static final String H2_SQL = "select rtrim(cast(id as varchar)) from t";
  private static final String PG_SQL = "select cast(id as varchar) from t";
  private static final String MYSQL_SQL = "select cast(id as char) from t";

  @Test
  void h2UsesExplicitSqlH2() {
    String resolved =
        PSExecSQLStmt.resolveDialectSql(
            PSJdbcUtils.H2_DRIVER,
            DEFAULT_SQL,
            "",
            "",
            "",
            DERBY_SQL,
            MYSQL_SQL,
            H2_SQL,
            PG_SQL);
    assertEquals(H2_SQL, resolved);
  }

  @Test
  void h2FallsBackToSqlDerbyWhenSqlH2Empty() {
    String resolved =
        PSExecSQLStmt.resolveDialectSql(
            "h2", DEFAULT_SQL, "", "", "", DERBY_SQL, MYSQL_SQL, "", PG_SQL);
    assertEquals(DERBY_SQL, resolved);
    assertTrue(resolved.contains("char"), "must not use MSSQL str() default for H2");
  }

  @Test
  void postgresqlUsesSqlPostgresql() {
    String resolved =
        PSExecSQLStmt.resolveDialectSql(
            PSJdbcUtils.POSTGRES_DRIVER,
            DEFAULT_SQL,
            "",
            "",
            "",
            DERBY_SQL,
            MYSQL_SQL,
            H2_SQL,
            PG_SQL);
    assertEquals(PG_SQL, resolved);

    String alias =
        PSExecSQLStmt.resolveDialectSql(
            "postgres", DEFAULT_SQL, "", "", "", DERBY_SQL, MYSQL_SQL, H2_SQL, PG_SQL);
    assertEquals(PG_SQL, alias);
  }

  @Test
  void postgresqlFallsBackToDefaultWhenNoDialectSql() {
    String resolved =
        PSExecSQLStmt.resolveDialectSql(
            "postgresql", DEFAULT_SQL, "", "", "", DERBY_SQL, MYSQL_SQL, H2_SQL, "");
    assertEquals(DEFAULT_SQL, resolved);
  }

  @Test
  void derbyAndMysqlUnchanged() {
    assertEquals(
        DERBY_SQL,
        PSExecSQLStmt.resolveDialectSql(
            PSJdbcUtils.DERBY_DRIVER,
            DEFAULT_SQL,
            "",
            "",
            "",
            DERBY_SQL,
            MYSQL_SQL,
            H2_SQL,
            PG_SQL));
    assertEquals(
        MYSQL_SQL,
        PSExecSQLStmt.resolveDialectSql(
            PSJdbcUtils.MYSQL_DRIVER,
            DEFAULT_SQL,
            "",
            "",
            "",
            DERBY_SQL,
            MYSQL_SQL,
            H2_SQL,
            PG_SQL));
  }
}
