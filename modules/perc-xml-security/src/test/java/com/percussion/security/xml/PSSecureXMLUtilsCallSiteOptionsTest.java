/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.security.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.Test;

/** Ensures secure factory helpers accept secureWithDtd options without requesting external XXE. */
class PSSecureXMLUtilsCallSiteOptionsTest {

  @Test
  void secureWithDtdDisablesExternalEntities() {
    PSXmlSecurityOptions opts = PSXmlSecurityOptions.secureWithDtd();
    assertFalse(opts.isEnableExternalEntities());
    assertFalse(opts.isEnableExternalParameterEntities());
    assertTrue(opts.isEnableDtdDeclarations());
    assertTrue(opts.isEnableExternalDtdReferences());
  }

  @Test
  void securedDocumentBuilderFactoryAcceptsSecureWithDtd() {
    DocumentBuilderFactory dbf =
        PSSecureXMLUtils.getSecuredDocumentBuilderFactory(PSXmlSecurityOptions.secureWithDtd());
    assertNotNull(dbf);
  }

  @Test
  void securedSaxParserFactoryAcceptsSecureWithDtd() {
    SAXParserFactory spf =
        PSSecureXMLUtils.getSecuredSaxParserFactory(PSXmlSecurityOptions.secureWithDtd());
    assertNotNull(spf);
  }
}
