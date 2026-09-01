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
import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * UI-02 persist: createActions + saveActions must INSERT a row visible to
 * Hibernate {@code RXMENUACTION} (H2 REST POST 200 then GET catalog miss).
 */
@Tag("UnitTest")
class PSUiDesignWsActionPersistTest {

  @Test
  void prepareActionForSave_newForcesInsert() {
    PSAction source = newAction(2048, "QaAm4119");
    assertFalse(source.isPersisted());

    PSAction prepared = PSUiDesignWs.prepareActionForSave(source);

    assertNotSame(source, prepared);
    assertFalse(prepared.isPersisted());
    assertEquals(IPSDbComponent.DBSTATE_NEW, prepared.getState());
    assertEquals("QaAm4119", prepared.getName());
  }

  @Test
  void prepareActionForSave_persistedKeepsLocator() {
    PSAction source = newAction(42, "MyMenu");
    PSKey key = PSAction.createKey("42");
    source.setLocator(key);
    assertTrue(source.isPersisted());

    PSAction prepared = PSUiDesignWs.prepareActionForSave(source);

    assertTrue(prepared.isPersisted());
    assertEquals(42, prepared.getId());
  }

  @Test
  void actionRowSpec_mapsAssignedNewMenuForInsert() {
    PSAction source = newAction(2048, "QaAm4119");
    source.setLabel("QA AM label");
    source.setDescription("issue 4119 persist");
    source.setMenuType(PSAction.TYPE_MENU);
    source.setClientAction(false);

    PSUiDesignWs.ActionRowSpec spec =
        PSUiDesignWs.actionRowSpec(PSUiDesignWs.prepareActionForSave(source));

    assertEquals(2048, spec.actionId);
    assertEquals("QaAm4119", spec.name);
    assertEquals("QA AM label", spec.displayName);
    assertEquals("issue 4119 persist", spec.description);
    assertEquals(PSAction.TYPE_MENU, spec.type);
    assertEquals(PSAction.HANDLER_SERVER, spec.handler);
  }

  @Test
  void invalidateActionCatalog_doesNotThrowWhenCacheUnavailable() {
    assertDoesNotThrow(PSUiDesignWs::invalidateActionCatalog);
  }

  @Test
  void insertActionRow_isVisibleToSelectAndDeleteOnH2() throws Exception {
    String url = "jdbc:h2:mem:issue4119actions" + System.nanoTime();
    try (Connection conn = DriverManager.getConnection(url);
        Statement st = conn.createStatement()) {
      st.execute(
          "CREATE TABLE RXMENUACTION ("
              + "ACTIONID INTEGER NOT NULL PRIMARY KEY,"
              + "NAME VARCHAR(50) NOT NULL,"
              + "DISPLAYNAME VARCHAR(50) NOT NULL,"
              + "DESCRIPTION VARCHAR(255),"
              + "URL VARCHAR(4000),"
              + "SORTORDER INTEGER,"
              + "TYPE VARCHAR(50),"
              + "HANDLER VARCHAR(50),"
              + "VERSION INTEGER NOT NULL)");
      st.execute(
          "CREATE TABLE RXMENUACTIONPARAM (ACTIONID INTEGER NOT NULL, PARAMNAME VARCHAR(50) NOT NULL)");
      st.execute(
          "CREATE TABLE RXMENUACTIONPROPERTIES (ACTIONID INTEGER NOT NULL, PROPNAME VARCHAR(100) NOT NULL, PROPVALUE VARCHAR(4000), DESCRIPTION VARCHAR(255), PRIMARY KEY (ACTIONID, PROPNAME))");
      st.execute(
          "CREATE TABLE RXMENUVISIBILITY (ACTIONID INTEGER NOT NULL, VISIBILITYCONTEXT VARCHAR(50) NOT NULL, \"VALUE\" VARCHAR(100) NOT NULL)");
      st.execute(
          "CREATE TABLE RXMODEUICONTEXTACTION (MODEID INTEGER NOT NULL, UICONTEXTID INTEGER NOT NULL, ACTIONID INTEGER NOT NULL)");
      st.execute(
          "CREATE TABLE RXMENUACTIONRELATION (ACTIONID INTEGER NOT NULL, CHILDACTIONID INTEGER NOT NULL)");

      PSAction source = newAction(2048, "QaH2Am");
      source.setLabel("QA H2 AM");
      source.setDescription("persist");
      PSUiDesignWs.ActionRowSpec spec =
          PSUiDesignWs.actionRowSpec(PSUiDesignWs.prepareActionForSave(source));
      assertFalse(PSUiDesignWs.actionRowExists(conn, spec.actionId, spec.name));
      PSUiDesignWs.insertActionRow(conn, spec);
      assertTrue(spec.restUserMenu);
      PSUiDesignWs.ensureRestUserMenuProperty(conn, spec.actionId);
      assertTrue(PSUiDesignWs.actionRowExists(conn, spec.actionId, spec.name));
      assertTrue(PSUiDesignWs.actionRowExists(conn, spec.actionId, "otherName"));

      PSAction updated = newAction(2048, "QaH2Am");
      updated.setLabel("Updated label");
      updated.setDescription("updated");
      PSUiDesignWs.ActionRowSpec updatedSpec = PSUiDesignWs.actionRowSpec(updated);
      PSUiDesignWs.updateActionRow(conn, updatedSpec);
      assertTrue(PSUiDesignWs.actionRowExists(conn, 2048, "QaH2Am"));

      PSUiDesignWs.deleteActionRow(conn, spec.actionId);
      assertFalse(PSUiDesignWs.actionRowExists(conn, spec.actionId, spec.name));
    }
  }

  private static PSAction newAction(int actionId, String name) {
    PSAction source = new PSAction(name, name, PSAction.TYPE_MENU, "", PSAction.HANDLER_SERVER, 0);
    PSKey key = PSAction.createKey(String.valueOf(actionId));
    key.setPersisted(false);
    source.setLocator(key);
    return source;
  }
}
