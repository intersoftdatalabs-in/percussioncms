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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Batch H: cms.objectstore {@link PSComponentUtils} uses typed {@link ObjectStoreErrorCodes}.
 */
public class PSCmsComponentUtilsTypedErrorCodeTest {

  @Test
  public void invalidEnumeratedAttrUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Sample");
    el.setAttribute("kind", "nope");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> PSComponentUtils.getEnumeratedAttribute(el, "kind", List.of("a", "b")));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void missingRequiredChildUsesTypedInvalidChild() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Sample");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> PSComponentUtils.getChildElement(el, "Child", true));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, ex.getTypedErrorCode());
  }
}
