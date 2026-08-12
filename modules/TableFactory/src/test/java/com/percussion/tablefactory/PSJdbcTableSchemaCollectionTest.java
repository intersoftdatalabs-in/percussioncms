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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Unit test for {@link PSJdbcTableSchemaCollection}. Iteration uses the inherited {@code
 * List<Object>} iterator from {@code PSCollection}; members are {@link PSJdbcTableSchema}.
 */
public class PSJdbcTableSchemaCollectionTest {

  @Test
  public void iteratorYieldsAddedSchemas() throws Exception {
    PSJdbcTableSchemaCollection collection = new PSJdbcTableSchemaCollection();
    PSJdbcTableSchema parent = newSchema("PARENT_TABLE");
    PSJdbcTableSchema child = newSchema("CHILD_TABLE");
    collection.add(parent);
    collection.add(child);

    assertEquals(2, collection.size());

    Iterator<?> it = collection.iterator();
    assertTrue(it.hasNext());
    assertSame(parent, it.next());
    assertTrue(it.hasNext());
    assertSame(child, it.next());

    List<PSJdbcTableSchema> fromForEach = new ArrayList<>();
    for (Object o : collection) {
      assertTrue(o instanceof PSJdbcTableSchema);
      fromForEach.add((PSJdbcTableSchema) o);
    }
    assertEquals(List.of(parent, child), fromForEach);
  }

  @Test
  public void getTableSchemaIsCaseInsensitive() throws Exception {
    PSJdbcTableSchemaCollection collection = new PSJdbcTableSchemaCollection();
    PSJdbcTableSchema schema = newSchema("MyTable");
    collection.add(schema);

    assertSame(schema, collection.getTableSchema("MyTable"));
    assertSame(schema, collection.getTableSchema("mytable"));
    assertNull(collection.getTableSchema("other"));
  }

  @Test
  public void getTableSchemaRejectsBlankName() {
    PSJdbcTableSchemaCollection collection = new PSJdbcTableSchemaCollection();
    assertThrows(IllegalArgumentException.class, () -> collection.getTableSchema(null));
    assertThrows(IllegalArgumentException.class, () -> collection.getTableSchema(""));
    assertThrows(IllegalArgumentException.class, () -> collection.getTableSchema("   "));
  }

  @Test
  public void xmlRoundTripPreservesTableNames() throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("MSSQL", "sqlserver", null);
    PSJdbcTableSchemaCollection collection = new PSJdbcTableSchemaCollection();
    collection.add(newSchema("TABLE_A"));
    collection.add(newSchema("TABLE_B"));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    doc.appendChild(collection.toXml(doc));

    PSJdbcTableSchemaCollection restored = new PSJdbcTableSchemaCollection(doc, map);
    assertEquals(2, restored.size());
    assertNotNull(restored.getTableSchema("TABLE_A"));
    assertNotNull(restored.getTableSchema("TABLE_B"));
    assertEquals("TABLE_A", restored.getTableSchema("table_a").getName());
  }

  private static PSJdbcTableSchema newSchema(String name) throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("MSSQL", "sqlserver", null);
    List<PSJdbcColumnDef> cols = new ArrayList<>();
    cols.add(
        new PSJdbcColumnDef(
            map, "col1", PSJdbcTableComponent.ACTION_CREATE, Types.VARCHAR, "50", false, null));
    return new PSJdbcTableSchema(name, cols.iterator());
  }
}
