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

package com.percussion.i18n.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.BeansErrorCodes;
import com.percussion.error.IPSBeansErrors;
import com.percussion.error.PSBeansException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Issue #4264 (parent #2616 leftover): {@link PSI18NTranslationKeyValues} throws typed {@link
 * BeansErrorCodes#XML_PROCESSING_ERROR}. Catalog code is non-auditable.
 */
@Tag("UnitTest")
class PSI18NTranslationKeyValuesTypedErrorCodeSliceTest {

  @Test
  void xmlProcessingErrorMatchesLegacyIntAndSkipsAudit() {
    assertEquals(
        IPSBeansErrors.XML_PROCESSING_ERROR, BeansErrorCodes.XML_PROCESSING_ERROR.numericCode());
    assertFalse(BeansErrorCodes.XML_PROCESSING_ERROR.isAuditable());
  }

  @Test
  void fromXmlInvalidKeyAttributeUsesTypedCode() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = doc.createElement("KeyValues");
    Element bad = doc.createElement("KeyValue");
    // missing required key attribute triggers processing failure path
    root.appendChild(bad);
    doc.appendChild(root);

    PSBeansException ex =
        assertThrows(
            PSBeansException.class,
            () -> PSI18NTranslationKeyValues.getInstance().fromXml(root));
    assertSame(BeansErrorCodes.XML_PROCESSING_ERROR, ex.getTypedErrorCode());
    assertEquals(BeansErrorCodes.XML_PROCESSING_ERROR.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }
}
