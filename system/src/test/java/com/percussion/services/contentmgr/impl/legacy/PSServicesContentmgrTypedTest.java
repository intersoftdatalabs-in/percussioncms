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
package com.percussion.services.contentmgr.impl.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.cms.objectstore.PSNavNameAliases;
import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.services.contentmgr.impl.IPSTypeKey;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for typed {@code com.percussion.services.contentmgr} query residual
 * (issue #3210 residual of #2022 after publisher batch #3188).
 */
@Tag("UnitTest")
@DisplayName("services.contentmgr package generics")
class PSServicesContentmgrTypedTest {

  @AfterEach
  void clearNavAliasCache() {
    PSContentRepository.putTypeConfigurationForTest(PSNavNameAliases.PERC_NAV_TREE_TYPE_ID, null);
    PSContentRepository.clearNavAliasTypeIdCache();
  }

  @Test
  @DisplayName("asStringObjectMap copies keys as strings and skips null keys")
  void asStringObjectMapCopiesAndSkipsNullKeys() {
    Map<Object, Object> raw = new LinkedHashMap<>();
    raw.put("sys_contentid", 42);
    raw.put(IPSHtmlParameters.SYS_FOLDERID, 7);
    raw.put(null, "ignored");

    Map<String, Object> typed = PSContentRepository.asStringObjectMap(raw);
    assertEquals(42, typed.get("sys_contentid"));
    assertEquals(7, typed.get(IPSHtmlParameters.SYS_FOLDERID));
    assertEquals(2, typed.size());
    assertTrue(typed.containsKey(IPSHtmlParameters.SYS_FOLDERID));
  }

  @Test
  @DisplayName("asStringObjectMap returns empty map for null input")
  void asStringObjectMapNullSafe() {
    Map<String, Object> typed = PSContentRepository.asStringObjectMap(null);
    assertTrue(typed.isEmpty());
    typed.put("x", 1);
    assertNull(PSContentRepository.asStringObjectMap(null).get("x"));
  }

  @Test
  @DisplayName("asStringObjectMap does not alias the source map")
  void asStringObjectMapDefensiveCopy() {
    Map<Object, Object> raw = new HashMap<>();
    raw.put("k", "v");
    Map<String, Object> typed = PSContentRepository.asStringObjectMap(raw);
    typed.put("k", "changed");
    assertEquals("v", raw.get("k"));
  }

  @Test
  @DisplayName("remapFolderId moves sys_folderid to rx:sys_folderid")
  void remapFolderIdMovesLegacyKey() {
    Map<Object, Object> raw = new LinkedHashMap<>();
    raw.put(IPSHtmlParameters.SYS_FOLDERID, 99);
    raw.put("rx:sys_contentid", 5);

    Map<String, Object> remapped = PSContentRepository.remapFolderId(raw);
    assertNull(remapped.get(IPSHtmlParameters.SYS_FOLDERID));
    assertEquals(99, remapped.get(IPSContentPropertyConstants.RX_SYS_FOLDERID));
    assertEquals(5, remapped.get("rx:sys_contentid"));
  }

  @Test
  @DisplayName("remapFolderId leaves rows without folder id unchanged")
  void remapFolderIdNoFolder() {
    Map<Object, Object> raw = new HashMap<>();
    raw.put("rx:sys_contentid", 8);
    Map<String, Object> remapped = PSContentRepository.remapFolderId(raw);
    assertEquals(8, remapped.get("rx:sys_contentid"));
    assertNull(remapped.get(IPSContentPropertyConstants.RX_SYS_FOLDERID));
  }

  @Test
  @DisplayName("rffNavTree 315 aliases to percNavTree 1017 JCR mapping")
  void resolveNavAliasTypeIdMapsRffNavTreeToPerc() {
    Set<IPSTypeKey> registered =
        Set.of(new PSContentTypeKey(1017), new PSContentTypeKey(1001));
    Map<Long, String> names =
        Map.of(315L, "rffNavTree", 1017L, "percNavTree", 1001L, "percPage");
    assertEquals(
        1017L, PSContentRepository.resolveNavAliasTypeId(315L, registered, names::get));
    assertNull(PSContentRepository.resolveNavAliasTypeId(1001L, registered, names::get));
    assertNull(PSContentRepository.resolveNavAliasTypeId(315L, List.of(), names::get));
  }

  @Test
  @DisplayName("rffNavTree 315 aliases to percNavTree 1017 when ItemDef has no name for 315")
  void resolveNavAliasTypeIdMapsRffNavTreeWhenMissingNameUnknown() {
    Set<IPSTypeKey> registered =
        Set.of(new PSContentTypeKey(1017), new PSContentTypeKey(1001));
    assertEquals(
        1017L,
        PSContentRepository.resolveNavAliasTypeId(
            315L, registered, id -> id == 1017L ? "percNavTree" : null));
    assertEquals(
        1017L, PSContentRepository.resolveNavAliasTypeId(315L, registered, id -> null));
    assertNull(
        PSContentRepository.resolveNavAliasTypeId(
            315L, Set.of(new PSContentTypeKey(1001)), id -> null));
  }

  @Test
  @DisplayName("public getTypeConfiguration is exact-match only (no perc/rff write alias)")
  void publicGetTypeConfigurationDoesNotApplyNavAlias() {
    assertNull(PSContentRepository.lookupTypeConfigurationExact(315L));
    assertNull(PSContentRepository.getTypeConfiguration(315));
    assertEquals(
        1017L,
        PSContentRepository.resolveNavAliasTypeId(
            315L, Set.of(new PSContentTypeKey(1017)), id -> null));
  }

  @Test
  @DisplayName("nav alias type-id cache memos negative lookups")
  void lookupNavAliasTypeConfigurationCachesNegativeResult() {
    PSContentRepository.clearNavAliasTypeIdCache();
    assertFalse(PSContentRepository.navAliasTypeIdCacheContains(315L));
    assertNull(PSContentRepository.lookupNavAliasTypeConfiguration(315L));
    assertTrue(PSContentRepository.navAliasTypeIdCacheContains(315L));
    assertEquals(
        PSContentRepository.NO_NAV_ALIAS, PSContentRepository.navAliasCachedTypeId(315L));
    assertNull(PSContentRepository.lookupNavAliasTypeConfiguration(315L));
    PSContentRepository.clearNavAliasTypeIdCache();
    assertFalse(PSContentRepository.navAliasTypeIdCacheContains(315L));
  }

  @Test
  @DisplayName("nav alias type-id cache memos resolved perc/rff alias ids")
  void lookupNavAliasTypeConfigurationCachesPositiveAliasId() {
    PSTypeConfiguration first = mock(PSTypeConfiguration.class);
    PSTypeConfiguration second = mock(PSTypeConfiguration.class);
    PSContentRepository.putTypeConfigurationForTest(PSNavNameAliases.PERC_NAV_TREE_TYPE_ID, first);
    PSContentRepository.clearNavAliasTypeIdCache();
    assertFalse(
        PSContentRepository.navAliasTypeIdCacheContains(PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
    assertSame(
        first,
        PSContentRepository.lookupNavAliasTypeConfiguration(
            PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
    assertTrue(
        PSContentRepository.navAliasTypeIdCacheContains(PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
    assertEquals(
        PSNavNameAliases.PERC_NAV_TREE_TYPE_ID,
        PSContentRepository.navAliasCachedTypeId(PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
    PSContentRepository.putTypeConfigurationForTest(PSNavNameAliases.PERC_NAV_TREE_TYPE_ID, second);
    assertSame(
        second,
        PSContentRepository.lookupNavAliasTypeConfiguration(
            PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
    assertEquals(
        PSNavNameAliases.PERC_NAV_TREE_TYPE_ID,
        PSContentRepository.navAliasCachedTypeId(PSNavNameAliases.RFF_NAV_TREE_TYPE_ID));
  }

  @Test
  @DisplayName("putTypeConfigurationForTest is visible to exact lookup then removable")
  void putTypeConfigurationForTestRoundTrip() {
    PSTypeConfiguration stub = mock(PSTypeConfiguration.class);
    PSContentRepository.putTypeConfigurationForTest(
        PSNavNameAliases.PERC_NAV_TREE_TYPE_ID, stub);
    assertSame(
        stub,
        PSContentRepository.lookupTypeConfigurationExact(
            PSNavNameAliases.PERC_NAV_TREE_TYPE_ID));
    PSContentRepository.putTypeConfigurationForTest(
        PSNavNameAliases.PERC_NAV_TREE_TYPE_ID, null);
    assertNull(
        PSContentRepository.lookupTypeConfigurationExact(
            PSNavNameAliases.PERC_NAV_TREE_TYPE_ID));
  }
}
