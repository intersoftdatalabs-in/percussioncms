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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.PSUnknownDocTypeException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.extension.PSDatabaseFunction;
import com.percussion.extension.PSDatabaseFunctionManager;
import com.percussion.i18n.PSLocale;
import com.percussion.server.actions.PSAction;
import com.percussion.server.actions.PSActionSet;
import com.percussion.server.cache.PSKeyRules;
import com.percussion.server.job.PSJobException;
import com.percussion.server.job.PSJobHandlerConfiguration;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Batch L (#3080): remaining production call sites outside design.objectstore K must throw typed
 * {@link ObjectStoreErrorCodes} (or {@code .numericCode()} peers where ctors are int-only) — not
 * bare {@code IPSObjectStoreErrors} ints.
 */
public class PSObjectStoreErrorCodesBatchLTest {

  @Test
  public void actionWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotAction");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSAction(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void actionMissingNameUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = PSXmlDocumentBuilder.createRoot(doc, PSAction.XML_NODE_NAME);
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSAction(empty));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void actionSetWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotActionSet");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSActionSet(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void actionSetMissingNameUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = PSXmlDocumentBuilder.createRoot(doc, PSActionSet.XML_NODE_NAME);
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSActionSet(empty));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void keyRulesWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotKeyRules");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSKeyRules(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void jobExceptionWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotJobException");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSJobException(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void jobExceptionMissingMsgCodeUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = PSXmlDocumentBuilder.createRoot(doc, PSJobException.XML_NODE_NAME);
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSJobException(empty));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void jobHandlerConfigNullRootUsesTypedNull() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    // no root element
    PSUnknownDocTypeException ex =
        assertThrows(PSUnknownDocTypeException.class, () -> new PSJobHandlerConfiguration(doc));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void jobHandlerConfigWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(doc, "NotJobHandlerConfig");
    PSUnknownDocTypeException ex =
        assertThrows(PSUnknownDocTypeException.class, () -> new PSJobHandlerConfiguration(doc));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void localeWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotLocale");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSLocale(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void databaseFunctionWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDatabaseFunction");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () ->
                new PSDatabaseFunction(
                    PSDatabaseFunctionManager.FUNCTION_TYPE_SYSTEM, wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void databaseFunctionMissingNameUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty =
        PSXmlDocumentBuilder.createRoot(doc, PSDatabaseFunction.getNodeName());
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () ->
                new PSDatabaseFunction(
                    PSDatabaseFunctionManager.FUNCTION_TYPE_SYSTEM, empty));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  public void requestPageMapNullRequestorUsesNumericDatasetRequestorNull() throws Exception {
    // PSDataSet with no requestor configured
    com.percussion.design.objectstore.PSDataSet dataSet =
        new com.percussion.design.objectstore.PSDataSet("batchL");
    PSIllegalArgumentException ex =
        assertThrows(
            PSIllegalArgumentException.class, () -> new PSRequestPageMap(dataSet, null));
    assertEquals(
        ObjectStoreErrorCodes.DATASET_REQUESTOR_NULL.numericCode(), ex.getErrorCode());
    // int-only ctor: typed code is not retained
  }
}
