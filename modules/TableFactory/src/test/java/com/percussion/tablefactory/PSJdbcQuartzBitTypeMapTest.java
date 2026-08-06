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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Quartz job-store flag columns ({@code IS_DURABLE}, etc.) use JDBC {@code BIT} so each backend
 * gets a boolean-compatible native type for {@code setBoolean}/{@code getBoolean} used by Quartz
 * delegates. Matrix DBs must all resolve BIT.
 */
@Tag("UnitTest")
class PSJdbcQuartzBitTypeMapTest {

  @Test
  void bitMapsToBooleanCompatibleNativeOnAllMatrixDrivers() throws Exception {
    // H2 / PostgreSQL: native BOOLEAN (Quartz boolean bind)
    assertNative("h2", "BOOLEAN");
    assertNative("postgresql", "BOOLEAN");

    // MySQL / SQL Server: native BIT (JDBC boolean bind works)
    assertNative("mysql", "BIT");
    assertNative("sqlserver", "BIT");
    assertNative("jtds:sqlserver", "BIT");
  }

  private static void assertNative(String driver, String expectedNative) throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap(null, driver, null);
    PSJdbcDataTypeMapping bit = map.getMapping("BIT");
    assertNotNull(bit, "BIT mapping missing for driver=" + driver);
    assertEquals(
        expectedNative,
        bit.getNative(),
        "BIT native type for driver=" + driver + " must stay boolean-compatible for Quartz");
  }
}
