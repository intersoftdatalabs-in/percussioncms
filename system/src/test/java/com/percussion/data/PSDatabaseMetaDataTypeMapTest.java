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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed data-type maps on {@link PSDatabaseMetaData} (rawtypes cleanup
 * slice #2298).
 */
@Tag("UnitTest")
class PSDatabaseMetaDataTypeMapTest {

  @Test
  void guessNativeDataTypeConversionReturnsInputWhenUnmapped() {
    // Unmapped native types pass through unchanged (map may contain prior entries from other tests)
    short nativeType = (short) 32000;
    assertEquals(nativeType, PSDatabaseMetaData.guessNativeDataTypeConversion(nativeType));
  }

  @Test
  void loadNativeDataTypeMapBuildsTypedMapFromDriverTypeInfo() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    ResultSet rs = mock(ResultSet.class);

    when(meta.getURL()).thenReturn("jdbc:jtds:sqlserver://localhost/rx");
    when(meta.getDatabaseProductName()).thenReturn("PSDatabaseMetaDataTypeMapTest-DB");
    when(meta.getTypeInfo()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString(1)).thenReturn("varchar", "integer");
    when(rs.getShort(2)).thenReturn((short) Types.VARCHAR, (short) Types.INTEGER);

    HashMap<String, Short> map = PSDatabaseMetaData.loadNativeDataTypeMap(conn, meta);
    assertNotNull(map);
    assertTrue(map.containsKey("varchar"));
    assertTrue(map.containsKey("integer"));
    assertEquals(Types.VARCHAR, map.get("varchar").intValue());
    assertEquals(Types.INTEGER, map.get("integer").intValue());

    // Second call returns the cached map for the same product name
    HashMap<String, Short> again = PSDatabaseMetaData.loadNativeDataTypeMap(conn, meta);
    assertSame(map, again);
  }

  @Test
  void loadNativeDataTypeMapRejectsNullArgs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDatabaseMetaData.loadNativeDataTypeMap(null, mock(DatabaseMetaData.class)));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDatabaseMetaData.loadNativeDataTypeMap(mock(Connection.class), null));
  }

  @Test
  void getTablesFiltersByCatalogAndTypeAgainstTypedList() throws Exception {
    PSDatabaseMetaData meta = new PSDatabaseMetaData("testDs");
    // m_tables stays empty after ctor — getTables returns empty array, not null
    String[] tables = meta.getTables("catalog", null, "%", new String[] {"TABLE"});
    assertNotNull(tables);
    assertEquals(0, tables.length);
  }
}
