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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.security.SecureStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Factory-path SQL guard (CodeQL {@code java/sql-injection} #657, T042) via shared {@link
 * SecureStringUtils#requireFactorySqlStatement(String)}.
 */
@DisplayName("PSJdbcResultSetIteratorStep — factory SQL guard (T042)")
class PSJdbcResultSetIteratorStepSqlInjectionTest {

  @Test
  void acceptsSingleSelect() {
    assertEquals(
        "SELECT * FROM RXSITES WHERE 1=0",
        SecureStringUtils.requireFactorySqlStatement("SELECT * FROM RXSITES WHERE 1=0"));
  }

  @Test
  void rejectsMultiStatement() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SecureStringUtils.requireFactorySqlStatement(
                "SELECT 1 FROM DUAL; DELETE FROM RXSITES"));
  }

  @Test
  void rejectsSqlCommentsOnFactoryPath() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireFactorySqlStatement("SELECT 1 -- comment"));
  }
}
