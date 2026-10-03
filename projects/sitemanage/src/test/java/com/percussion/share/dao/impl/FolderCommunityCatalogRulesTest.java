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

import com.percussion.pathmanagement.data.PSFolderCommunityCatalog;
import com.percussion.pathmanagement.data.PSFolderCommunityChoice;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Folder community catalog ids are positive community ids, sorted by name (#5105). */
class FolderCommunityCatalogRulesTest {

  @Test
  void fromChoices_keepsPositiveIdsSortsByNameAndDropsDuplicates() {
    PSFolderCommunityCatalog catalog =
        FolderCommunityCatalogRules.fromChoices(
            Arrays.asList(
                choice("12", "Enterprise"),
                null,
                choice("10", "Default"),
                choice("12", "Enterprise again"),
                choice("0", "None"),
                choice("-1", "All"),
                choice("nope", "Bad"),
                choice(" 8 ", "  Extra  ")));

    assertEquals(3, catalog.getChoices().size());
    assertEquals("Default", catalog.getChoices().get(0).getName());
    assertEquals("10", catalog.getChoices().get(0).getId());
    assertEquals("Enterprise", catalog.getChoices().get(1).getName());
    assertEquals("12", catalog.getChoices().get(1).getId());
    assertEquals("Extra", catalog.getChoices().get(2).getName());
    assertEquals("8", catalog.getChoices().get(2).getId());
    assertTrue(FolderCommunityCatalogRules.contains(catalog, 12));
    assertEquals("Enterprise", FolderCommunityCatalogRules.nameOf(catalog, 12));
    assertFalse(FolderCommunityCatalogRules.contains(catalog, 9));
    assertFalse(FolderCommunityCatalogRules.contains(catalog, 0));
    assertFalse(FolderCommunityCatalogRules.contains(null, 12));
    assertNull(FolderCommunityCatalogRules.nameOf(catalog, 9));
  }

  @Test
  void blankName_usesTheId() {
    PSFolderCommunityCatalog catalog =
        FolderCommunityCatalogRules.fromChoices(Arrays.asList(choice("4", "  ")));
    assertEquals("4", catalog.getChoices().get(0).getName());
    assertEquals("4", FolderCommunityCatalogRules.nameOf(catalog, 4));
  }

  private static PSFolderCommunityChoice choice(String id, String name) {
    PSFolderCommunityChoice choice = new PSFolderCommunityChoice();
    choice.setId(id);
    choice.setName(name);
    return choice;
  }
}
