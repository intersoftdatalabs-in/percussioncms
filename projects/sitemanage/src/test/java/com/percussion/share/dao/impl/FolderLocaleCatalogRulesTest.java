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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.i18n.PSLocale;
import com.percussion.pathmanagement.data.PSFolderLocaleCatalog;
import com.percussion.pathmanagement.data.PSFolderLocaleChoice;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Folder locale catalog codes are active language strings, sorted by name (#5106). */
class FolderLocaleCatalogRulesTest {

  @Test
  void fromChoices_keepsLanguageCodesSortsByNameAndDropsDuplicates() {
    PSFolderLocaleCatalog catalog =
        FolderLocaleCatalogRules.fromChoices(
            Arrays.asList(
                choice("fr-fr", "French"),
                null,
                choice("en-us", "English"),
                choice("FR-FR", "French again"),
                choice(" ", "Blank"),
                choice("not a locale", "Bad"),
                choice("../en", "Path"),
                choice(" de-de ", "  German  ")));

    assertEquals(3, catalog.getChoices().size());
    assertEquals("English", catalog.getChoices().get(0).getName());
    assertEquals("en-us", catalog.getChoices().get(0).getCode());
    assertEquals("French", catalog.getChoices().get(1).getName());
    assertEquals("fr-fr", catalog.getChoices().get(1).getCode());
    assertEquals("German", catalog.getChoices().get(2).getName());
    assertEquals("de-de", catalog.getChoices().get(2).getCode());
    assertTrue(FolderLocaleCatalogRules.contains(catalog, "FR-FR"));
    assertEquals("French", FolderLocaleCatalogRules.nameOf(catalog, "fr-fr"));
    assertFalse(FolderLocaleCatalogRules.contains(catalog, "es-es"));
    assertFalse(FolderLocaleCatalogRules.contains(catalog, " "));
    assertFalse(FolderLocaleCatalogRules.contains(null, "en-us"));
    assertNull(FolderLocaleCatalogRules.nameOf(catalog, "es-es"));
    assertTrue(FolderLocaleCatalogRules.sameCode("en-us", "EN-US"));
    assertFalse(FolderLocaleCatalogRules.sameCode("en-us", "fr-fr"));
    assertFalse(FolderLocaleCatalogRules.sameCode("", "en-us"));
    assertFalse(FolderLocaleCatalogRules.sameCode(null, null));
  }

  @Test
  void blankName_usesTheCode() {
    PSFolderLocaleCatalog catalog =
        FolderLocaleCatalogRules.fromChoices(Arrays.asList(choice("ja-jp", "  ")));
    assertEquals("ja-jp", catalog.getChoices().get(0).getName());
    assertEquals("ja-jp", FolderLocaleCatalogRules.nameOf(catalog, "JA-JP"));
  }

  @Test
  void choicesFromLocales_keepsActiveRowsOnly() {
    PSLocale english = new PSLocale("en-us", "English", "", PSLocale.STATUS_ACTIVE);
    PSLocale retired = new PSLocale("xx-xx", "Retired", "", PSLocale.STATUS_INACTIVE);
    PSLocale french = new PSLocale("fr-fr", "French", "", PSLocale.STATUS_ACTIVE);
    french.setLanguageString("  ");
    List<PSFolderLocaleChoice> raw =
        FolderLocaleCatalogRules.choicesFromLocales(Arrays.asList(english, null, retired, french));
    PSFolderLocaleCatalog catalog = FolderLocaleCatalogRules.fromChoices(raw);
    assertEquals(1, catalog.getChoices().size());
    assertEquals("en-us", catalog.getChoices().get(0).getCode());
    assertEquals("English", catalog.getChoices().get(0).getName());
  }

  private static PSFolderLocaleChoice choice(String code, String name) {
    PSFolderLocaleChoice choice = new PSFolderLocaleChoice();
    choice.setCode(code);
    choice.setName(name);
    return choice;
  }
}
