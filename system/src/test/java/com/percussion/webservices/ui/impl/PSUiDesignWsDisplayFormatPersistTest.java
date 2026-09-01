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
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * UI-05 persist: createDisplayFormats + saveDisplayFormats must INSERT a row
 * visible to findDisplayFormats (H2 REST POST 201 then GET-by-name 404 / By_Author
 * replay). {@code updateDisplayFormats} Dataset105 is INSERT only when HTML
 * {@code DISPLAYID IS NULL}; catalog GET inner-joins columns so sys_title is required.
 */
@Tag("UnitTest")
class PSUiDesignWsDisplayFormatPersistTest {

  @Test
  void prepareDisplayFormatForSave_newForcesInsert() throws Exception {
    PSDisplayFormat source = newDisplayFormat(1031, "QaDf4101");
    assertFalse(source.isPersisted());

    PSDisplayFormat prepared = PSUiDesignWs.prepareDisplayFormatForSave(source);

    assertNotSame(source, prepared);
    assertFalse(prepared.isPersisted());
    assertEquals(IPSDbComponent.DBSTATE_NEW, prepared.getState());
    assertEquals("QaDf4101", prepared.getName());
  }

  @Test
  void prepareDisplayFormatForSave_persistedKeepsLocator() throws Exception {
    PSDisplayFormat source = newDisplayFormat(42, "MyFmt");
    PSKey key = PSDisplayFormat.createKey(new String[] {"42"});
    source.setLocator(key);
    assertTrue(source.isPersisted());

    PSDisplayFormat prepared = PSUiDesignWs.prepareDisplayFormatForSave(source);

    assertTrue(prepared.isPersisted());
    assertEquals(42, prepared.getDisplayId());
  }

  @Test
  void displayFormatRowSpec_mapsAssignedNewFormatForInsert() throws Exception {
    PSDisplayFormat source = newDisplayFormat(1031, "QaDf4101");
    source.setDisplayName("QA DF label");
    source.setDescription("issue 4101 persist");

    PSUiDesignWs.DisplayFormatRowSpec spec =
        PSUiDesignWs.displayFormatRowSpec(PSUiDesignWs.prepareDisplayFormatForSave(source));

    assertEquals(1031, spec.displayId);
    assertEquals("QaDf4101", spec.internalName);
    assertEquals("QA DF label", spec.displayName);
    assertEquals("issue 4101 persist", spec.description);
    assertFalse(spec.columns.isEmpty());
    assertEquals("sys_title", spec.columns.get(0).source);
    assertTrue(
        spec.properties.stream()
            .anyMatch(
                p ->
                    PSDisplayFormat.PROP_COMMUNITY.equals(p.name)
                        && PSDisplayFormat.PROP_COMMUNITY_ALL.equals(p.value)));
  }

  @Test
  void displayFormatRowSpec_restrictCommunityDropsAllSentinel() throws Exception {
    PSDisplayFormat source = newDisplayFormat(1031, "QaDf4098");
    source.addCommunity("1001");

    PSUiDesignWs.DisplayFormatRowSpec spec = PSUiDesignWs.displayFormatRowSpec(source);

    assertTrue(
        spec.properties.stream()
            .anyMatch(p -> PSDisplayFormat.PROP_COMMUNITY.equals(p.name) && "1001".equals(p.value)));
    assertFalse(
        spec.properties.stream()
            .anyMatch(
                p ->
                    PSDisplayFormat.PROP_COMMUNITY.equals(p.name)
                        && PSDisplayFormat.PROP_COMMUNITY_ALL.equals(p.value)));
  }

