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
package com.percussion.services.utils.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Unit tests for Betwixt-compatible XML element naming (shared by Jackson + Betwixt helpers). */
class PSXmlElementNameMapperTest {

  @Test
  void stripsPsPrefixAndHyphenates() {
    assertEquals("keyword", PSXmlElementNameMapper.mapTypeToElementName("PSKeyword"));
    assertEquals("keyword-choice", PSXmlElementNameMapper.mapTypeToElementName("PSKeywordChoice"));
  }

  @Test
  void stripsIpsPrefix() {
    assertEquals("guid", PSXmlElementNameMapper.mapTypeToElementName("IPSGuid"));
  }

  @Test
  void mapsSampleKeywordPilotNames() {
    assertEquals("sample-keyword", PSXmlElementNameMapper.mapTypeToElementName("SampleKeyword"));
    assertEquals("sample-choice", PSXmlElementNameMapper.mapTypeToElementName("SampleChoice"));
  }

  @Test
  void flattensMultiCapRunsBeforeHyphenation() {
    // GUID -> Guid then hyphenate -> guid (not g-u-i-d)
    assertEquals("guid", PSXmlElementNameMapper.mapTypeToElementName("PSGUID"));
    // AAType -> Aatype (second capital lowered) then hyphenate -> aatype (not a-a-type)
    assertEquals("aatype", PSXmlElementNameMapper.mapTypeToElementName("PSAAType"));
  }

  @Test
  void innerClassUsesSegmentAfterDollar() {
    assertEquals(
        "sample-choice", PSXmlElementNameMapper.mapTypeToElementName("Outer$SampleChoice"));
  }

  @Test
  void propertyNamesHyphenateWithoutPsStrip() {
    assertEquals(
        "content-type-id", PSXmlElementNameMapper.mapPropertyToElementName("contentTypeId"));
    assertEquals("label", PSXmlElementNameMapper.mapPropertyToElementName("label"));
  }
}
