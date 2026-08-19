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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PSNavNameAliasesTest {

  @Test
  void percAndRffTypeNamesAreTheSameRole() {
    assertTrue(PSNavNameAliases.sameNavRole("percNavTree", "rffNavTree"));
    assertTrue(PSNavNameAliases.sameNavRole("percNavon", "rffNavon"));
    assertTrue(PSNavNameAliases.sameNavRole("percNavImage", "rffNavImage"));
    assertTrue(PSNavNameAliases.sameNavRole("PERCNAVTREE", "rffnavtree"));
    assertFalse(PSNavNameAliases.sameNavRole("percNavTree", "percNavon"));
    assertFalse(PSNavNameAliases.sameNavRole("percNavTree", "percPage"));
    assertFalse(PSNavNameAliases.sameNavRole(null, "rffNavTree"));
  }

  @Test
  void percCeAppMatchesRffEditorUrl() {
    assertTrue(
        PSNavNameAliases.sameNavEditor(
            "psx_cepercNavTree", "../psx_cerffNavTree/rffNavTree.html"));
    assertTrue(
        PSNavNameAliases.sameNavEditor("psx_cepercNavon", "../psx_cerffNavon/rffNavon.html"));
    assertTrue(
        PSNavNameAliases.sameNavEditor(
            "psx_cerffNavTree", "../psx_cepercNavTree/percNavTree.html"));
    assertFalse(
        PSNavNameAliases.sameNavEditor("psx_cepercNavTree", "../psx_cepercPage/percPage.html"));
  }

  @Test
  void navTreeAndNavonTypeNameHelpers() {
    assertTrue(PSNavNameAliases.isNavTreeTypeName("rffNavTree"));
    assertTrue(PSNavNameAliases.isNavTreeTypeName("percNavTree"));
    assertFalse(PSNavNameAliases.isNavTreeTypeName("percNavon"));
    assertTrue(PSNavNameAliases.isNavonTypeName("rffNavon"));
    assertTrue(PSNavNameAliases.isNavonTypeName("percNavon"));
    assertFalse(PSNavNameAliases.isNavonTypeName("rffNavTree"));
    assertTrue(PSNavNameAliases.isNavImageTypeName("rffNavImage"));
    assertTrue(PSNavNameAliases.isNavImageTypeName("percNavImage"));
    assertTrue(PSNavNameAliases.isNavTypeName("rffNavTree"));
    assertFalse(PSNavNameAliases.isNavTypeName("percPage"));
    assertFalse(PSNavNameAliases.isNavTypeName(null));
  }

  @Test
  void findRegisteredNavAliasTypeIdMapsRffNavTreeToPercNavTree() {
    Map<Long, String> names =
        Map.of(
            315L, "rffNavTree",
            1017L, "percNavTree",
            314L, "rffNavon",
            1016L, "percNavon",
            1001L, "percPage");
    assertEquals(
        1017L,
        PSNavNameAliases.findRegisteredNavAliasTypeId(315L, names::get, names.keySet()));
    assertEquals(
        315L,
        PSNavNameAliases.findRegisteredNavAliasTypeId(1017L, names::get, names.keySet()));
    assertEquals(
        1016L,
        PSNavNameAliases.findRegisteredNavAliasTypeId(314L, names::get, names.keySet()));
    assertNull(
        PSNavNameAliases.findRegisteredNavAliasTypeId(1001L, names::get, names.keySet()));
    assertNull(
        PSNavNameAliases.findRegisteredNavAliasTypeId(315L, names::get, List.of(315L, 1001L)));
    assertNull(PSNavNameAliases.findRegisteredNavAliasTypeId(315L, names::get, List.of()));
    assertNull(PSNavNameAliases.findRegisteredNavAliasTypeId(315L, null, names.keySet()));
  }

  @Test
  void splitConfiguredNamesDropsEmptyTokens() {
    assertEquals(List.of("percNavon", "rffNavon"), PSNavNameAliases.splitConfiguredNames("percNavon,rffNavon"));
    assertEquals(List.of("perc.nav.slot", "rffNav"), PSNavNameAliases.splitConfiguredNames("perc.nav.slot; rffNav"));
    assertTrue(PSNavNameAliases.splitConfiguredNames("").isEmpty());
    assertTrue(PSNavNameAliases.splitConfiguredNames(null).isEmpty());
  }
}
