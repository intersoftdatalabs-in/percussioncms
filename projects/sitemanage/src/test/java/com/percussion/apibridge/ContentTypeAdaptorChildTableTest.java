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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSTableLocator;
import com.percussion.design.objectstore.PSTableRef;
import com.percussion.design.objectstore.PSTableSet;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.util.PSCollection;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * REST child field-set backend table provisioning (#6509 / parent #1690).
 *
 * <p>A {@code POST .../fields} with a new {@code fieldSet} must target its own child table
 * ({@code <parent>_<child>}), wire the CE pipe {@code TableRef}, and create the physical table
 * ({@code SYSID}/{@code SORTRANK} + parent link) before design save re-inits the editor
 * application.
 */
@Tag("UnitTest")
class ContentTypeAdaptorChildTableTest {

  @Test
  void childTableNameFor_matchesPackagedConvention() {
    assertEquals(
        "CT_TESTWEBSERVICES_CHILD_1",
        ContentTypeAdaptor.childTableNameFor("CT_TESTWEBSERVICES", "child_1"));
    assertEquals("PERCPAGE_KID", ContentTypeAdaptor.childTableNameFor("PERCPAGE", "kid"));
  }

  @Test
  void childTableNameFor_rejectsBlank() {
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.childTableNameFor(" ", "kid"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ContentTypeAdaptor.childTableNameFor("PERCPAGE", " "));
  }

  @Test
  void newChildField_targetsOwnTable() {
    PSItemDefinition def = stubDefinition();
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_kid_note");
    body.setFieldSet("kid");

    PSField field = ContentTypeAdaptor.addPersistableLocalField(def, body);

    assertTrue(
        field.getLocator() instanceof PSBackEndColumn,
        "child field keeps a backend column locator");
    PSBackEndTable table = ((PSBackEndColumn) field.getLocator()).getTable();
    assertEquals("PERCPAGE_KID", table.getAlias());
    assertEquals("PERCPAGE_KID", table.getTable());
  }

  @Test
  void secondChildField_reusesChildTable() {
    PSItemDefinition def = stubDefinition();
    ContentTypeField first = new ContentTypeField();
    first.setName("rx_kid_a");
    first.setFieldSet("kid");
    ContentTypeField second = new ContentTypeField();
    second.setName("rx_kid_b");
    second.setFieldSet("kid");

    PSField fa = ContentTypeAdaptor.addPersistableLocalField(def, first);
    PSField fb = ContentTypeAdaptor.addPersistableLocalField(def, second);

    assertEquals(tableAliasOf(fa), tableAliasOf(fb));
    assertEquals("PERCPAGE_KID", tableAliasOf(fb));
  }

  @Test
  void existingChildTable_preserved() {
    PSItemDefinition def = stubDefinition();
    PSFieldSet parent = def.getFieldSet();
    PSFieldSet child = new PSFieldSet("acl");
    child.setType(PSFieldSet.TYPE_COMPLEX_CHILD);
    child.setSourceType(PSField.TYPE_LOCAL);
    PSBackEndTable acl = new PSBackEndTable("PSX_OBJECTACL");
    acl.setTable("PSX_OBJECTACL");
    child.add(
        new PSField(PSField.TYPE_LOCAL, "permissions", new PSBackEndColumn(acl, "PERMISSIONS")));
    parent.add(child);

    PSBackEndTable table = ContentTypeAdaptor.tableForTargetFieldSet(def, parent, child);

    assertEquals("PSX_OBJECTACL", table.getAlias());
  }

  @Test
  void ensureChildTableRef_addsRefAndIsIdempotent() {
    PSItemDefinition def = stubDefinitionWithPipe("PERCPAGE");

    ContentTypeAdaptor.ensureChildTableRef(def, "PERCPAGE_KID");
    ContentTypeAdaptor.ensureChildTableRef(def, "PERCPAGE_KID");

    List<String> aliases = tableRefAliases(def);
    assertTrue(aliases.contains("PERCPAGE"));
    assertEquals(1, aliases.stream().filter("PERCPAGE_KID"::equals).count());
  }

  @Test
  void existingChildTableRefName_matchesAliasCaseInsensitive() {
    PSItemDefinition def = stubDefinitionWithPipe("PERCPAGE");
    ContentTypeAdaptor.ensureChildTableRef(def, "PERCPAGE_KID");

    assertEquals(
        "PERCPAGE_KID", ContentTypeAdaptor.existingChildTableRefName(def, "percpage_kid"));
    assertEquals("PERCPAGE", ContentTypeAdaptor.existingChildTableRefName(def, "PERCPAGE"));
    assertEquals("OTHER", ContentTypeAdaptor.existingChildTableRefName(def, "OTHER"));
  }

  @Test
  void pipeTableSets_emptyWithoutEditor() {
    assertTrue(ContentTypeAdaptor.pipeTableSets(stubDefinition()).isEmpty());
    assertTrue(ContentTypeAdaptor.pipeTableSets(null).isEmpty());
  }

  @Test
  void isComplexChildField_routesChildAndParent() {
    PSItemDefinition def = stubDefinition();
    ContentTypeField kidBody = new ContentTypeField();
    kidBody.setName("rx_kid_note");
    kidBody.setFieldSet("kid");
    PSField kid = ContentTypeAdaptor.addPersistableLocalField(def, kidBody);
    ContentTypeField parentBody = new ContentTypeField();
    parentBody.setName("rx_note");
    PSField parentField = ContentTypeAdaptor.addPersistableLocalField(def, parentBody);

    assertTrue(ContentTypeAdaptor.isComplexChildField(def, kid));
    assertFalse(ContentTypeAdaptor.isComplexChildField(def, parentField));
    assertFalse(ContentTypeAdaptor.isComplexChildField(def, null));
    assertFalse(ContentTypeAdaptor.isComplexChildField(null, kid));
  }

  @Test
  void ensureLocalFieldBackendColumn_childFieldUsesEnsureChildTable() {
    ContentTypeLocalFieldColumnSchema schema = mock(ContentTypeLocalFieldColumnSchema.class);
    ContentTypeAdaptor adaptor = adaptorWith(schema);
    PSItemDefinition def = stubDefinition();
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_kid_note");
    body.setFieldSet("kid");
    PSField field = ContentTypeAdaptor.addPersistableLocalField(def, body);

    adaptor.ensureLocalFieldBackendColumn(def, field);

    verify(schema).ensureChildTable(eq("PERCPAGE_KID"), eq("RX_KID_NOTE"), anyString(), anyString());
    verify(schema, never()).ensureColumn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void ensureLocalFieldBackendColumn_parentFieldUsesEnsureColumn() {
    ContentTypeLocalFieldColumnSchema schema = mock(ContentTypeLocalFieldColumnSchema.class);
    ContentTypeAdaptor adaptor = adaptorWith(schema);
    PSItemDefinition def = stubDefinition();
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    PSField field = ContentTypeAdaptor.addPersistableLocalField(def, body);

    adaptor.ensureLocalFieldBackendColumn(def, field);

    verify(schema).ensureColumn(eq("PERCPAGE"), eq("RX_NOTE"), anyString(), anyString());
    verify(schema, never())
        .ensureChildTable(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void createChildTableSql_dialects() {
    String h2 =
        JdbcContentTypeLocalFieldColumnSchema.createChildTableSql(
            "H2", "KID", "RXF", "VARCHAR(50)", false);
    assertTrue(h2.contains("SYSID INTEGER NOT NULL"), h2);
    assertTrue(h2.contains("SORTRANK INTEGER"), h2);
    assertTrue(h2.contains("CONTENTID INTEGER NOT NULL"), h2);
    assertTrue(h2.contains("REVISIONID INTEGER NOT NULL"), h2);

    String oracle =
        JdbcContentTypeLocalFieldColumnSchema.createChildTableSql(
            "Oracle", "KID", "RXF", "VARCHAR2(50)", false);
    assertTrue(oracle.contains("SYSID NUMBER(10) NOT NULL"), oracle);

    String mssql =
        JdbcContentTypeLocalFieldColumnSchema.createChildTableSql(
            "Microsoft SQL Server", "KID", "RXF", "NVARCHAR(50)", true);
    assertTrue(mssql.contains("SYSID INT NOT NULL"), mssql);
  }

  @Test
  void ensureChildTable_h2CreatesKeysAndColumnIdempotent() throws Exception {
    String url = "jdbc:h2:mem:ctchild" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    Supplier<Connection> connections = () -> open(url);
    JdbcContentTypeLocalFieldColumnSchema schema =
        new JdbcContentTypeLocalFieldColumnSchema(connections);

    schema.ensureChildTable("KIDT", "RXF", "text", "50");
    schema.ensureChildTable("KIDT", "RXF", "text", "50");
    schema.ensureColumn("KIDT", "RXG", "text", "50");

    try (Connection check = connections.get()) {
      for (String col : List.of("CONTENTID", "REVISIONID", "SYSID", "SORTRANK", "RXF", "RXG")) {
        assertTrue(
            JdbcContentTypeLocalFieldColumnSchema.columnExists(check, "KIDT", col),
            "missing column " + col);
      }
    }
  }

  @Test
  void childTableSchemaDefault_delegatesToEnsureColumn() {
    ContentTypeLocalFieldColumnSchema noop = ContentTypeLocalFieldColumnSchema.noop();
    // No repository: default must degrade to the column path without throwing.
    noop.ensureChildTable("KIDT", "RXF", "text", "50");
  }

  private static String tableAliasOf(PSField field) {
    assertTrue(field.getLocator() instanceof PSBackEndColumn);
    return ((PSBackEndColumn) field.getLocator()).getTable().getAlias();
  }

  private static ContentTypeAdaptor adaptorWith(ContentTypeLocalFieldColumnSchema schema) {
    return new ContentTypeAdaptor(
        mock(IPSContentDesignWs.class),
        mock(PSItemDefManager.class),
        mock(IPSSystemDesignWs.class),
        () -> true,
        schema);
  }

  private static PSItemDefinition stubDefinition() {
    PSFieldSet parent = new PSFieldSet("percPage");
    PSDisplayMapper mapper = new PSDisplayMapper("percPage");
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getFieldSet()).thenReturn(parent);
    when(def.getComplexChildren()).thenReturn(List.of());
    when(def.getDisplayMapper("percPage")).thenReturn(mapper);
    when(def.getTypeTables()).thenReturn(List.of(new PSBackEndTable("PERCPAGE")));
    when(def.getContentEditor()).thenReturn(null);
    return def;
  }

  private static PSItemDefinition stubDefinitionWithPipe(String parentTable) {
    PSItemDefinition def = stubDefinition();
    PSCollection refs = new PSCollection(PSTableRef.class);
    refs.add(new PSTableRef(parentTable, parentTable));
    PSTableSet set = new PSTableSet(new PSTableLocator("contentCredential"), refs);
    PSCollection sets = new PSCollection(PSTableSet.class);
    sets.add(set);
    PSContainerLocator locator = new PSContainerLocator(sets);
    PSContentEditorPipe pipe = mock(PSContentEditorPipe.class);
    when(pipe.getLocator()).thenReturn(locator);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(editor.getPipe()).thenReturn(pipe);
    when(def.getContentEditor()).thenReturn(editor);
    return def;
  }

  private static List<String> tableRefAliases(PSItemDefinition def) {
    List<String> aliases = new ArrayList<>();
    for (PSTableSet set : ContentTypeAdaptor.pipeTableSets(def)) {
      java.util.Iterator<?> refs = set.getTableRefs();
      while (refs.hasNext()) {
        Object next = refs.next();
        if (next instanceof PSTableRef ref) {
          aliases.add(ref.getAlias());
        }
      }
    }
    return aliases;
  }

  private static Connection open(String url) {
    try {
      return DriverManager.getConnection(url, "sa", "");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to open H2 connection", e);
    }
  }
}
