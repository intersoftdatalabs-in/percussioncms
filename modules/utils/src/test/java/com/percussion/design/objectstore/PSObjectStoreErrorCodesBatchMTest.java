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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.error.PSException;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.utils.exceptions.ConnectorConfigurationException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Batch M (#3082): modules/utils production call sites must throw typed {@link ObjectStoreErrorCode}
 * (utils-local {@code IPSErrorCode} peers matching ObjectStoreErrorCodes numeric codes) — not bare
 * {@code IPSObjectStoreErrors} ints.
 */
@Tag("UnitTest")
public class PSObjectStoreErrorCodesBatchMTest {

  @Test
  public void objectStoreErrorCodePeersMatchLegacyInts() {
    assertEquals(
        IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE,
        ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE.numericCode());
    assertEquals(
        IPSObjectStoreErrors.XML_ELEMENT_INVALID_ATTR,
        ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode());
    assertEquals(
        IPSObjectStoreErrors.XML_ELEMENT_INVALID_CHILD,
        ObjectStoreErrorCode.XML_ELEMENT_INVALID_CHILD.numericCode());
    assertFalse(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE.isAuditable());
    assertFalse(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR.isAuditable());
    assertFalse(ObjectStoreErrorCode.XML_ELEMENT_INVALID_CHILD.isAuditable());
  }

  @Test
  public void xmlDomUtilCheckNodeWrongTypeUsesTypedCode() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Actual");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> PSXMLDomUtil.checkNode(el, "Expected"));
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void xmlDomUtilCheckAttributeMissingUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Root");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> PSXMLDomUtil.checkAttribute(el, "req", true));
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void xmlDomUtilCheckAttributeEnumeratedIllegalUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = PSXmlDocumentBuilder.createRoot(doc, "Root");
    el.setAttribute("mode", "illegal");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () ->
                PSXMLDomUtil.checkAttributeEnumerated(
                    el, "mode", new String[] {"default", "ok"}, false));
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void connectorConfigWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotConnectorConfigException");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new ConnectorConfigurationException(wrong));
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void connectorConfigInvalidMsgCodeUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root =
        PSXmlDocumentBuilder.createRoot(doc, ConnectorConfigurationException.XML_NODE_NAME);
    root.setAttribute("msgCode", "not-an-int");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new ConnectorConfigurationException(root));
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void unknownNodeTypeConvenienceCtorUsesTypedInvalidChild() {
    PSException nested = new PSException(ObjectStoreErrorCode.XML_ELEMENT_WRONG_TYPE, "nested");
    PSUnknownNodeTypeException ex =
        new PSUnknownNodeTypeException("Parent", "Child", nested);
    assertEquals(ObjectStoreErrorCode.XML_ELEMENT_INVALID_CHILD.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCode.XML_ELEMENT_INVALID_CHILD, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
