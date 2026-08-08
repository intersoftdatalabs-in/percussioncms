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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed property/key-field iterators on {@link PSItemRelatedItem} (issue
 * #2401 cms.objectstore rawtypes batch 2e).
 */
public class PSItemRelatedItemGenericsTest {

  @Test
  public void testTypedPropertyAndKeyFieldIterators() {
    PSItemRelatedItem related = new PSItemRelatedItem();
    related.setRelationshipId(42);
    related.addProperty("sys_slotid", "301");
    related.addProperty("sys_variantid", "501");

    org.w3c.dom.Document doc = com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    org.w3c.dom.Element keyEl = doc.createElement("KeyField");
    keyEl.setAttribute("name", "sys_title");
    keyEl.appendChild(doc.createTextNode("Hello"));
    related.addKeyField("sys_title", keyEl);

    Iterator<String> props = related.getAllProperties();
    Set<String> propNames = new HashSet<>();
    while (props.hasNext()) {
      String name = props.next();
      assertNotNull(name);
      propNames.add(name);
      assertNotNull(related.getProperty(name));
    }
    assertTrue(propNames.contains("sys_slotid"));
    assertTrue(propNames.contains("sys_variantid"));
    assertEquals("301", related.getProperty("sys_slotid"));

    Iterator<String> keys = related.getAllKeyFields();
    Set<String> keyNames = new HashSet<>();
    while (keys.hasNext()) {
      String name = keys.next();
      assertNotNull(name);
      keyNames.add(name);
    }
    assertTrue(keyNames.contains("sys_title"));
    assertNotNull(related.getKeyField("sys_title"));
  }
}
