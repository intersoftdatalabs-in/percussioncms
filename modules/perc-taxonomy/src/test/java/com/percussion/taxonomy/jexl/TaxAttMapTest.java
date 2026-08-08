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
package com.percussion.taxonomy.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Attribute_lang;
import com.percussion.taxonomy.domain.Language;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link TaxAttMap} construction from Hibernate attribute langs. */
public class TaxAttMapTest {

  @Test
  public void emptyConstructor_createsEmptyMap() {
    TaxAttMap map = new TaxAttMap();
    assertTrue(map.isEmpty());
  }

  @Test
  public void constructorFromAttributeLangs_indexesByName() {
    Attribute attribute = new Attribute();
    attribute.setId(42);
    attribute.setIs_multiple(true);
    attribute.setIs_required(false);
    attribute.setAttribute_langs(new ArrayList<>());

    Language language = new Language();
    language.setName("English");

    Attribute_lang attLang = new Attribute_lang();
    attLang.setName("Color");
    attLang.setAttribute(attribute);
    attLang.setLanguage(language);
    attribute.addAttribute_lang(attLang);

    List<Attribute_lang> langs = new ArrayList<>();
    langs.add(attLang);

    TaxAttMap map = new TaxAttMap(langs);
    assertEquals(1, map.size());
    TaxAttribute taxAttribute = map.get("Color");
    assertNotNull(taxAttribute);
    assertEquals(42, taxAttribute.getId());
    assertEquals("Color", taxAttribute.getName());
    assertTrue(taxAttribute.isMultiple());
  }
}
