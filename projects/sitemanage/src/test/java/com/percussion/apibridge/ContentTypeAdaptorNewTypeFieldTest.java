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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSContentType;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSTableRef;
import com.percussion.design.objectstore.PSTableSet;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/** New content types must not save the empty sys_Default main_editor (#4905). */
class ContentTypeAdaptorNewTypeFieldTest {

  @Test
  void defaultTemplateGainsTitleFieldAndContentTable() throws Exception {
    PSItemDefinition def = loadDefault("percQaSrc");
    ContentTypeAdaptor.ensureNewContentTypeHasLocalField(def);

    PSField title = ContentTypeAdaptor.findField(def, "title");
    assertNotNull(title);
    assertEquals(PSField.TYPE_LOCAL, title.getType());

    boolean renamed = false;
    PSContentEditorPipe pipe = (PSContentEditorPipe) def.getContentEditor().getPipe();
    Iterator<?> sets = pipe.getLocator().getTableSets();
    while (sets.hasNext()) {
      PSTableSet set = (PSTableSet) sets.next();
      Iterator<?> refs = set.getTableRefs();
      while (refs.hasNext()) {
        PSTableRef ref = (PSTableRef) refs.next();
        if ("PERCQASRC".equals(ref.getName())) {
          renamed = true;
          assertEquals("PERCQASRC", ref.getAlias());
        }
        assertTrue(!"psx_dummy".equalsIgnoreCase(ref.getName()));
      }
    }
    assertTrue(renamed);
  }

  @Test
  void createTableSqlIncludesContentId() {
    String h2 =
        JdbcContentTypeLocalFieldColumnSchema.createContentTableSql(
            "H2", "PERCQASRC", "TITLE", "VARCHAR(50)", false);
    assertTrue(h2.contains("CONTENTID INTEGER NOT NULL"));
    assertTrue(h2.contains("REVISIONID INTEGER NOT NULL"));
    assertTrue(h2.contains("TITLE VARCHAR(50)"));
    String oracle =
        JdbcContentTypeLocalFieldColumnSchema.createContentTableSql(
            "Oracle", "PERCQASRC", "TITLE", "VARCHAR2(50)", false);
    assertTrue(oracle.contains("NUMBER(10)"));
  }

  private static PSItemDefinition loadDefault(String name) throws Exception {
    try (InputStream in =
        com.percussion.design.objectstore.PSContentTypeHelper.class.getResourceAsStream(
            "sys_Default.xml")) {
      Element root = PSXmlDocumentBuilder.createXmlDocument(in, false).getDocumentElement();
      PSContentEditor editor = new PSContentEditor(root, null, null);
      editor.setName(name);
      PSContentType type =
          new PSContentType(42, name, name, "", "../psx_ce" + name + "/" + name + ".html", false, 1);
      return new PSItemDefinition("psx_ce" + name, type, editor);
    }
  }
}
