/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import org.junit.jupiter.api.Test;

/** Unit test for PSJdbcDataTypeMap. */
public class PSJdbcDataTypeMapTest {

  /** Test the map */
  @Test
  public void testMap() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("MSSQL", "inetdae7", null);
    String nativeStr = "INT";
    String jdbcStr = map.getJdbcString(nativeStr);
    int jdbcInt = map.convertJdbcString(jdbcStr);

    assertEquals(nativeStr, map.getNativeString(jdbcStr));
    assertEquals(nativeStr, map.getNativeString(jdbcInt));
    assertEquals(jdbcStr, map.convertJdbcType(jdbcInt));

    PSJdbcDataTypeMapping dataType;
    dataType = map.getMapping("BIT");
    assertEquals("BIT", dataType.getJdbc());
    dataType = map.getMapping(Types.INTEGER);
    assertEquals("INTEGER", dataType.getJdbc());

    // make sure we get null answers for non-mapped types
    dataType = map.getMapping("ARRAY");
    assertNull(dataType);
    dataType = map.getMapping(Types.ARRAY);
    assertNull(dataType);
    nativeStr = map.getNativeString("ARRAY");
    assertNull(nativeStr);
    nativeStr = map.getNativeString(Types.ARRAY);
    assertNull(nativeStr);

