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

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for cms.objectstore Element super-ctor / createKey / collection restore after
 * this-escape fixes (#2467 / parent #2022). Complements {@code PSDesignObjectStoreThisEscapeTest}.
 *
 * <p>Covers: createKeyDefault Element path, double-load elimination (ContentType/SlotType),
 * collection Element restore, public fromXml still honoring subclass createKey after construction.
 */
public class PSDbComponentThisEscapeTest {

  @Test
  public void variantSlotTypeElementCtorRoundTripUsesCreateKeyDefault() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source = sampleVariantSlotTypeElement(doc, 42, 7);

    // Element ctor: super(source) → createKeyDefault only (no double fromXml).
    PSVariantSlotType restored = new PSVariantSlotType(source);

    assertEquals(42, restored.getVariantId());
    assertEquals(7, restored.getSlotId());
    assertNotNull(restored.getLocator());
    assertEquals(IPSDbComponent.DBSTATE_UNMODIFIED, restored.getState());
  }

  @Test
  public void variantSlotTypePublicFromXmlAfterConstructionStillWorks() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element first = sampleVariantSlotTypeElement(doc, 1, 2);
    PSVariantSlotType target = new PSVariantSlotType(first);

    Element second = sampleVariantSlotTypeElement(doc, 99, 88);
    target.fromXml(second);

    assertEquals(99, target.getVariantId());
    assertEquals(88, target.getSlotId());
  }

  @Test
  public void contentTypeElementCtorLoadsFieldsWithoutDoubleKeyApply() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source =
        sampleContentTypeElement(doc, 15, "percPage", "Page", "../rx_cePage/page.html", false, 1);

    PSContentType restored = new PSContentType(source);

    assertEquals(15, restored.getTypeId());
    assertEquals("percPage", restored.getName());
    assertEquals("Page", restored.getLabel());
    assertEquals("../rx_cePage/page.html", restored.getEditorUrl());
    assertFalse(restored.isHiddenFromMenu());
    assertEquals(1, restored.getObjectType());
    assertEquals(IPSDbComponent.DBSTATE_UNMODIFIED, restored.getState());
  }

  @Test
  public void contentTypePublicFromXmlHonorsCreateKeyAfterConstruction() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element first = sampleContentTypeElement(doc, 1, "a", "A", "../rx_ceA/a.html", true, 1);
    PSContentType target = new PSContentType(first);

    Element second = sampleContentTypeElement(doc, 77, "b", "B", "../rx_ceB/b.html", false, 2);
    target.fromXml(second);

    assertEquals(77, target.getTypeId());
    assertEquals("b", target.getName());
    assertEquals("B", target.getLabel());
    assertEquals("../rx_ceB/b.html", target.getEditorUrl());
    assertFalse(target.isHiddenFromMenu());
    assertEquals(2, target.getObjectType());
  }

  @Test
  public void slotTypeElementCtorAndPublicFromXmlRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source = sampleSlotTypeElement(doc, 9, "sys_content");

    PSSlotType restored = new PSSlotType(source);
    assertEquals(9, restored.getSlotId());
    assertEquals("sys_content", restored.getSlotName());

    Element second = sampleSlotTypeElement(doc, 11, "rffList");
    restored.fromXml(second);
    assertEquals(11, restored.getSlotId());
    assertEquals("rffList", restored.getSlotName());
  }

  @Test
  public void contentTypeSetElementArrayCtorRestoresMembers() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element a =
        sampleContentTypeElement(doc, 10, "percPage", "Page", "../rx_cePage/page.html", false, 1);
    Element b =
        sampleContentTypeElement(
            doc, 20, "percFolder", "Folder", "../rx_ceFolder/folder.html", true, 2);

    PSContentTypeSet set = new PSContentTypeSet(new Element[] {a, b});
    assertEquals(2, set.size());
    assertEquals("percPage", set.getContentTypeById(10).getName());
    assertEquals("percFolder", set.getContentTypeByName("PERCFOLDER").getName());
  }

  @Test
  public void contentTypeSetElementRootCtorRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element a = sampleContentTypeElement(doc, 3, "t1", "T1", "../rx_ceT1/t1.html", false, 1);
    Element b = sampleContentTypeElement(doc, 4, "t2", "T2", "../rx_ceT2/t2.html", false, 1);
    PSContentTypeSet original = new PSContentTypeSet(new Element[] {a, b});
    Element xml = original.toXml(doc);

    PSContentTypeSet restored = new PSContentTypeSet(xml);
    assertEquals(2, restored.size());
    assertNotNull(restored.getContentTypeById(3));
    assertNotNull(restored.getContentTypeById(4));
  }

  @Test
  public void itemDefSummaryElementCtorRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement(PSItemDefSummary.getNodeName());
    root.setAttribute("id", "0");
    root.setAttribute("name", "percPage");
    root.setAttribute("label", "Page");
    root.setAttribute("typeId", "301");
    root.setAttribute("editorUrl", "../rx_cePage/page.html");
    root.setAttribute("hideFromMenu", "false");
    PSXmlDocumentBuilder.addElement(doc, root, "Description", "A page type");

    PSItemDefSummary restored = new PSItemDefSummary(root);
    assertEquals("percPage", restored.getName());
    assertEquals("Page", restored.getLabel());
    assertEquals(301, restored.getTypeId());
    assertEquals("../rx_cePage/page.html", restored.getEditorUrl());
    assertEquals("A page type", restored.getDescription());
    assertFalse(restored.getHideFromMenu());
  }

  @Test
  public void slotTypeContentTypeVariantElementCtorUsesCreateKeyDefault() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSKey key =
        new PSKey(
            new String[] {"SLOTID", "CONTENTTYPEID", "VARIANTID"},
            new String[] {"5", "301", "42"},
            true);
    Element root = doc.createElement("PSXSlotTypeContentTypeVariant");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.appendChild(key.toXml(doc));

    PSSlotTypeContentTypeVariant restored = new PSSlotTypeContentTypeVariant(root);
    assertEquals(5, restored.getSlotId());
    assertEquals(301L, restored.getContentTypeId());
    assertEquals(42, restored.getVariantId());
  }

  /**
   * Builds a minimal {@code PSXVariantSlotType} document matching {@link
   * PSVariantSlotType#toXml(Document)} / super key serialization.
   */
  private static Element sampleVariantSlotTypeElement(Document doc, int variantId, int slotId) {
    PSKey key =
        new PSKey(
            new String[] {"VARIANTID", "SLOTID"},
            new String[] {String.valueOf(variantId), String.valueOf(slotId)},
            true);
    Element root = doc.createElement("PSXVariantSlotType");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.appendChild(key.toXml(doc));
    return root;
  }

  private static Element sampleContentTypeElement(
      Document doc,
      int typeId,
      String name,
      String label,
      String editorUrl,
      boolean hideFromMenu,
      int objectType) {
    // PSContentType.createKey(Element) expects a PSXKey node (new PSKey(el)), not PSXSimpleKey.
    PSKey key =
        new PSKey(new String[] {"CONTENTTYPEID"}, new String[] {String.valueOf(typeId)}, true);

    Element root = doc.createElement("PSXContentType");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.setAttribute("name", name);
    root.setAttribute("label", label);
    root.setAttribute("hideFromMenu", hideFromMenu ? "1" : "0");
    root.setAttribute("objectType", String.valueOf(objectType));
    root.appendChild(key.toXml(doc));
    PSXmlDocumentBuilder.addElement(doc, root, "QueryRequest", editorUrl);
    return root;
  }

  private static Element sampleSlotTypeElement(Document doc, int slotId, String slotName) {
    // PSSlotType.createKey(Element) expects a PSXKey node (new PSKey(el)).
    PSKey key = new PSKey(new String[] {"SLOTID"}, new String[] {String.valueOf(slotId)}, true);

    Element root = doc.createElement("PSXSlotType");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.setAttribute("systemSlot", "0");
    root.setAttribute("slotType", "0");
    root.setAttribute("allowedRelationshipName", "ActiveAssembly");
    root.appendChild(key.toXml(doc));
    PSXmlDocumentBuilder.addElement(doc, root, "SlotName", slotName);
    PSXmlDocumentBuilder.addElement(doc, root, "SlotDesc", "desc");
    return root;
  }
}
