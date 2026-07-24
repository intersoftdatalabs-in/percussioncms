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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Driver-correct column rename SQL for recreate-style ALTER (#548 / PR #1494 Kilo).
 */
@Tag("UnitTest")
public class PSJdbcStatementFactoryColumnRenameTest {

  @Test
  void h2UsesAlterTableAlterColumnRenameTo() {
    String sql =
        PSJdbcStatementFactory.buildColumnRenameStatement(
            PSJdbcUtils.H2_DRIVER, "MY_TABLE", "COL_NEW", "COL");
    assertEquals("ALTER TABLE MY_TABLE ALTER COLUMN COL_NEW RENAME TO COL", sql);
    assertFalse(sql.startsWith("RENAME COLUMN"));
  }

  @Test
  void derbyUsesRenameColumnQualifiedForm() {
    String sql =
        PSJdbcStatementFactory.buildColumnRenameStatement(
            PSJdbcUtils.DERBY_DRIVER, "MY_TABLE", "COL_NEW", "COL");
    assertEquals("RENAME COLUMN MY_TABLE.COL_NEW TO COL", sql);
    assertTrue(sql.startsWith("RENAME COLUMN"));
  }
}