    // make sure we get null answers for bogus jdbc types
    dataType = map.getMapping("FOO");
    assertNull(dataType);
    jdbcStr = map.getJdbcString("F00");
    assertNull(jdbcStr);
  }

  /**
   * Grab specific data types from the DB2 map and see if they have the expected values for each of
   * the attributes. <b>This method is assuming values in PSJdbcDataTypeMaps.xml; if that file is
   * updated, this test may need to be updated.</b>
   */
  @Test
  public void testDB2Mappings() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("DB2", "db2", null);
    PSJdbcDataTypeMapping dataType;

    dataType = map.getMapping(Types.BIT);
    assertEquals("BIT", dataType.getJdbc());
    assertEquals("CHARACTER", dataType.getNative());
    assertEquals("1", dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BIGINT);
    assertEquals("BIGINT", dataType.getJdbc());
    assertEquals("BIGINT", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.NUMERIC);
    assertEquals("NUMERIC", dataType.getJdbc());
    assertEquals("DECIMAL", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNotNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BINARY);
    assertEquals("BINARY", dataType.getJdbc());
    assertEquals("CHARACTER", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNotNull(dataType.getSuffix());

    dataType = map.getMapping(Types.VARCHAR);
    assertEquals("VARCHAR", dataType.getJdbc());
    assertEquals("VARCHAR", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.TIMESTAMP);
    assertEquals("TIMESTAMP", dataType.getJdbc());
    assertEquals("TIMESTAMP", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BLOB);
    assertEquals("BLOB", dataType.getJdbc());
    assertEquals("BLOB", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNotNull(dataType.getSuffix());

    // test max index col size
    assertEquals(-1, map.getMaxIndexColSize());
  }

  /**
   * Grab specific data types from the Oracle map and see if they have the expected values for each
   * of the attributes. <b>This method is assuming values in PSJdbcDataTypeMaps.xml; if that file is
   * updated, this test may need to be updated.</b>
   */
  @Test
  public void testOracleMappings() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("ORACLE", "oracle:thin", null);
    PSJdbcDataTypeMapping dataType;

    dataType = map.getMapping(Types.BIT);
    assertEquals("BIT", dataType.getJdbc());
    assertEquals("CHAR", dataType.getNative());
    assertEquals("1", dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BIGINT);
    assertEquals("BIGINT", dataType.getJdbc());
    assertEquals("NUMBER", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertEquals("0", dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.NUMERIC);
    assertEquals("NUMERIC", dataType.getJdbc());
    assertEquals("NUMBER", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNotNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BINARY);
    assertEquals("BINARY", dataType.getJdbc());
    assertEquals("RAW", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.VARCHAR);
    assertEquals("VARCHAR", dataType.getJdbc());
    assertEquals("VARCHAR2", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.TIMESTAMP);
    assertEquals("TIMESTAMP", dataType.getJdbc());
    assertEquals("DATE", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BLOB);
    assertEquals("BLOB", dataType.getJdbc());
    assertEquals("BLOB", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    // test max index col size
    assertEquals(-1, map.getMaxIndexColSize());
  }

  /**
   * Grab specific data types from the MSSQL map and see if they have the expected values for each
   * of the attributes. <b>This method is assuming values in PSJdbcDataTypeMaps.xml; if that file is
   * updated, this test may need to be updated.</b>
   */
  @Test
  public void testMSSQLMappings() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("MSSQL", "inetdae7", null);
    PSJdbcDataTypeMapping dataType;

    dataType = map.getMapping(Types.BIT);
    assertEquals("BIT", dataType.getJdbc());
    assertEquals("BIT", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BIGINT);
    assertEquals("BIGINT", dataType.getJdbc());
    assertEquals("BIGINT", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.NUMERIC);
    assertEquals("NUMERIC", dataType.getJdbc());
    assertEquals("NUMERIC", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNotNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BINARY);
    assertEquals("BINARY", dataType.getJdbc());
    assertEquals("BINARY", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.VARCHAR);
    assertEquals("VARCHAR", dataType.getJdbc());
    assertEquals("NVARCHAR", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.TIMESTAMP);
    assertEquals("TIMESTAMP", dataType.getJdbc());
    assertEquals("DATETIME", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    dataType = map.getMapping(Types.BLOB);
    assertEquals("BLOB", dataType.getJdbc());
    assertEquals("IMAGE", dataType.getNative());
    assertNull(dataType.getDefaultSize());
    assertNull(dataType.getDefaultScale());
    assertNull(dataType.getSuffix());

    // test max index col size
    assertEquals(-1, map.getMaxIndexColSize());
  }

  /**
   * Tests for different databases if the createForeignKeyIndexes flag is turned on, if specified in
   * PSJdbcDataTypeMap.xml
   *
   * @throws Exception
   */
  @Test
  public void testSetIndexesForForeignKey() throws Exception {
    var map = new PSJdbcDataTypeMap("MYSQL", "mysql", null);
    assertTrue(map.isCreateForeignKeyIndexes());

    var map_mssql = new PSJdbcDataTypeMap("MSSQL", "inetdae7", null);
    assertTrue(map_mssql.isCreateForeignKeyIndexes());

    var map_oracle = new PSJdbcDataTypeMap("ORACLE", "oracle:thin", null);
    assertTrue(map_oracle.isCreateForeignKeyIndexes());

    var map_db2 = new PSJdbcDataTypeMap("DB2", "db2", null);
    assertTrue(map_db2.isCreateForeignKeyIndexes());
  }

  /**
   * H2 map for #548 default embedded replacement. Assumes values in PSJdbcDataTypeMaps.xml.
   */
  @Test
  public void testH2Mappings() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("H2", "h2", null);
    PSJdbcDataTypeMapping dataType;

    dataType = map.getMapping(Types.BIT);
    assertEquals("BIT", dataType.getJdbc());
    assertEquals("BOOLEAN", dataType.getNative());

    dataType = map.getMapping(Types.INTEGER);
    assertEquals("INTEGER", dataType.getJdbc());
    assertEquals("INTEGER", dataType.getNative());

    dataType = map.getMapping(Types.CLOB);
    assertEquals("CLOB", dataType.getJdbc());
    assertEquals("CLOB", dataType.getNative());

    dataType = map.getMapping(Types.BLOB);
    assertEquals("BLOB", dataType.getJdbc());
    assertEquals("BLOB", dataType.getNative());

    dataType = map.getMapping(Types.DATE);
    assertEquals("DATE", dataType.getJdbc());
    assertEquals("TIMESTAMP", dataType.getNative());

    dataType = map.getMapping(Types.VARCHAR);
    assertEquals("VARCHAR", dataType.getJdbc());
    assertEquals("VARCHAR", dataType.getNative());
    assertNotNull(dataType.getDefaultSize());
  }
}
