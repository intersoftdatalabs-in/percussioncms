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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Behavioral tests for typed {@link PSDisplayFormatOption} map after rawtypes cleanup (#2939). */
public class PSDisplayFormatOptionTest {

  @Test
  public void emptyOptionHasNoDisplayFormats() {
    PSDisplayFormatOption option = new PSDisplayFormatOption();
    assertFalse(option.haveDisplayFormats());
  }

  @Test
  public void addGetRemoveRoundTrip() {
    PSDisplayFormatOption option = new PSDisplayFormatOption();
    option.addItemDisplayFormat("/Sites/a", "df-1");
    assertTrue(option.haveDisplayFormats());
    assertEquals("df-1", option.getItemDisplayFormat("/Sites/a"));
    assertNull(option.getItemDisplayFormat("/Sites/missing"));

    option.removeItemDisplayFormat("/Sites/a");
    assertFalse(option.haveDisplayFormats());
    assertNull(option.getItemDisplayFormat("/Sites/a"));
  }

  @Test
  public void addRejectsNullOrEmptyPath() {
    PSDisplayFormatOption option = new PSDisplayFormatOption();
    assertThrows(IllegalArgumentException.class, () -> option.addItemDisplayFormat(null, "df"));
    assertThrows(IllegalArgumentException.class, () -> option.addItemDisplayFormat("  ", "df"));
    assertThrows(IllegalArgumentException.class, () -> option.getItemDisplayFormat(null));
    assertThrows(IllegalArgumentException.class, () -> option.removeItemDisplayFormat(""));
  }

  @Test
  public void toXmlEmitsItemsAndFromXmlRestores() throws Exception {
    PSDisplayFormatOption option = new PSDisplayFormatOption();
    option.addItemDisplayFormat("/Sites/a", "df-1");
    option.addItemDisplayFormat("/Sites/b", "df-2");

    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = option.toXml(doc);
    assertEquals("PSXDisplayFormatOption", root.getNodeName());
    NodeList items = root.getElementsByTagName("Item");
    assertEquals(2, items.getLength());

    PSDisplayFormatOption restored = new PSDisplayFormatOption();
    restored.fromXml(root);
    assertTrue(restored.haveDisplayFormats());
    assertEquals("df-1", restored.getItemDisplayFormat("/Sites/a"));
    assertEquals("df-2", restored.getItemDisplayFormat("/Sites/b"));
    assertEquals(option, restored);
    assertEquals(option.hashCode(), restored.hashCode());
  }

  @Test
  public void fromXmlRejectsNull() {
    PSDisplayFormatOption option = new PSDisplayFormatOption();
    assertThrows(IllegalArgumentException.class, () -> option.fromXml(null));
  }
}
