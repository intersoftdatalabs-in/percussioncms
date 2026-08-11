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

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Batch K1 (#3062): design.objectstore D–H cohort call sites must throw typed {@link
 * ObjectStoreErrorCodes} (not bare {@code IPSObjectStoreErrors} ints) where IPSErrorCode ctors
 * exist.
 */
public class PSDesignObjectstoreTypedErrorCodeBatchK1Test {

  @Test
  public void dataMappingNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSDataMapping((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void dataMappingWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDataMapping");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSDataMapping(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void defaultSelectedNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSDefaultSelected((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void defaultSelectedWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDefaultSelected");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSDefaultSelected(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void entryNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSEntry((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void entryWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotEntry");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSEntry(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void displayMapperNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSDisplayMapper((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void displayMapperWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDisplayMapper");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSDisplayMapper(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void formActionNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class,
            () -> new PSFormAction((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void formActionWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotFormAction");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSFormAction(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void dataEncryptorNullSourceUsesTypedNull() {
    PSDataEncryptor encryptor = new PSDataEncryptor(false);
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> encryptor.fromXml(null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void dataEncryptorWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDataEncryptor");
    PSDataEncryptor encryptor = new PSDataEncryptor(false);
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> encryptor.fromXml(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  @Test
  public void fieldSetNullSourceUsesTypedNull() {
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSFieldSet((Element) null, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void fieldSetWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotFieldSet");
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSFieldSet(wrong, null, null));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }
}
