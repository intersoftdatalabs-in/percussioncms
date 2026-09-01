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

package com.percussion.webservices.ui.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSSearch;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * UI-06 persist: createSearches + saveSearches must INSERT and stay visible to findSearches
 * (H2 REST POST 200 then GET 404 / no 409 on duplicate).
 */
@Tag("UnitTest")
class PSUiDesignWsSearchPersistTest {

  @Test
  void prepareSearchForSave_newKeepsCommunityAndForcesInsert() throws Exception {
    PSSearch source = new PSSearch("MySearch");
    source.setType(PSSearch.TYPE_STANDARDSEARCH);
    PSKey key = PSSearch.createKey(new String[] {"1014"});
    key.setPersisted(false);
    source.setLocator(key);

    PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);

    assertNotSame(source, prepared);
    assertFalse(prepared.isPersisted());
    assertEquals(IPSDbComponent.DBSTATE_NEW, prepared.getState());
    assertTrue(
        prepared.doesPropertyHaveValue(PSSearch.PROP_COMMUNITY, PSSearch.PROP_COMMUNITY_ALL),
        "new searches keep sys_community=-1 so the update resource can write AnyCommunity ACL");
  }

  @Test
  void prepareSearchForSave_persistedStripsCommunity() throws Exception {
    PSSearch source = new PSSearch("MySearch");
    source.setType(PSSearch.TYPE_STANDARDSEARCH);
    PSKey key = PSSearch.createKey(new String[] {"42"});
    source.setLocator(key);
    assertTrue(source.isPersisted());
    assertTrue(source.doesPropertyHaveValue(PSSearch.PROP_COMMUNITY, PSSearch.PROP_COMMUNITY_ALL));

    PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);

    assertTrue(prepared.isPersisted());
    assertFalse(
        prepared.doesPropertyHaveValue(PSSearch.PROP_COMMUNITY, PSSearch.PROP_COMMUNITY_ALL),
        "updates still strip sys_community (Workbench already processed ACLs)");
  }

  @Test
  void invalidateSearchCatalog_doesNotThrowWhenCacheUnavailable() {
    assertDoesNotThrow(PSUiDesignWs::invalidateSearchCatalog);
  }

  @Test
  void searchRowSpec_mapsAssignedNewSearchForInsert() throws Exception {
    PSSearch source = new PSSearch("QaSearch");
    source.setType(PSSearch.TYPE_STANDARDSEARCH);
    source.setDisplayName("QA label");
    source.setDescription("SPA UI-06 create");
    source.setDisplayFormatId("7");
    PSKey key = PSSearch.createKey(new String[] {"1014"});
    key.setPersisted(false);
    source.setLocator(key);

    PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);
    PSUiDesignWs.SearchRowSpec spec = PSUiDesignWs.searchRowSpec(prepared);

    assertEquals(1014, spec.searchId);
    assertEquals("QaSearch", spec.internalName);
    assertEquals("QA label", spec.displayName);
    assertEquals(PSSearch.TYPE_STANDARDSEARCH, spec.type);
    assertEquals(Integer.valueOf(7), spec.displayFormat);
    assertEquals("SPA UI-06 create", spec.description);
    assertEquals(0, spec.caseSensitive);
    assertEquals(PSSearch.DEFAULT_MAX, spec.maximumItems);
    assertEquals(1, spec.parentCategory);
  }

  @Test
  void parseDisplayFormatId_blankAndInvalidDefaultToOne() {
    assertEquals(Integer.valueOf(1), PSUiDesignWs.parseDisplayFormatId(null));
    assertEquals(Integer.valueOf(1), PSUiDesignWs.parseDisplayFormatId(""));
    assertEquals(Integer.valueOf(1), PSUiDesignWs.parseDisplayFormatId("nope"));
    assertEquals(Integer.valueOf(0), PSUiDesignWs.parseDisplayFormatId("0"));
  }

  @Test
  void insertSearchRow_isVisibleToSelectOnH2() throws Exception {
    String url = "jdbc:h2:mem:issue4084searches" + System.nanoTime();
    try (Connection conn = DriverManager.getConnection(url);
        Statement st = conn.createStatement()) {
      st.execute(
          "CREATE TABLE PSX_SEARCHES ("
              + "SEARCHID INTEGER NOT NULL PRIMARY KEY,"
              + "INTERNALNAME VARCHAR(255) NOT NULL,"
              + "DISPLAYNAME VARCHAR(255) NOT NULL,"
              + "PARENTCATEGORY INTEGER NOT NULL,"
              + "CUSTOMURL VARCHAR(255),"
              + "TYPE VARCHAR(50),"
              + "DISPLAYFORMAT INTEGER,"
              + "MAXIMUMITEMS INTEGER NOT NULL,"
              + "DESCRIPTION VARCHAR(255),"
              + "CASESENSITIVE INTEGER,"
              + "VERSION INTEGER NOT NULL)");
      st.execute(
          "CREATE TABLE PSX_SEARCHFIELDS (SEARCHID INTEGER NOT NULL, FIELDNAME VARCHAR(50) NOT NULL)");
      st.execute(
          "CREATE TABLE PSX_SEARCHPROPERTIES (PROPERTYID INTEGER NOT NULL, PROPERTYNAME VARCHAR(50) NOT NULL, PROPERTYVALUE VARCHAR(255))");
      PSSearch source = new PSSearch("QaH2Search");
      source.setType(PSSearch.TYPE_STANDARDSEARCH);
      PSKey key = PSSearch.createKey(new String[] {"2048"});
      key.setPersisted(false);
      source.setLocator(key);
      PSUiDesignWs.SearchRowSpec spec =
          PSUiDesignWs.searchRowSpec(PSUiDesignWs.prepareSearchForSave(source));
      assertFalse(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
      PSUiDesignWs.insertSearchRow(conn, spec);
      assertTrue(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
      assertTrue(PSUiDesignWs.searchRowExists(conn, spec.searchId, "otherName"));
      PSUiDesignWs.deleteSearchRow(conn, spec.searchId);
      assertFalse(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
    }
  }

  @Test
  void matchSearchByGuid_matchesAssignedId() throws Exception {
    PSSearch source = new PSSearch("MatchMe");
    source.setType(PSSearch.TYPE_STANDARDSEARCH);
    PSKey key = PSSearch.createKey(new String[] {"77"});
    key.setPersisted(false);
    source.setLocator(key);
    assertEquals(source, PSUiDesignWs.matchSearchByGuid(List.of(source), source.getGUID()));
    assertTrue(PSUiDesignWs.matchSearchesByGuids(List.of(source), List.of(source.getGUID())).size() == 1);
  }
}
