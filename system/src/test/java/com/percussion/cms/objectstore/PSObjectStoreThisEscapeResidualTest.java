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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for residual cms.objectstore Element/ctor this-escape real fixes (#2613 /
 * parent #2022), after createKey/Element batch #2467. Complements {@link
 * PSDbComponentThisEscapeTest}.
 */
public class PSObjectStoreThisEscapeResidualTest {

  @Test
  public void actionNamedCtorAndElementRoundTrip() throws Exception {
    PSAction original =
        new PSAction(
            "preview_item",
            "Preview",
            PSAction.TYPE_MENUITEM,
            "../sys_cxSupport/preview.html",
            PSAction.HANDLER_CLIENT,
            3);
    assertEquals("preview_item", original.getName());
    assertEquals("Preview", original.getLabel());
    assertEquals(PSAction.TYPE_MENUITEM, original.getMenuType());
    assertEquals(3, original.getSortRank());
    assertTrue(original.isClientAction());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);

    PSAction restored = new PSAction(xml);
    assertEquals("preview_item", restored.getName());
    assertEquals("Preview", restored.getLabel());
    assertEquals(PSAction.TYPE_MENUITEM, restored.getMenuType());
    assertEquals(3, restored.getSortRank());
    assertTrue(restored.isClientAction());

    // Public fromXml after construction still works
    PSAction second =
        new PSAction(
            "other",
            "Other",
            PSAction.TYPE_MENU,
            "",
            PSAction.HANDLER_SERVER,
            1);
    Element secondXml = second.toXml(doc);
    restored.fromXml(secondXml);
    assertEquals("other", restored.getName());
    assertEquals("Other", restored.getLabel());
    assertEquals(PSAction.TYPE_MENU, restored.getMenuType());
    assertFalse(restored.isClientAction());
  }

  @Test
  public void actionParameterAndPropertyElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();

    PSActionParameter param = new PSActionParameter("sys_contentid", "301", "id param");
    Element paramXml = param.toXml(doc);
    PSActionParameter paramRestored = new PSActionParameter(paramXml);
    assertEquals("sys_contentid", paramRestored.getName());
    assertEquals("301", paramRestored.getValue());
    assertEquals("id param", paramRestored.getDescription());

    PSActionProperty prop = new PSActionProperty(PSAction.PROP_MUTLI_SELECT, PSAction.NO, "multi");
    Element propXml = prop.toXml(doc);
    PSActionProperty propRestored = new PSActionProperty(propXml);
    assertEquals(PSAction.PROP_MUTLI_SELECT, propRestored.getName());
    assertEquals(PSAction.NO, propRestored.getValue());
    assertEquals("multi", propRestored.getDescription());
  }

  @Test
  public void actionVisibilityContextCtorAcceptsValues() {
    PSActionVisibilityContext ctx =
        new PSActionVisibilityContext(
            PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY,
            new String[] {"10", "20"},
            "communities");
    assertEquals(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, ctx.getName());
    assertTrue(ctx.contains("10"));
    assertTrue(ctx.contains("20"));
    assertEquals("communities", ctx.getDescription());
  }

  @Test
  public void folderElementRoundTrip() throws Exception {
    // (name, id, communityId, permissions, description)
    PSFolder original = new PSFolder("SiteRoot", 1001, 7, 0, "Root folder");
    assertEquals("SiteRoot", original.getName());
    assertEquals(1001, original.getLocator().getId());
    assertEquals(7, original.getCommunityId());
    assertEquals("Root folder", original.getDescription());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);

    PSFolder restored = new PSFolder(xml);
    assertEquals("SiteRoot", restored.getName());
    assertEquals(7, restored.getCommunityId());
    assertEquals("Root folder", restored.getDescription());

    restored.fromXml(xml);
    assertEquals("SiteRoot", restored.getName());
    assertEquals(7, restored.getCommunityId());
  }

  @Test
  public void folderPropertyNamedAndElementRoundTrip() throws Exception {
    PSFolderProperty prop = new PSFolderProperty("sys_pubFileName", "site", "pub name");
    assertEquals("sys_pubFileName", prop.getName());
    assertEquals("site", prop.getValue());
    assertEquals("pub name", prop.getDescription());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = prop.toXml(doc);
    PSFolderProperty restored = new PSFolderProperty(xml);
    assertEquals("sys_pubFileName", restored.getName());
    assertEquals("site", restored.getValue());
    assertEquals("pub name", restored.getDescription());
  }

  @Test
  public void searchDefaultAndNamedCtors() throws Exception {
    PSSearch def = new PSSearch();
    assertNotNull(def.getInternalName());
    assertTrue(def.getInternalName().startsWith("search"));
    assertEquals(PSSearch.DEFAULT_MAX, def.getMaximumResultSize());

    PSSearch named = new PSSearch("MySearch", false);
    assertEquals("MySearch", named.getInternalName());
    assertEquals("MySearch", named.getDisplayName());
    assertFalse(named.isCustomApp());
  }

  @Test
  public void searchFieldNamedAndElementRoundTrip() throws Exception {
    PSSearchField field =
        new PSSearchField("sys_title", "Title", "T", PSSearchField.TYPE_TEXT, "title field");
    assertEquals("sys_title", field.getFieldName());
    assertEquals("Title", field.getDisplayName());
    assertEquals(PSSearchField.TYPE_TEXT, field.getFieldType());
    assertEquals("title field", field.getFieldDescription());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = field.toXml(doc);
    PSSearchField restored = new PSSearchField(xml);
    assertEquals("sys_title", restored.getFieldName());
    assertEquals("Title", restored.getDisplayName());
    assertEquals(PSSearchField.TYPE_TEXT, restored.getFieldType());
  }

  @Test
  public void displayFormatDefaultCtorSetsNames() throws Exception {
    PSDisplayFormat df = new PSDisplayFormat();
    assertNotNull(df.getDisplayName());
    assertTrue(df.getDisplayName().startsWith("Display Format "));
    assertNotNull(df.getInternalName());
    assertTrue(df.getInternalName().startsWith("display_format_"));
  }
}
