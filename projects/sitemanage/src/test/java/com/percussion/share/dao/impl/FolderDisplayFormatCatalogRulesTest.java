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

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.pathmanagement.data.PSFolderDisplayFormatCatalog;
import com.percussion.pathmanagement.data.PSFolderDisplayFormatChoice;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.share.dao.impl.FolderDisplayFormatCatalogRules.IdKind;
import com.percussion.share.dao.impl.FolderDisplayFormatCatalogRules.ParsedId;
import com.percussion.share.dao.impl.FolderDisplayFormatCatalogRules.SourceRow;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Catalog id rules for folder {@code sys_displayformat} (#5131). */
@Tag("UnitTest")
class FolderDisplayFormatCatalogRulesTest {

  @Test
  void prefersFolderFormatsAndFallsBackWhenNoneAreMarked() {
    PSFolderDisplayFormatCatalog onlyFolder =
        FolderDisplayFormatCatalogRules.fromRows(
            Arrays.asList(
                new SourceRow(3, "Default", true),
                new SourceRow(9, "Related", false),
                new SourceRow(0, "Unset", true),
                new SourceRow(-1, "Missing", true)));
    assertEquals(List.of("3"), ids(onlyFolder));
    assertEquals("Default", FolderDisplayFormatCatalogRules.nameOf(onlyFolder, "03"));

    PSFolderDisplayFormatCatalog fallback =
        FolderDisplayFormatCatalogRules.fromRows(
            Arrays.asList(new SourceRow(9, "Related", false), new SourceRow(4, "", false)));
    assertEquals(List.of("4", "9"), ids(fallback));
    assertEquals("4", FolderDisplayFormatCatalogRules.nameOf(fallback, "4"));
  }

  @Test
  void nameIsNotAnIdAndLeadingZerosCollapse() {
    ParsedId name = FolderDisplayFormatCatalogRules.parseId("Simple");
    assertEquals(IdKind.INVALID, name.kind());
    assertEquals("", name.canonical());
    assertEquals(IdKind.ABSENT, FolderDisplayFormatCatalogRules.parseId("0").kind());
    assertEquals(IdKind.ABSENT, FolderDisplayFormatCatalogRules.parseId("  ").kind());
    assertEquals(IdKind.ABSENT, FolderDisplayFormatCatalogRules.parseId("-1").kind());
    assertEquals("12", FolderDisplayFormatCatalogRules.parseId("012").canonical());
    assertTrue(FolderDisplayFormatCatalogRules.sameId("12", "012"));
    assertFalse(FolderDisplayFormatCatalogRules.sameId("12", "Simple"));
    assertFalse(FolderDisplayFormatCatalogRules.sameId(null, null));
    assertFalse(FolderDisplayFormatCatalogRules.contains(null, "12"));
  }

  @Test
  void reloadResolvesStoredIdToCatalogNameAndIgnoresNameOnly() {
    PSFolderDisplayFormatCatalog catalog =
        FolderDisplayFormatCatalogRules.fromRows(
            Arrays.asList(new SourceRow(12, "Simple", true), new SourceRow(3, "Default", true)));

    PSFolderProperties resolved = new PSFolderProperties();
    FolderDisplayFormatCatalogRules.copyOntoProperties(resolved, "012", "  ", catalog);
    assertEquals("12", resolved.getDisplayFormatId());
    assertEquals("Simple", resolved.getDisplayFormatName());

    PSFolderProperties transientWins = new PSFolderProperties();
    FolderDisplayFormatCatalogRules.copyOntoProperties(transientWins, "12", "Default", catalog);
    assertEquals("12", transientWins.getDisplayFormatId());
    assertEquals("Default", transientWins.getDisplayFormatName());

    PSFolderProperties nameOnly = new PSFolderProperties();
    nameOnly.setDisplayFormatName("Simple");
    FolderDisplayFormatCatalogRules.copyOntoProperties(nameOnly, null, "Simple", catalog);
    assertNull(nameOnly.getDisplayFormatId());
    assertEquals("Simple", nameOnly.getDisplayFormatName());
    assertNull(FolderDisplayFormatCatalogRules.nameOf(catalog, "Simple"));
  }

  @Test
  void fromFormatsDropsUnsetIdsAndKeepsTheNumericId() throws PSCmsException {
    PSDisplayFormat unset = new PSDisplayFormat();
    PSDisplayFormat simple = new PSDisplayFormat();
    simple.tuneClone(7);
    simple.setDisplayName("Simple");

    PSFolderDisplayFormatCatalog catalog =
        FolderDisplayFormatCatalogRules.fromFormats(Arrays.asList(null, unset, simple));
    assertEquals(List.of("7"), ids(catalog));
    assertEquals("Simple", catalog.getChoices().get(0).getName());
    assertTrue(FolderDisplayFormatCatalogRules.fromFormats(null).getChoices().isEmpty());
  }

  private static List<String> ids(PSFolderDisplayFormatCatalog catalog) {
    return catalog.getChoices().stream().map(PSFolderDisplayFormatChoice::getId).toList();
  }
}
