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

import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Batch K3 (#3067): design.objectstore Q–Z + server/legacy call sites must throw typed {@link
 * ObjectStoreErrorCodes} (or Design-owned ACL/SPINST codes on {@link DesignErrorCodes}) where
 * IPSErrorCode ctors exist — not bare {@code IPSObjectStoreErrors} ints.
 */
public class PSDesignObjectstoreTypedErrorCodeBatchK3Test {

  @Test
  public void referenceNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSReference((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void referenceWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotReference");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSReference(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void relationshipSetNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSRelationshipSet((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void relationshipSetWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotRelationshipSet");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSRelationshipSet(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void ruleNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSRule((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void ruleWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotRule");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSRule(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void tableRefNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSTableRef((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void tableRefWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotTableRef");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSTableRef(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void textLiteralNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSTextLiteral((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void textLiteralWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotTextLiteral");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSTextLiteral(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void urlRequestNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSUrlRequest((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void urlRequestWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotUrlRequest");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSUrlRequest(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void visibilityRulesNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSVisibilityRules((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void visibilityRulesWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotVisibilityRules");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSVisibilityRules(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void resultPagerNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSResultPager((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void resultPagerWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotResultPager");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSResultPager(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void recipientEmptyNameUsesNumericObjectStoreCode() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> new PSRecipient(""));
    assertEquals(ObjectStoreErrorCodes.RECIPIENT_NAME_EMPTY.numericCode(), ex.getErrorCode());
  }

  @Test
  public void serverConfigurationNullAclUsesDesignCode() {
    PSServerConfiguration cfg = new PSServerConfiguration();
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> cfg.setAcl(null));
    assertEquals(DesignErrorCodes.SRV_ACL_NULL.numericCode(), ex.getErrorCode());
  }

  @Test
  public void relationshipPropertyNameEmptyUsesNumericCode() {
    assertEquals(
        ObjectStoreErrorCodes.RELATIONSHIP_PROPERTY_NAME_EMPTY.numericCode(),
        new PSRelationshipProperty("x").getErrorCode());
  }

  @Test
  public void xmlFieldNameEmptyUsesNumericCode() {
    assertEquals(
        ObjectStoreErrorCodes.XML_FIELD_NAME_EMPTY.numericCode(),
        new PSXmlField("x").getErrorCode());
  }
}
