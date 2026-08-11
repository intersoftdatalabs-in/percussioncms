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
package com.percussion.cx.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Batch H: cx.objectstore call sites must throw typed {@link ObjectStoreErrorCodes} (not bare
 * {@code IPSObjectStoreErrors} ints).
 */
public class PSCxObjectstoreTypedErrorCodeTest {

  @Test
  public void propertiesWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotProps");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSProperties(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void propertiesMissingChildUsesTypedInvalidChild() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = PSXmlDocumentBuilder.createRoot(doc, PSProperties.XML_NODE_NAME);
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSProperties(empty));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, ex.getTypedErrorCode());
  }

  @Test
  public void parametersWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotParams");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSParameters(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void menuBarWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotMenuBar");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSMenuBar(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void menuActionWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotAction");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSMenuAction(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void componentUtilsInvalidEnumeratedAttrUsesTypedInvalidAttr() throws Exception {
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
  public void componentUtilsMissingRequiredAttrUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Sample");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> PSComponentUtils.getRequiredAttribute(el, "mustHave"));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void componentUtilsMissingRequiredChildUsesTypedInvalidChild() throws Exception {
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
