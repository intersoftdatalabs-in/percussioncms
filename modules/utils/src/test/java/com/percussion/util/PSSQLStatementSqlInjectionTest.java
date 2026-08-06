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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.SecureStringUtils;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for shared SQL statement guards used by {@link PSSQLStatement} (CodeQL {@code
 * java/sql-injection} #661 / residual #1765, T042). General Statement path rejects multi-statement
 * only; {@link PSSQLStatement#getStatement(Connection)} always returns the wrapper so the guard
 * applies even when debug logging is off.
 */
@DisplayName("PSSQLStatement — multi-statement rejection via SecureStringUtils (T042)")
class PSSQLStatementSqlInjectionTest {

  @Test
  void acceptsSingleSelectAndStripsTrailingSemicolon() {
    assertEquals(
        "SELECT COUNT(*) FROM CONTENTSTATUS",
        SecureStringUtils.requireSingleSqlStatement("SELECT COUNT(*) FROM CONTENTSTATUS;"));
  }

  @Test
  void rejectsStackedStatements() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSingleSqlStatement("SELECT 1; DROP TABLE CONTENTSTATUS"));
  }

  @Test
  void generalGuardAllowsCommentMarkersInsideLiterals() {
    // Intentionally allowed on the general Statement path (kilo review): comments in
    // string literals / hints must not fail the whole codebase.
    assertEquals(
        "SELECT * FROM T WHERE name = 'a--b'",
        SecureStringUtils.requireSingleSqlStatement("SELECT * FROM T WHERE name = 'a--b'"));
  }

  @Test
  void getStatementAlwaysReturnsWrapper() throws SQLException {
    Connection conn = proxyConnection(proxyStatement(new AtomicReference<>()));
    Statement stmt = PSSQLStatement.getStatement(conn);
    assertInstanceOf(PSSQLStatement.class, stmt);
    stmt.close();
  }

  @Test
  void executeQueryRejectsStackedSqlBeforeDelegate() throws SQLException {
    AtomicReference<String> captured = new AtomicReference<>();
    Statement stmt = new PSSQLStatement(proxyStatement(captured));

    assertThrows(
        IllegalArgumentException.class,
        () -> stmt.executeQuery("SELECT 1; DROP TABLE CONTENTSTATUS"));
    // Delegate must not have been called with the stacked payload.
    assertTrue(captured.get() == null || !captured.get().contains("DROP"));

    ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL;");
    assertEquals(null, rs);
    assertEquals("SELECT 1 FROM DUAL", captured.get());
    stmt.close();
  }

  @Test
  void executeUpdateRejectsStackedSql() throws SQLException {
    AtomicReference<String> captured = new AtomicReference<>();
    Statement stmt = new PSSQLStatement(proxyStatement(captured));

    assertThrows(
        IllegalArgumentException.class, () -> stmt.executeUpdate("UPDATE T SET x=1; DROP TABLE T"));
    stmt.executeUpdate("UPDATE T SET x=1");
    assertEquals("UPDATE T SET x=1", captured.get());
    stmt.close();
  }

  private static Connection proxyConnection(Statement statement) {
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if ("createStatement".equals(name)) {
                return statement;
              }
              if ("close".equals(name)) {
                return null;
              }
              return defaultReturn(method.getReturnType());
            });
  }

  private static Statement proxyStatement(AtomicReference<String> capturedSql) {
    return (Statement)
        Proxy.newProxyInstance(
            Statement.class.getClassLoader(),
            new Class<?>[] {Statement.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if (("executeQuery".equals(name)
                      || "executeUpdate".equals(name)
                      || "execute".equals(name)
                      || "addBatch".equals(name))
                  && args != null
                  && args.length >= 1
                  && args[0] instanceof String) {
                capturedSql.set((String) args[0]);
                if ("executeQuery".equals(name)) {
                  return null;
                }
                if ("executeUpdate".equals(name)) {
                  return 0;
                }
                if ("execute".equals(name)) {
                  return false;
                }
                return null;
              }
              if ("close".equals(name)) {
                return null;
              }
              return defaultReturn(method.getReturnType());
            });
  }

  private static Object defaultReturn(Class<?> returnType) {
    if (returnType == boolean.class) {
      return false;
    }
    if (returnType == int.class) {
      return 0;
    }
    if (returnType == long.class) {
      return 0L;
    }
    if (returnType == void.class) {
      return null;
    }
    return null;
  }
}
