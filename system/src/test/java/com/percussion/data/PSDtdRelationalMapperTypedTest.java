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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSDtdTree;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSDtdRelationalMapper} table/column maps and result-set
 * name-to-index initialization after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSDtdRelationalMapperTypedTest {

  @Test
  void simpleDtdMapsRootTableAndNameLookup() throws Exception {
    PSDtdTree tree = parseSimpleDtd();
    PSDtdRelationalMapper mapper = new PSDtdRelationalMapper(tree);

    assertTrue(mapper.getNumTables() >= 1);
    PSDtdRelationalMapper.TableDef table = mapper.getTable(1);
    assertNotNull(table);
    assertEquals(table, mapper.getTable(table.getName()));
    assertEquals(1, mapper.getTableOrdinal(table.getName()));
  }

  @Test
  void initResultSetBuildsOneBasedNameToIndexMap() throws Exception {
    PSDtdTree tree = parseSimpleDtd();
    PSDtdRelationalMapper mapper = new PSDtdRelationalMapper(tree);
    PSDtdRelationalMapper.TableDef table = mapper.getTable(1);

    // Ensure at least two columns so the map is multi-entry.
    if (table.getNumColumns() < 2) {
      table.addColumn(mapper.new ColumnDef("Item/extra1"));
      table.addColumn(mapper.new ColumnDef("Item/extra2"));
    }

    PSResultSet rs = new PSResultSet();
    table.initResultSet(rs);
    Map<String, Integer> names = rs.getColumnNames();

    // setMetaData rebuilds the map from column metadata (1-based).
    assertEquals(table.getNumColumns(), names.size());
    for (int i = 1; i <= table.getNumColumns(); i++) {
      String colName = table.getColumn(i).getName();
      assertEquals(Integer.valueOf(i), names.get(colName), "ordinal for " + colName);
    }
  }

  private static PSDtdTree parseSimpleDtd() throws Exception {
    // Minimal element with character data — yields a root table with a data column.
    String dtd = "<!ELEMENT Item (#PCDATA)>\n";
    return new PSDtdTree(new ByteArrayInputStream(dtd.getBytes(StandardCharsets.UTF_8)), "Item");
  }
}