  @Test
  void displayFormatRowSpec_includesAddedColumn() throws Exception {
    PSDisplayFormat source = newDisplayFormat(1031, "QaDf4101");
    PSDisplayColumn extra =
        new PSDisplayColumn(
            "sys_contentcreatedby",
            "Created by",
            PSDisplayColumn.GROUPING_FLAT,
            PSDisplayColumn.DATATYPE_TEXT,
            "",
            true);
    extra.setPosition(1);
    source.getColumnContainer().add(extra);

    PSUiDesignWs.DisplayFormatRowSpec spec = PSUiDesignWs.displayFormatRowSpec(source);

    assertTrue(spec.columns.stream().anyMatch(c -> "sys_title".equals(c.source)));
    assertTrue(spec.columns.stream().anyMatch(c -> "sys_contentcreatedby".equals(c.source)));
  }

  @Test
  void invalidateDisplayFormatCatalog_doesNotThrowWhenCacheUnavailable() {
    assertDoesNotThrow(PSUiDesignWs::invalidateDisplayFormatCatalog);
  }

  @Test
  void displayFormatLoadUsesUuidNotPackedLong() {
    // host 0 GUIDs: longValue == uuid; DISPLAYID HTML param must be the uuid
    // so getDisplayFormats (DISPLAYID IS NOT NULL) does not miss and replay By_Author.
    com.percussion.services.guidmgr.data.PSGuid guid =
        new com.percussion.services.guidmgr.data.PSGuid(
            com.percussion.services.catalog.PSTypeEnum.DISPLAY_FORMAT, 1031L);
    assertEquals(1031, guid.getUUID());
    assertEquals(1031L, guid.longValue());
  }

