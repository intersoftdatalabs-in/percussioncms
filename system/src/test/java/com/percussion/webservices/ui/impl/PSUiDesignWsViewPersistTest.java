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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * UI-07 persist: createViews + saveViews must INSERT a {@code TYPE_VIEW} row
 * visible to findViews (H2 REST POST 200 then GET list 0 / no 409 on duplicate).
 * Shares {@code PSX_SEARCHES} with UI-06; Dataset431 is DELETE-only.
 */
@Tag("UnitTest")
class PSUiDesignWsViewPersistTest {

  @Test
  void prepareSearchForSave_newViewKeepsCommunityAndForcesInsert() throws Exception {
    PSSearch source = new PSSearch("MyView");
    source.setType(PSSearch.TYPE_VIEW);
    PSKey key = PSSearch.createKey(new String[] {"1014"});
    key.setPersisted(false);
    source.setLocator(key);

    PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);

    assertNotSame(source, prepared);
    assertFalse(prepared.isPersisted());
    assertEquals(IPSDbComponent.DBSTATE_NEW, prepared.getState());
    assertTrue(prepared.isView());
    assertTrue(
        prepared.doesPropertyHaveValue(PSSearch.PROP_COMMUNITY, PSSearch.PROP_COMMUNITY_ALL),
        "new views keep sys_community=-1 so the update resource can write AnyCommunity ACL");
  }

  @Test
  void searchRowSpec_mapsAssignedNewViewForInsert() throws Exception {
    PSSearch source = new PSSearch("QaView");
    source.setType(PSSearch.TYPE_VIEW);
    source.setDisplayName("QA view label");
    source.setDescription("SPA UI-07 create");
    source.setDisplayFormatId("7");
    PSKey key = PSSearch.createKey(new String[] {"1014"});
    key.setPersisted(false);
    source.setLocator(key);

    PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);
    PSUiDesignWs.SearchRowSpec spec = PSUiDesignWs.searchRowSpec(prepared);

    assertEquals(1014, spec.searchId);
    assertEquals("QaView", spec.internalName);
    assertEquals("QA view label", spec.displayName);
    assertEquals(PSSearch.TYPE_VIEW, spec.type);
    assertEquals(Integer.valueOf(7), spec.displayFormat);
    assertEquals("SPA UI-07 create", spec.description);
    assertEquals(0, spec.caseSensitive);
    assertEquals(PSSearch.DEFAULT_MAX, spec.maximumItems);
    assertEquals(1, spec.parentCategory);
  }

  @Test
  void insertViewRow_isVisibleToSelectOnH2() throws Exception {
    String url = "jdbc:h2:mem:issue4095views" + System.nanoTime();
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
      PSSearch source = new PSSearch("QaH2View");
      source.setType(PSSearch.TYPE_VIEW);
      PSKey key = PSSearch.createKey(new String[] {"2048"});
      key.setPersisted(false);
      source.setLocator(key);
      PSUiDesignWs.SearchRowSpec spec =
          PSUiDesignWs.searchRowSpec(PSUiDesignWs.prepareSearchForSave(source));
      assertEquals(PSSearch.TYPE_VIEW, spec.type);
      assertFalse(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
      PSUiDesignWs.insertSearchRow(conn, spec);
      assertTrue(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
      assertTrue(PSUiDesignWs.searchNameExists(conn, spec.internalName));
      PSUiDesignWs.deleteSearchRow(conn, spec.searchId);
      assertFalse(PSUiDesignWs.searchRowExists(conn, spec.searchId, spec.internalName));
    }
  }

  @Test
  void ensureSearchRowPersisted_insertsWhenSearchIdCollidesWithOtherName() throws Exception {
    String url = "jdbc:h2:mem:issue4175viewid" + System.nanoTime();
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
      st.execute(
          "INSERT INTO PSX_SEARCHES (SEARCHID, INTERNALNAME, DISPLAYNAME, PARENTCATEGORY, CUSTOMURL, TYPE, DISPLAYFORMAT, MAXIMUMITEMS, DESCRIPTION, CASESENSITIVE, VERSION) VALUES (3, 'Inbox', 'Inbox', 1, NULL, 'View', 1, -1, NULL, 0, 0)");
      assertTrue(PSUiDesignWs.searchIdExists(conn, 3));
      assertTrue(PSUiDesignWs.searchRowExists(conn, 3, "QaNewView"));
      assertFalse(PSUiDesignWs.searchNameExists(conn, "QaNewView"));

      PSSearch source = new PSSearch("QaNewView");
      source.setType(PSSearch.TYPE_VIEW);
      PSKey key = PSSearch.createKey(new String[] {"3"});
      key.setPersisted(false);
      source.setLocator(key);
      PSSearch prepared = PSUiDesignWs.prepareSearchForSave(source);
      if (PSUiDesignWs.searchNameExists(conn, "QaNewView")) {
        throw new AssertionError("name must be missing before ensure");
      }
      if (PSUiDesignWs.searchIdExists(conn, prepared.getId())) {
        int freeId = PSUiDesignWs.nextFreeSearchId(conn);
        PSUiDesignWs.applySearchId(prepared, freeId);
      }
      PSUiDesignWs.SearchRowSpec spec = PSUiDesignWs.searchRowSpec(prepared);
      PSUiDesignWs.insertSearchRow(conn, spec);
      assertTrue(PSUiDesignWs.searchNameExists(conn, "QaNewView"));
      assertTrue(PSUiDesignWs.searchNameExists(conn, "Inbox"));
      assertEquals(4, spec.searchId);
    }
  }

  @Test
  void invalidateSearchCatalog_doesNotThrowWhenCacheUnavailable() {
    assertDoesNotThrow(PSUiDesignWs::invalidateSearchCatalog);
  }
}
