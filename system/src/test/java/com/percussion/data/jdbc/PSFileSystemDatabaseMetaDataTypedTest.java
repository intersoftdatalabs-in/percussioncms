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
package com.percussion.data.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed metadata result construction in {@link PSFileSystemDatabaseMetaData}.
 */
@Tag("UnitTest")
class PSFileSystemDatabaseMetaDataTypedTest {

  @Test
  void getTableTypesReturnsDirectoryAndFile() throws SQLException {
    PSFileSystemDatabaseMetaData meta = new PSFileSystemDatabaseMetaData();
    try (ResultSet rs = meta.getTableTypes()) {
      assertTrue(rs.next());
      assertEquals("DIRECTORY", rs.getString("TABLE_TYPE"));
      assertTrue(rs.next());
      assertEquals("FILE", rs.getString(1));
      assertFalse(rs.next());
    }
  }

  @Test
  void getPrimaryKeysReturnsFullnameColumn() throws SQLException {
    PSFileSystemDatabaseMetaData meta = new PSFileSystemDatabaseMetaData();
    try (ResultSet rs = meta.getPrimaryKeys("C:/catalog", "", "sample.txt")) {
      assertTrue(rs.next());
      assertEquals("C:/catalog", rs.getString("TABLE_CAT"));
      assertEquals("", rs.getString("TABLE_SCHEM"));
      assertEquals("sample.txt", rs.getString("TABLE_NAME"));
      assertEquals("fullname", rs.getString("COLUMN_NAME"));
      assertEquals(1, rs.getInt("KEY_SEQ"));
      assertFalse(rs.next());
    }
  }
}
