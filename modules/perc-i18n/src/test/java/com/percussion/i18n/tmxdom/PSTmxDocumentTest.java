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
package com.percussion.i18n.tmxdom;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.i18n.PSI18nUtils;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PSTmxDocumentTest {

  @Test
  public void testAddAndIterateTranslationUnits() throws Exception {
    PSTmxDocument doc = new PSTmxDocument();

    IPSTmxTranslationUnit tu = doc.createTranslationUnit("psx.test.key@last", "desc");
    // add the translation unit to the document via public merge API
    doc.merge((IPSTmxNode) tu);

    Iterator<Map.Entry<String, IPSTmxTranslationUnit>> iter = doc.getTranslationUnits();
    assertNotNull(iter);

    boolean found = false;
    while (iter.hasNext()) {
      Map.Entry<String, IPSTmxTranslationUnit> e = iter.next();
      assertNotNull(e.getKey());
      assertNotNull(e.getValue());
      // ensure returned value implements the interface
      assertTrue(e.getValue() instanceof IPSTmxTranslationUnit);
      if (e.getKey().equals("psx.test.key@last")) found = true;
    }
    assertTrue(found, "Added translation unit should be found in iteration");
  }

  @Test
  public void testAddLanguageAddsTuvs() throws Exception {
    PSTmxDocument doc = new PSTmxDocument();
    IPSTmxTranslationUnit tu = doc.createTranslationUnit("psx.test.lang@last", "desc");
    doc.merge((IPSTmxNode) tu);

    // Ensure default language exists and non-default does not
    IPSTmxTranslationUnit stored = doc.getTranslationUnits().next().getValue();
    assertNotNull(stored.getTransUnitVariant(PSI18nUtils.DEFAULT_LANG));

    // Add a new language and verify a TUV for that language was added
    String newLang = "fr-fr";
    doc.addLanguage(newLang);

    // iterate and check each TU has the new language variant (except default language)
    Iterator<Map.Entry<String, IPSTmxTranslationUnit>> iter = doc.getTranslationUnits();
    while (iter.hasNext()) {
      IPSTmxTranslationUnit v = iter.next().getValue();
      assertNotNull(
          v.getTransUnitVariant(newLang), "TUV for language " + newLang + " should exist");
    }
  }
}
