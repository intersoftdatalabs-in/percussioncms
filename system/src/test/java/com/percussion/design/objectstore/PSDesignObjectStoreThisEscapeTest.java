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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for ctor/fromXml this-escape real-fix batch (#2404): private helpers /
 * final setters preserve Element restore and value construction.
 */
public class PSDesignObjectStoreThisEscapeTest {

  @Test
  public void choicesGlobalCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSChoices original = new PSChoices(42);
    Element elem = original.toXml(doc);

    PSChoices restored = new PSChoices(elem, null, null);
    assertEquals(original, restored);
    assertEquals(42, restored.getGlobal());
    assertEquals(PSChoices.TYPE_GLOBAL, restored.getType());
  }

  @Test
  public void choicesLocalCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSEntry entry = new PSEntry("v1", new PSDisplayText("label1"));
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(entry);
    PSChoices original = new PSChoices(local);
    Element elem = original.toXml(doc);

    PSChoices restored = new PSChoices(elem, null, null);
    assertEquals(original, restored);
    assertEquals(PSChoices.TYPE_LOCAL, restored.getType());
  }

  @Test
  public void fieldValueCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSField original = new PSField("sys_title", null);
    original.setType(PSField.TYPE_SYSTEM);
    Element elem = original.toXml(doc);

    PSField restored = new PSField(elem, null, null);
    assertEquals(original.getSubmitName(), restored.getSubmitName());
    assertEquals(original.getType(), restored.getType());
  }

  @Test
  public void entryValueCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSEntry original = new PSEntry("val", new PSDisplayText("lbl"));
    Element elem = original.toXml(doc);

    PSEntry restored = new PSEntry(elem, null, null);
    assertEquals(original.getValue(), restored.getValue());
    assertEquals(original.getLabel().getText(), restored.getLabel().getText());
  }

  @Test
  public void urlRequestCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("p1", new PSTextLiteral("one")));
    PSUrlRequest original = new PSUrlRequest("req", "http://example.intsof/test", params);
    Element elem = original.toXml(doc);

    PSUrlRequest restored = new PSUrlRequest(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getHref(), restored.getHref());
  }

  @Test
  public void aclEntryNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAclEntry original = new PSAclEntry("admin1", PSAclEntry.ACE_TYPE_USER);
    Element elem = original.toXml(doc);

    PSAclEntry restored = new PSAclEntry(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertTrue(restored.isUser());
  }

  @Test
  public void aclElementRoundTripPreservesEntries() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAcl original = new PSAcl();
    PSCollection entries = new PSCollection(PSAclEntry.class);
    PSAclEntry entry = new PSAclEntry("Editor", PSAclEntry.ACE_TYPE_ROLE);
    entries.add(entry);
    original.setEntries(entries);
    original.setAccessForMultiMembershipMaximum();
    Element elem = original.toXml(doc);

    PSAcl restored = new PSAcl(elem, null, null);
    assertNotNull(restored.getEntries());
    assertEquals(1, restored.getEntries().size());
    assertEquals("Editor", ((PSAclEntry) restored.getEntries().get(0)).getName());
    assertTrue(restored.isAccessForMultiMembershipMaximum());
  }

  @Test
  public void fieldSetNameCtor() {
    PSFieldSet set = new PSFieldSet("body");
    assertEquals("body", set.getName());
    assertEquals(PSFieldSet.TYPE_PARENT, set.getType());
  }

  @Test
  public void propertyNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSProperty original =
        new PSProperty("dimage", PSProperty.TYPE_STRING, "gif", false, "image type");
    Element elem = original.toXml(doc);

    PSProperty restored = new PSProperty(elem);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
  }
}