  @Test
  void insertDisplayFormatRow_isVisibleToSelectOnH2() throws Exception {
    String url = "jdbc:h2:mem:issue4101displayformats" + System.nanoTime();
    try (Connection conn = DriverManager.getConnection(url);
        Statement st = conn.createStatement()) {
      st.execute(
          "CREATE TABLE PSX_DISPLAYFORMATS ("
              + "DISPLAYID INTEGER NOT NULL PRIMARY KEY,"
              + "INTERNALNAME VARCHAR(255) NOT NULL,"
              + "DISPLAYNAME VARCHAR(255) NOT NULL,"
              + "DESCRIPTION VARCHAR(255),"
              + "VERSION INTEGER NOT NULL)");
      st.execute(
          "CREATE TABLE PSX_DISPLAYFORMATCOLUMNS ("
              + "DISPLAYID INTEGER NOT NULL,"
              + "SOURCE VARCHAR(50) NOT NULL,"
              + "DISPLAYNAME VARCHAR(255) NOT NULL,"
              + "TYPE INTEGER,"
              + "RENDERTYPE VARCHAR(50),"
              + "SORTORDER CHAR(1),"
              + "SEQUENCE INTEGER,"
              + "DESCRIPTION VARCHAR(255),"
              + "WIDTH INTEGER,"
              + "PRIMARY KEY (DISPLAYID, SOURCE))");
      st.execute(
          "CREATE TABLE PSX_DISPLAYFORMATPROPERTIES ("
              + "PROPERTYID INTEGER NOT NULL,"
              + "PROPERTYNAME VARCHAR(50) NOT NULL,"
              + "PROPERTYVALUE VARCHAR(100) NOT NULL,"
              + "DESCRIPTION VARCHAR(255),"
              + "PRIMARY KEY (PROPERTYID, PROPERTYNAME, PROPERTYVALUE))");
      PSDisplayFormat source = newDisplayFormat(2048, "QaH2Df");
      source.setDisplayName("QA H2 DF");
      PSUiDesignWs.DisplayFormatRowSpec spec =
          PSUiDesignWs.displayFormatRowSpec(PSUiDesignWs.prepareDisplayFormatForSave(source));
      assertFalse(PSUiDesignWs.displayFormatRowExists(conn, spec.displayId, spec.internalName));
      PSUiDesignWs.insertDisplayFormatRow(conn, spec);
      PSUiDesignWs.ensureDisplayFormatColumns(conn, spec);
      PSUiDesignWs.ensureDisplayFormatProperties(conn, spec);
      assertTrue(PSUiDesignWs.displayFormatRowExists(conn, spec.displayId, spec.internalName));
      assertTrue(PSUiDesignWs.displayFormatColumnExists(conn, spec.displayId, "sys_title"));
      PSUiDesignWs.ensureDisplayFormatColumns(conn, spec);
      assertTrue(PSUiDesignWs.displayFormatColumnExists(conn, spec.displayId, "sys_title"));
      PSUiDesignWs.DisplayFormatColumnSpec extra =
          new PSUiDesignWs.DisplayFormatColumnSpec(
              "sys_checkoutstatus",
              "Checkout status",
              0,
              "Text",
              "A",
              1,
              null,
              null);
      PSUiDesignWs.insertDisplayFormatColumn(conn, spec.displayId, extra);
      assertTrue(PSUiDesignWs.displayFormatColumnExists(conn, spec.displayId, "sys_checkoutstatus"));
      PSUiDesignWs.ensureDisplayFormatColumns(conn, spec);
      assertFalse(PSUiDesignWs.displayFormatColumnExists(conn, spec.displayId, "sys_checkoutstatus"));
      assertTrue(PSUiDesignWs.displayFormatColumnExists(conn, spec.displayId, "sys_title"));
      PSDisplayFormat loaded = PSUiDesignWs.loadDisplayFormatFromDb(conn, spec.displayId, spec.internalName);
      assertEquals("QaH2Df", loaded.getName());
      assertEquals(2048, loaded.getDisplayId());
      assertTrue(loaded.getColumnContainer().size() >= 1);
      assertTrue(
          loaded.doesPropertyHaveValue(
              PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));

      PSDisplayFormat restricted = newDisplayFormat(2048, "QaH2Df");
      restricted.setDisplayName("QA H2 DF");
      restricted.addCommunity("1001");
      PSUiDesignWs.DisplayFormatRowSpec restrictedSpec = PSUiDesignWs.displayFormatRowSpec(restricted);
      PSUiDesignWs.ensureDisplayFormatProperties(conn, restrictedSpec);
      assertTrue(
          PSUiDesignWs.displayFormatPropertyExists(
              conn, spec.displayId, PSDisplayFormat.PROP_COMMUNITY, "1001"));
      assertFalse(
          PSUiDesignWs.displayFormatPropertyExists(
              conn,
              spec.displayId,
              PSDisplayFormat.PROP_COMMUNITY,
              PSDisplayFormat.PROP_COMMUNITY_ALL));
      PSDisplayFormat loadedRestricted =
          PSUiDesignWs.loadDisplayFormatFromDb(conn, spec.displayId, spec.internalName);
      assertTrue(loadedRestricted.doesPropertyHaveValue(PSDisplayFormat.PROP_COMMUNITY, "1001"));
      assertFalse(
          loadedRestricted.doesPropertyHaveValue(
              PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));

      PSDisplayFormat allAgain = newDisplayFormat(2048, "QaH2Df");
      allAgain.setDisplayName("QA H2 DF");
      allAgain.addCommunity(null);
      PSUiDesignWs.ensureDisplayFormatProperties(conn, PSUiDesignWs.displayFormatRowSpec(allAgain));
      PSDisplayFormat loadedAll =
          PSUiDesignWs.loadDisplayFormatFromDb(conn, spec.displayId, spec.internalName);
      assertTrue(
          loadedAll.doesPropertyHaveValue(
              PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));
      assertFalse(loadedAll.doesPropertyHaveValue(PSDisplayFormat.PROP_COMMUNITY, "1001"));
    }
  }

  private static PSDisplayFormat newDisplayFormat(int displayId, String name) throws Exception {
    PSDisplayFormat source = new PSDisplayFormat();
    source.setInternalName(name);
    source.setDisplayName(name);
    PSKey key = PSDisplayFormat.createKey(new String[] {String.valueOf(displayId)});
    key.setPersisted(false);
    source.setLocator(key);
    return source;
  }
}
