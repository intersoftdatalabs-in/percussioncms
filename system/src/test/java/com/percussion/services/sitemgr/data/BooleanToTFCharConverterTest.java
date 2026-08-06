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
package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for CHAR(1) T/F boolean conversion used by {@link PSSite}. */
class BooleanToTFCharConverterTest {

  private final BooleanToTFCharConverter converter = new BooleanToTFCharConverter();

  @Test
  void convertToDatabaseColumn_usesSingleCharTF() {
    assertEquals("T", converter.convertToDatabaseColumn(Boolean.TRUE));
    assertEquals("F", converter.convertToDatabaseColumn(Boolean.FALSE));
    assertNull(converter.convertToDatabaseColumn(null));
    // Critical: must not emit multi-char Boolean.toString() values
    assertEquals(1, converter.convertToDatabaseColumn(true).length());
    assertEquals(1, converter.convertToDatabaseColumn(false).length());
  }

  @Test
  void convertToEntityAttribute_acceptsLegacyForms() {
    assertTrue(converter.convertToEntityAttribute("T"));
    assertTrue(converter.convertToEntityAttribute("t"));
    assertTrue(converter.convertToEntityAttribute("Y"));
    assertTrue(converter.convertToEntityAttribute("1"));
    assertTrue(converter.convertToEntityAttribute("true"));
    assertFalse(converter.convertToEntityAttribute("F"));
    assertFalse(converter.convertToEntityAttribute("N"));
    assertFalse(converter.convertToEntityAttribute("0"));
    assertFalse(converter.convertToEntityAttribute("false"));
    assertNull(converter.convertToEntityAttribute(null));
    assertNull(converter.convertToEntityAttribute("  "));
  }

  @Test
  void siteCanonicalFlags_roundTripAsSingleChar() {
    PSSite site = new PSSite();
    site.setCanonical(true);
    site.setCanonicalReplace(false);
    site.setSecure(true);
    site.setGenerateSitemap(false);
    site.setPageBased(true);

    assertTrue(site.isCanonical());
    assertFalse(site.isCanonicalReplace());
    assertTrue(site.isSecure());
    assertFalse(site.isGenerateSitemap());
    assertTrue(site.isPageBased());

    // Field values used by Hibernate must fit CHAR(1)
    assertEquals("T", BooleanToTFCharConverter.toChar(true));
    assertEquals("F", BooleanToTFCharConverter.toChar(false));
  }
}
