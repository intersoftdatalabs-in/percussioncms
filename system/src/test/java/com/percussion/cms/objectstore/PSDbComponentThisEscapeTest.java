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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for {@link PSDbComponent} Element super-ctor / {@code fromXmlBase} + {@code
 * createKeyDefault} path (#2404 review follow-up). Complements design.objectstore coverage in
 * {@code PSDesignObjectStoreThisEscapeTest}.
 *
 * <p>Uses {@link PSVariantSlotType} because it calls {@code super(source)} then {@code
 * fromXml(source)} and stores a multi-part {@link PSKey} (not {@link PSSimpleKey}, whose node name
 * mismatches the PSContentType {@code createKey} override).
 */
public class PSDbComponentThisEscapeTest {

  @Test
  public void variantSlotTypeElementCtorRoundTripUsesCreateKeyDefaultThenFromXml()
      throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source = sampleVariantSlotTypeElement(doc, 42, 7);

    // Element ctor: super(source) → createKeyDefault (non-virtual), then subclass fromXml →
    // virtual createKey (PSKey(Element)).
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
}
