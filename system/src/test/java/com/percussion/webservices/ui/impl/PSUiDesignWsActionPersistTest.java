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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.menus.RxmActionMenuConstants;
import com.percussion.webservices.PSErrorsException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
    assertNotSame(source, prepared);
  }

  @Test
  void prepareActionForSave_neverReturnsTheCallerInstance() {
    PSAction source = newAction(7, "NoMutate");
    PSAction prepared = PSUiDesignWs.prepareActionForSave(source);
    assertNotSame(source, prepared);
    assertEquals("NoMutate", source.getName());
    assertEquals(7, source.getId());
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
  void evictActionMenuRegion_doesNotThrowWhenFactoryMissing() {
    PSUiDesignWs ws = new PSUiDesignWs();
    assertDoesNotThrow(ws::evictActionMenuRegion);
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
      assertFalse(spec.restUserMenu);
      source.getProperties().setProperty(PSUiDesignWs.REST_USER_MENU_PROP, PSAction.YES);
      assertTrue(PSUiDesignWs.actionRowSpec(source).restUserMenu);
      PSUiDesignWs.ensureRestUserMenuProperty(conn, spec.actionId);
      assertTrue(PSUiDesignWs.actionRowExists(conn, spec.actionId, spec.name));
      assertTrue(PSUiDesignWs.actionRowExists(conn, spec.actionId, "otherName"));
      assertFalse(PSUiDesignWs.actionRowExists(conn, 9999, "QaH2Am"));

      PSAction sibling = newAction(2049, "Victim");
      sibling.setLabel("Victim label");
      PSUiDesignWs.insertActionRow(conn, PSUiDesignWs.actionRowSpec(sibling));

      PSAction updated = newAction(2048, "Victim");
      updated.setLabel("Updated label");
      updated.setDescription("updated");
      PSUiDesignWs.ActionRowSpec updatedSpec = PSUiDesignWs.actionRowSpec(updated);
      PSUiDesignWs.updateActionRow(conn, updatedSpec);
      assertTrue(PSUiDesignWs.actionRowExists(conn, 2048, "Victim"));
      try (var rs =
          conn.prepareStatement("SELECT DISPLAYNAME FROM RXMENUACTION WHERE ACTIONID = 2049")
              .executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Victim label", rs.getString(1));
      }

      try (var ins =
          conn.prepareStatement("INSERT INTO RXMENUACTIONPARAM (ACTIONID, PARAMNAME) VALUES (?, ?)")) {
        ins.setInt(1, spec.actionId);
        ins.setString(2, "sys_contentid");
        ins.executeUpdate();
      }
      PSUiDesignWs.deleteActionRow(conn, spec.actionId);
      assertFalse(PSUiDesignWs.actionRowExists(conn, spec.actionId, spec.name));
      assertTrue(PSUiDesignWs.actionRowExists(conn, 2049, "Victim"));
      try (var rs = conn.prepareStatement("SELECT COUNT(*) FROM RXMENUACTIONPARAM").executeQuery()) {
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
      }
    }
  }

  @Test
  void restUserMenuProperty_matchesSharedConstant() {
    assertEquals(RxmActionMenuConstants.REST_USER_MENU_PROP, PSUiDesignWs.REST_USER_MENU_PROP);
  }

  @Test
  void actionRowSpec_restUserMenuOnlyFromExplicitProperty() {
    PSAction unpersisted = newAction(2048, "QaAm");
    assertFalse(unpersisted.isPersisted());
    assertFalse(PSUiDesignWs.actionRowSpec(unpersisted).restUserMenu);

    PSAction prepared = PSUiDesignWs.prepareActionForSave(unpersisted);
    assertFalse(PSUiDesignWs.actionRowSpec(prepared).restUserMenu);

    unpersisted.getProperties().setProperty(PSUiDesignWs.REST_USER_MENU_PROP, PSAction.YES);
    assertTrue(PSUiDesignWs.actionRowSpec(unpersisted).restUserMenu);
  }

  @Test
  void persistActionRowOn_clearsRestUserMenuWhenPropertyAbsent() throws Exception {
    try (Connection conn = newActionSchema()) {
      PSAction source = newAction(2048, "QaH2Am");
      source.getProperties().setProperty(PSUiDesignWs.REST_USER_MENU_PROP, PSAction.YES);
      PSUiDesignWs.persistActionRowOn(conn, source);
      assertTrue(propertyExists(conn, 2048));

      PSAction soapUpdate = newAction(2048, "QaH2Am");
      PSKey key = PSAction.createKey("2048");
      soapUpdate.setLocator(key);
      assertTrue(soapUpdate.isPersisted());
      assertFalse(PSUiDesignWs.actionRowSpec(soapUpdate).restUserMenu);
      PSUiDesignWs.persistActionRowOn(conn, soapUpdate);
      assertFalse(propertyExists(conn, 2048));
    }
  }

  @Test
  void updateActionRowMatchingName_doesNotClobberDifferentName() throws Exception {
    try (Connection conn = newActionSchema()) {
      PSAction victim = newAction(2048, "Victim");
      victim.setLabel("Victim label");
      PSUiDesignWs.insertActionRow(conn, PSUiDesignWs.actionRowSpec(victim));

      PSAction attacker = newAction(2048, "OtherName");
      attacker.setLabel("Attacker");
      assertFalse(
          PSUiDesignWs.updateActionRowMatchingName(conn, PSUiDesignWs.actionRowSpec(attacker)));
      try (var rs =
          conn.prepareStatement("SELECT NAME, DISPLAYNAME FROM RXMENUACTION WHERE ACTIONID = 2048")
              .executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Victim", rs.getString(1));
        assertEquals("Victim label", rs.getString(2));
      }

      victim.setLabel("Victim updated");
      assertTrue(PSUiDesignWs.updateActionRowMatchingName(conn, PSUiDesignWs.actionRowSpec(victim)));
      try (var rs =
          conn.prepareStatement("SELECT DISPLAYNAME FROM RXMENUACTION WHERE ACTIONID = 2048")
              .executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Victim updated", rs.getString(1));
      }
    }
  }

  @Test
  void insertActionRow_duplicateActionIdIsPrimaryKeyViolation() throws Exception {
    try (Connection conn = newActionSchema()) {
      PSUiDesignWs.insertActionRow(conn, PSUiDesignWs.actionRowSpec(newAction(2048, "Victim")));
      SQLException thrown =
          assertThrows(
              SQLException.class,
              () ->
                  PSUiDesignWs.insertActionRow(
                      conn, PSUiDesignWs.actionRowSpec(newAction(2048, "OtherName"))));
      assertTrue(PSUiDesignWs.isPrimaryKeyViolation(thrown));
      assertFalse(
          PSUiDesignWs.updateActionRowMatchingName(
              conn, PSUiDesignWs.actionRowSpec(newAction(2048, "OtherName"))));
      try (var rs =
          conn.prepareStatement("SELECT NAME FROM RXMENUACTION WHERE ACTIONID = 2048")
              .executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Victim", rs.getString(1));
      }
    }
  }

  @Test
  void isXmlDocumentExpected_onlyMatchesMissingXmlDocument() {
    SQLException xml = new SQLException("Xml Document Expected, none supplied");
    PSErrorsException xmlWrapped = new PSErrorsException();
    xmlWrapped.addError(new PSGuid(PSTypeEnum.ACTION, 7), xml);
    assertTrue(PSUiDesignWs.isXmlDocumentExpected(xmlWrapped));
    assertTrue(PSUiDesignWs.isXmlDocumentExpected(xml));

    PSErrorsException dependency = new PSErrorsException();
    dependency.addError(new PSGuid(PSTypeEnum.ACTION, 8), new IllegalStateException("still referenced"));
    assertFalse(PSUiDesignWs.isXmlDocumentExpected(dependency));
    assertFalse(PSUiDesignWs.isXmlDocumentExpected(new IllegalStateException("boom")));
  }

  @Test
  void isPrimaryKeyViolation_requiresSqlState23505OrVendorIntegrity() {
    assertTrue(PSUiDesignWs.isPrimaryKeyViolation(new SQLException("dup", "23505")));
    assertFalse(PSUiDesignWs.isPrimaryKeyViolation(new SQLException("timeout", "HYT00")));
  }

  private static PSAction newAction(int actionId, String name) {
    PSAction source = new PSAction(name, name, PSAction.TYPE_MENU, "", PSAction.HANDLER_SERVER, 0);
    PSKey key = PSAction.createKey(String.valueOf(actionId));
    key.setPersisted(false);
    source.setLocator(key);
    return source;
  }

  private static Connection newActionSchema() throws SQLException {
    String url = "jdbc:h2:mem:issue4151actions" + System.nanoTime();
    Connection conn = DriverManager.getConnection(url);
    try (Statement st = conn.createStatement()) {
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
          "CREATE TABLE RXMENUACTIONPROPERTIES (ACTIONID INTEGER NOT NULL, PROPNAME VARCHAR(100) NOT NULL, PROPVALUE VARCHAR(4000), DESCRIPTION VARCHAR(255), PRIMARY KEY (ACTIONID, PROPNAME))");
    }
    return conn;
  }

  private static boolean propertyExists(Connection conn, int actionId) throws SQLException {
    try (var ps =
        conn.prepareStatement(
            "SELECT PROPNAME FROM RXMENUACTIONPROPERTIES WHERE ACTIONID = ? AND PROPNAME = ?")) {
      ps.setInt(1, actionId);
      ps.setString(2, PSUiDesignWs.REST_USER_MENU_PROP);
      try (var rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }
}
