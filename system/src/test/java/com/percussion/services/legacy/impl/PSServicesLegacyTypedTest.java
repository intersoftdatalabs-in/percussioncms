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
package com.percussion.services.legacy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for typed {@code com.percussion.services.legacy} Hibernate
 * query helpers (issue #3210 residual of #2022 after publisher batch #3188).
 */
@Tag("UnitTest")
@DisplayName("services.legacy package generics")
class PSServicesLegacyTypedTest {

  @Test
  @DisplayName("findLocales HQL is order-only when lang and label are blank")
  void localeFindHqlNeitherFilter() {
    String hql = PSCmsObjectMgr.buildLocaleFindHql(null, "  ");
    assertEquals("from PSLocale locale order by locale.m_displayName asc", hql);
    assertFalse(hql.contains(":lang"));
    assertFalse(hql.contains(":label"));
  }

  @Test
  @DisplayName("findLocales HQL binds language only")
  void localeFindHqlLangOnly() {
    String hql = PSCmsObjectMgr.buildLocaleFindHql("en-us", null);
    assertTrue(hql.contains("where locale.m_languageString = :lang"));
    assertFalse(hql.contains(":label"));
    assertTrue(hql.endsWith("order by locale.m_displayName asc"));
  }

  @Test
  @DisplayName("findLocales HQL binds label only")
  void localeFindHqlLabelOnly() {
    String hql = PSCmsObjectMgr.buildLocaleFindHql("", "Test%");
    assertTrue(hql.contains("where locale.m_displayName like :label"));
    assertFalse(hql.contains(":lang"));
  }

  @Test
  @DisplayName("findLocales HQL binds language and label with AND")
  void localeFindHqlBothFilters() {
    String hql = PSCmsObjectMgr.buildLocaleFindHql("fr-ca", "Français%");
    assertTrue(hql.contains("where locale.m_languageString = :lang"));
    assertTrue(hql.contains("and locale.m_displayName like :label"));
    assertFalse(hql.contains("where locale.m_displayName"));
  }

  @Test
  @DisplayName("update date mutation HQL appends null guard when not overwriting")
  void updateDateHqlNullGuard() {
    String overwrite = PSCmsObjectMgr.formatUpdateDateHql("m_contentPostDate", true);
    String once = PSCmsObjectMgr.formatUpdateDateHql("m_contentPostDate", false);
    assertTrue(overwrite.contains("set cs.m_contentPostDate = :dateToSet"));
    assertTrue(overwrite.contains("cs.m_contentId in (:ids)"));
    assertFalse(overwrite.contains("is null"));
    assertTrue(once.startsWith(overwrite));
    assertTrue(once.endsWith(" and cs.m_contentPostDate is null"));
  }

  @Test
  @DisplayName("item-entry HQL selects summary columns and binds content id")
  void itemEntryHqlColumnsAndBind() {
    assertTrue(PSCmsObjectMgr.itemQuery.contains("c.m_contentId"));
    assertTrue(PSCmsObjectMgr.itemQuery.contains("c.m_name"));
    assertTrue(PSCmsObjectMgr.itemQuery.contains("from PSComponentSummary c"));
    assertEquals(
        PSCmsObjectMgr.itemQuery + " where c.m_contentId = :id",
        PSCmsObjectMgr.ITEM_ENTRY_BY_ID_HQL);
    assertFalse(PSCmsObjectMgr.ITEM_ENTRY_BY_ID_HQL.contains("where c.m_contentId="));
  }

  @Test
  @DisplayName("action menu relation SQL uses RXMENUACTIONRELATION columns")
  void actionMenuRelationSql() {
    assertEquals(
        "select ACTIONID, CHILDACTIONID from RXMENUACTIONRELATION",
        PSCmsObjectMgr.ACTION_MENU_RELATION_SQL);
  }

  @Test
  @DisplayName("action menu relation row normalizes Object[] and List pairs (#3379)")
  void actionMenuRelationPairFromNativeRow() {
    int[] fromArray = PSCmsObjectMgr.toActionMenuRelationPair(new Object[] {8, 17});
    assertEquals(8, fromArray[0]);
    assertEquals(17, fromArray[1]);
    int[] fromList =
        PSCmsObjectMgr.toActionMenuRelationPair(java.util.Arrays.asList(104, 118));
    assertEquals(104, fromList[0]);
    assertEquals(118, fromList[1]);
    org.junit.jupiter.api.Assertions.assertNull(PSCmsObjectMgr.toActionMenuRelationPair(null));
    org.junit.jupiter.api.Assertions.assertNull(
        PSCmsObjectMgr.toActionMenuRelationPair(new Object[] {1}));
    org.junit.jupiter.api.Assertions.assertNull(
        PSCmsObjectMgr.toActionMenuRelationPair(new Object[] {"8", 17}));
  }
}
