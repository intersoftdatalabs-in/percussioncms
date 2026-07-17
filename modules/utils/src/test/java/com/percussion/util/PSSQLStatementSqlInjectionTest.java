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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSSQLStatement#requireSingleSqlStatement(String)} (CodeQL {@code
 * java/sql-injection} #661, T042).
 */
@DisplayName("PSSQLStatement.requireSingleSqlStatement — multi-statement rejection (T042)")
class PSSQLStatementSqlInjectionTest {

  @Test
  void acceptsSingleSelectAndStripsTrailingSemicolon() {
    assertEquals(
        "SELECT COUNT(*) FROM CONTENTSTATUS",
        PSSQLStatement.requireSingleSqlStatement("SELECT COUNT(*) FROM CONTENTSTATUS;"));
  }

  @Test
  void rejectsStackedStatements() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSSQLStatement.requireSingleSqlStatement(
                "SELECT 1; DROP TABLE CONTENTSTATUS"));
  }

  @Test
  void rejectsSqlComments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSSQLStatement.requireSingleSqlStatement("SELECT 1 -- comment"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSSQLStatement.requireSingleSqlStatement("SELECT 1 /* evil */"));
  }
}
