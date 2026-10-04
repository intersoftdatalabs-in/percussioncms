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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSFolderAllowedSiteChoice;
import com.percussion.pathmanagement.data.PSFolderAllowedSitesCatalog;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.impl.FolderAllowedSitesCatalogRules.Kind;
import com.percussion.share.dao.impl.FolderAllowedSitesCatalogRules.Parsed;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Allowed publish site ids are positive decimals, sorted, and catalogued by id (#5132). */
class FolderAllowedSitesCatalogRulesTest {

  @Test
  void parse_sortsUniqueIdsAndRejectsJunk() {
    Parsed listed = FolderAllowedSitesCatalogRules.parse(" 302, 301,0302 ");
    assertEquals(Kind.LIST, listed.kind());
    assertEquals("301,302", listed.canonical());

    Parsed cleared = FolderAllowedSitesCatalogRules.parse("  ");
    assertEquals(Kind.CLEAR, cleared.kind());
    assertEquals("", cleared.canonical());

    assertEquals(Kind.OMIT, FolderAllowedSitesCatalogRules.parse(null).kind());
    assertEquals(Kind.INVALID, FolderAllowedSitesCatalogRules.parse("301,abc").kind());
    assertEquals(Kind.INVALID, FolderAllowedSitesCatalogRules.parse("0").kind());
    assertEquals(Kind.INVALID, FolderAllowedSitesCatalogRules.parse("301,").kind());
    assertEquals(Kind.INVALID, FolderAllowedSitesCatalogRules.parse("-4").kind());
    assertEquals(Kind.INVALID, FolderAllowedSitesCatalogRules.parse("Enterprise").kind());
  }

  @Test
  void sameList_ignoresOrderAndTreatsBlankAsEmpty() {
    assertTrue(FolderAllowedSitesCatalogRules.sameList("302,301", "301, 302"));
    assertTrue(FolderAllowedSitesCatalogRules.sameList(null, ""));
    assertTrue(FolderAllowedSitesCatalogRules.sameList("0301", "301"));
    assertFalse(FolderAllowedSitesCatalogRules.sameList("301", "301,302"));
    assertFalse(FolderAllowedSitesCatalogRules.sameList("301", null));
    assertTrue(FolderAllowedSitesCatalogRules.sameRaw(" abc ", "abc"));
    assertFalse(FolderAllowedSitesCatalogRules.sameRaw(null, ""));
    assertTrue(FolderAllowedSitesCatalogRules.sameList("nope", "nope"));
    assertFalse(FolderAllowedSitesCatalogRules.sameList("nope", "301"));
  }

  @Test
  void fromSites_keepsPositiveNamedSitesAndSortsByName() {
    IPSSite corporate = site(302L, "Corporate");
    IPSSite enterprise = site(301L, "Enterprise");
    IPSSite nameless = site(303L, "  ");
    IPSSite missingId = site(null, "Ghost");
    PSFolderAllowedSitesCatalog catalog =
        FolderAllowedSitesCatalogRules.fromSites(
            Arrays.asList(corporate, null, enterprise, nameless, missingId));
    assertEquals(2, catalog.getChoices().size());
    assertEquals("302", catalog.getChoices().get(0).getId());
    assertEquals("Corporate", catalog.getChoices().get(0).getName());
    assertEquals("301", catalog.getChoices().get(1).getId());
    assertEquals("Enterprise", catalog.getChoices().get(1).getName());
    assertTrue(FolderAllowedSitesCatalogRules.contains(catalog, "0301"));
    assertEquals("Corporate", FolderAllowedSitesCatalogRules.nameOf(catalog, "302"));
    assertFalse(FolderAllowedSitesCatalogRules.contains(catalog, "999"));
    assertFalse(FolderAllowedSitesCatalogRules.contains(catalog, "Enterprise"));
    assertNull(FolderAllowedSitesCatalogRules.nameOf(catalog, "999"));
    assertTrue(FolderAllowedSitesCatalogRules.containsAll(catalog, "302,301"));
    assertFalse(FolderAllowedSitesCatalogRules.containsAll(catalog, "301,999"));
    assertFalse(FolderAllowedSitesCatalogRules.containsAll(catalog, ""));
    assertFalse(FolderAllowedSitesCatalogRules.containsAll(null, "301"));
  }

  @Test
  void fromChoices_dropsNamesAndDuplicateIds() {
    PSFolderAllowedSitesCatalog catalog =
        FolderAllowedSitesCatalogRules.fromChoices(
            Arrays.asList(choice("12", "Beta"), choice("12", "Other"), choice("x", "Nope"), null));
    assertEquals(1, catalog.getChoices().size());
    assertEquals("Beta", catalog.getChoices().get(0).getName());
    assertEquals(0, FolderAllowedSitesCatalogRules.fromSites(null).getChoices().size());
    assertEquals(0, FolderAllowedSitesCatalogRules.fromChoices(List.of()).getChoices().size());
  }

  private static IPSSite site(Long id, String name) {
    IPSSite site = mock(IPSSite.class);
    when(site.getSiteId()).thenReturn(id);
    when(site.getName()).thenReturn(name);
    return site;
  }

  private static PSFolderAllowedSiteChoice choice(String id, String name) {
    PSFolderAllowedSiteChoice choice = new PSFolderAllowedSiteChoice();
    choice.setId(id);
    choice.setName(name);
    return choice;
  }
}
