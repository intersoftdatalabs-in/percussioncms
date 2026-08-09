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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for residual {@link PSDbComponentSet} subclass type parameters (#2454 / epic
 * #2022). Verifies typed iterators and typed lookup helpers without casts.
 */
public class PSDbComponentSetSubclassesTypedTest {

  @Test
  public void contentTypeSetLookupByNameAndIdIsTyped() throws Exception {
    PSContentTypeSet set = new PSContentTypeSet();
    PSContentType page =
        new PSContentType(10, "percPage", "Page", "desc", "../rx_cePage/page.html", false, 1);
    PSContentType folder =
        new PSContentType(20, "percFolder", "Folder", "", "../rx_ceFolder/folder.html", true, 2);
    set.add(page);
    set.add(folder);

    assertEquals(2, set.size());
    assertSame(page, set.getContentTypeByName("PERCPAGE"));
    assertSame(folder, set.getContentTypeById(20));
    assertNull(set.getContentTypeByName("missing"));
    assertNull(set.getContentTypeById(99));

    Iterator<PSContentType> it = set.iterator();
    assertTrue(it.hasNext());
    PSContentType first = it.next();
    assertNotNull(first.getName());

    assertThrows(IllegalArgumentException.class, () -> set.getContentTypeByName(null));
    assertThrows(IllegalArgumentException.class, () -> set.getContentTypeByName(""));
    assertThrows(IllegalArgumentException.class, () -> set.getContentTypeById(-1));
  }

  @Test
  public void slotTypeSetLookupByNameAndIdIsTyped() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element a = sampleSlotTypeElement(doc, 2, "sys_content");
    Element b = sampleSlotTypeElement(doc, 4, "rffList");

    PSSlotTypeSet set = new PSSlotTypeSet(new Element[] {a, b});
    assertEquals(2, set.size());
    assertEquals("sys_content", set.getSlotTypeByName("SYS_CONTENT").getSlotName());
    assertEquals(4, set.getSlotTypeById(4).getSlotId());
    assertNull(set.getSlotTypeByName("nope"));
    assertNull(set.getSlotTypeById(99));

    Iterator<PSSlotType> it = set.iterator();
    PSSlotType first = it.next();
    assertTrue(first.getSlotId() > 0);

    assertThrows(IllegalArgumentException.class, () -> set.getSlotTypeByName(null));
    assertThrows(IllegalArgumentException.class, () -> set.getSlotTypeById(-1));
  }

  @Test
  public void contentTypeVariantSetLookupByNameAndIdIsTyped() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element a = sampleTemplateElement(doc, 11, "Page");
    Element b = sampleTemplateElement(doc, 25, "Snippet");

    PSContentTypeVariantSet set = new PSContentTypeVariantSet(new Element[] {a, b});
    assertEquals(2, set.size());
    assertEquals("Page", set.getContentVariantByName("page").getName());
    assertEquals(25, set.getContentVariantById(25).getVariantId());
    assertNull(set.getContentVariantByName("missing"));
    assertNull(set.getContentVariantById(99));

    Iterator<PSContentTypeTemplate> it = set.iterator();
    PSContentTypeTemplate first = it.next();
    assertNotNull(first.getName());

    assertThrows(IllegalArgumentException.class, () -> set.getContentVariantByName(""));
    assertThrows(IllegalArgumentException.class, () -> set.getContentVariantById(-1));
  }

  @Test
  public void variantSlotTypeSetTypedIteratorAndSize() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSVariantSlotTypeSet set = new PSVariantSlotTypeSet();
    set.add(new PSVariantSlotType(sampleVariantSlotTypeElement(doc, 42, 7)));
    set.add(new PSVariantSlotType(sampleVariantSlotTypeElement(doc, 42, 8)));
    assertEquals(2, set.size());

    int count = 0;
    Iterator<PSVariantSlotType> it = set.iterator();
    while (it.hasNext()) {
      PSVariantSlotType entry = it.next();
      assertEquals(42, entry.getVariantId());
      assertTrue(entry.getSlotId() == 7 || entry.getSlotId() == 8);
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void slotTypeContentTypeVariantSetIsVariantAllowedTyped() {
    PSSlotTypeContentTypeVariantSet set = new PSSlotTypeContentTypeVariantSet();
    set.add(PSSlotTypeContentTypeVariant.create(1, 301, 11));
    set.add(PSSlotTypeContentTypeVariant.create(1, 302, 25));

    assertTrue(set.isVariantAllowed(11));
    assertTrue(set.isVariantAllowed(25));
    assertFalse(set.isVariantAllowed(99));
    assertThrows(IllegalArgumentException.class, () -> set.isVariantAllowed((PSContentTypeTemplate) null));

    Iterator<PSSlotTypeContentTypeVariant> it = set.iterator();
    PSSlotTypeContentTypeVariant first = it.next();
    assertEquals(1, first.getSlotId());
    assertTrue(first.getVariantId() == 11 || first.getVariantId() == 25);
  }

  private static Element sampleSlotTypeElement(Document doc, int slotId, String name) {
    PSKey key = new PSKey(new String[] {"SLOTID"}, new String[] {String.valueOf(slotId)}, true);
    Element root = doc.createElement("PSXSlotType");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.setAttribute("systemSlot", "0");
    root.setAttribute("slotType", "0");
    root.setAttribute("allowedRelationshipName", "ActiveAssembly");
    root.appendChild(key.toXml(doc));
    Element slotName = doc.createElement("SlotName");
    slotName.appendChild(doc.createTextNode(name));
    root.appendChild(slotName);
    Element slotDesc = doc.createElement("SlotDesc");
    slotDesc.appendChild(doc.createTextNode("desc-" + name));
    root.appendChild(slotDesc);
    return root;
  }

  private static Element sampleTemplateElement(Document doc, int variantId, String name) {
    PSKey key =
        new PSKey(new String[] {"TEMPLATE_ID"}, new String[] {String.valueOf(variantId)}, true);
    Element root = doc.createElement("PSXContentTypeTemplate");
    root.setAttribute("state", IPSDbComponent.STATE_LABELS[IPSDbComponent.DBSTATE_UNMODIFIED]);
    root.setAttribute("outputFormat", "1");
    root.setAttribute("aaType", "0");
    root.appendChild(key.toXml(doc));
    Element desc = doc.createElement("VariantDescription");
    desc.appendChild(doc.createTextNode(name));
    root.appendChild(desc);
    Element url = doc.createElement("AssemblyUrl");
    url.appendChild(doc.createTextNode("../assembly/render"));
    root.appendChild(url);
    return root;
  }

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
}
