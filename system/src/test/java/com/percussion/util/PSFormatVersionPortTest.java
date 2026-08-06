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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.system.utils.PSFormatVersion;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.StringReader;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Behavioral regression for v8.1.7 PR #921 review: short/null buildNumber must not crash. */
class PSFormatVersionPortTest {

  private static final String XML_TEMPLATE =
      """
      <PSXFormatVersion buildId="1" buildNumber="%s" displayVersion="8.2.0"
      interfaceVersion="9" majorVersion="8" microVersion="0" minorVersion="2" optionalId=""
      versionString="release"/>
      """;

  @Test
  void shortBuildNumberDoesNotThrow() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(String.format(XML_TEMPLATE, "123")), false);
    PSFormatVersion fv = PSFormatVersion.createFromXml(doc.getDocumentElement());
    String vs = assertDoesNotThrow(fv::getVersionString, "short buildNumber must not throw");
    assertTrue(vs.contains("Build"));
    assertFalse(vs.contains("null"), "must not append literal null");
  }

  @Test
  void normalBuildNumberStillFormats() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(String.format(XML_TEMPLATE, "20260701")), false);
    PSFormatVersion fv = PSFormatVersion.createFromXml(doc.getDocumentElement());
    String vs = fv.getVersionString();
    assertTrue(vs.contains("Build"));
    assertTrue(vs.contains("202607"));
  }

  @Test
  void nullBuildNumberAppendsUnknownNotNullLiteral() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(String.format(XML_TEMPLATE, "20260701")), false);
    PSFormatVersion fv = PSFormatVersion.createFromXml(doc.getDocumentElement());
    Field f = PSFormatVersion.class.getDeclaredField("m_buildNumber");
    f.setAccessible(true);
    f.set(fv, null);
    String vs = assertDoesNotThrow(fv::getVersionString);
    assertTrue(vs.contains("unknown"), vs);
    assertFalse(vs.contains("null"), vs);
  }
}
