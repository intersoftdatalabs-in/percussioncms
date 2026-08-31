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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSAction;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.data.ActionType;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-02 POST create / PUT update / DELETE persist via {@code createActions}/{@code saveActions}/{@code
 * deleteActions}. Admin only; unique name; no lock steal; system menus are 409.
 */
@Tag("UnitTest")
class ActionMenuAdaptorWriteTest {

  private IPSUiDesignWs designWs;
  private ActionMenuAdaptor adaptor;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor = new ActionMenuAdaptor(designWs, () -> true);
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of());
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/User/MyMenu");
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSAction action = stubAction("MyMenu", 42);
    when(designWs.createActions(
            eq(List.of("MyMenu")), anyList(), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(action));

    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    body.setLabel("My Menu");
    body.setDescription("created via REST");
    body.setMenuType("MENU");
    body.setUrl("");

    ActionMenu out = adaptor.createActionMenu(body);

    assertEquals("MyMenu", out.getName());
    assertEquals("My Menu", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    assertEquals(PSAction.TYPE_MENU, out.getMenuType());
    verify(designWs)
        .createActions(eq(List.of("MyMenu")), anyList(), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSAction>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveActions(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    PSAction persisted = saved.getValue().get(0);
    assertEquals("My Menu", persisted.getLabel());
    assertEquals("created via REST", persisted.getDescription());
    assertEquals(PSAction.TYPE_MENU, persisted.getMenuType());
  }

  @Test
  void create_blankTypeWithUrl_isDynamic() throws Exception {
    PSAction action = stubAction("DynMenu", 42);
    when(designWs.createActions(eq(List.of("DynMenu")), anyList(), any(), any()))
        .thenReturn(List.of(action));
    ActionMenu body = new ActionMenu();
    body.setName("DynMenu");
    body.setUrl("/sys_action");
    adaptor.createActionMenu(body);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ActionType>> types = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .createActions(eq(List.of("DynMenu")), types.capture(), eq("test-session"), eq("Admin"));
    assertEquals(ActionType.DYNAMIC, types.getValue().get(0));
    assertEquals("/sys_action", action.getURL());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("MyMenu");
    when(designWs.findActions(eq("MyMenu"), isNull(), isNull())).thenReturn(List.of(existing));

    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createActionMenu(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createActions(eq(List.of("MyMenu")), anyList(), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException("The name 'MyMenu' for type 'ACTION' already exists."));
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createActionMenu(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createActionMenu(null));
    ActionMenu blank = new ActionMenu();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createActionMenu(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    ActionMenu body = new ActionMenu();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createActionMenu(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
  }

  @Test
  void create_wildcardName_throwsBeforeDesignWs() {
    ActionMenu body = new ActionMenu();
    body.setName("My*Menu");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createActionMenu(body));
    assertEquals("name must not contain wildcards", ex.getMessage());
  }

  @Test
  void create_invalidType_throwsBeforeDesignWs() {
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    body.setMenuType("nope");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createActionMenu(body));
    assertTrue(ex.getMessage().toLowerCase().contains("menu type"));
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor = new ActionMenuAdaptor(designWs, () -> false);
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createActionMenu(body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createActionMenu(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void update_loadsWithLockNoStealAndSavesFields() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    body.setDescription("new desc");
    body.setMenuType("MENUITEM");
    body.setUrl("/u");

    ActionMenu out = adaptor.saveActionMenu("MyMenu", body);

    assertEquals("MyMenu", out.getName());
    assertEquals("Updated", locked.getLabel());
    assertEquals("new desc", locked.getDescription());
    assertEquals(PSAction.TYPE_MENUITEM, locked.getMenuType());
    assertEquals("/u", locked.getURL());
    verify(designWs).saveActions(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
    verify(designWs).loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_unknown_returnsNull() throws Exception {
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of());
    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    assertNull(adaptor.saveActionMenu("missing", body));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_system_is409() throws Exception {
    PSAction system = stubAction("Edit", 42);
    stubCatalogLoad(system);
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/System/Edit");
    ActionMenu body = new ActionMenu();
    body.setLabel("Edit");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveActionMenu("Edit", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("system"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void update_lockConflict_is409() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.loadActions(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveActionMenu("MyMenu", new ActionMenu()));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_nonAdmin_is403() {
    adaptor = new ActionMenuAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveActionMenu("MyMenu", new ActionMenu()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void delete_thenFindIsMissing() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(existing));

    assertTrue(adaptor.deleteActionMenu("MyMenu"));
    verify(designWs)
        .deleteActions(eq(List.of(existing.getGUID())), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_unknown_returnsFalse() throws Exception {
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of());
    assertFalse(adaptor.deleteActionMenu("missing"));
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_system_is409() throws Exception {
    PSAction system = stubAction("Edit", 42);
    stubCatalogLoad(system);
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/System/Edit");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("Edit"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.loadActions(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("MyMenu"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_dependents_is409() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.loadActions(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(existing));
    PSErrorsException errors = new PSErrorsException();
    errors.addError(existing.getGUID(), new PSErrorException("Object has dependents"));
    doThrow(errors).when(designWs).deleteActions(anyList(), eq(false), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("MyMenu"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
  }

  @Test
  void delete_nonAdmin_is403() {
    adaptor = new ActionMenuAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("MyMenu"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void requireValidName_rejectsSpacesAndWildcards() {
    assertEquals("MyMenu", ActionMenuAdaptor.requireValidName("MyMenu"));
    assertThrows(IllegalArgumentException.class, () -> ActionMenuAdaptor.requireValidName("  "));
    assertThrows(
        IllegalArgumentException.class, () -> ActionMenuAdaptor.requireValidName("has space"));
    assertThrows(IllegalArgumentException.class, () -> ActionMenuAdaptor.requireValidName("x*y"));
  }

  @Test
  void isSystemMenuPath_matchesSystemSegment() {
    assertTrue(ActionMenuAdaptor.isSystemMenuPath("//ContentExplorer/Menus/System/Edit"));
    assertTrue(ActionMenuAdaptor.isSystemMenuPath("Menus\\System\\Copy"));
    assertFalse(ActionMenuAdaptor.isSystemMenuPath("//ContentExplorer/Menus/User/MyMenu"));
    assertFalse(ActionMenuAdaptor.isSystemMenuPath(""));
    assertFalse(ActionMenuAdaptor.isSystemMenuPath(null));
  }

  @Test
  void resolveCreateType_defaultsAndAliases() {
    ActionMenu blank = new ActionMenu();
    assertEquals(ActionType.CASCADING, ActionMenuAdaptor.resolveCreateType(blank));
    ActionMenu withUrl = new ActionMenu();
    withUrl.setUrl("/x");
    assertEquals(ActionType.DYNAMIC, ActionMenuAdaptor.resolveCreateType(withUrl));
    ActionMenu item = new ActionMenu();
    item.setMenuType("MENUITEM");
    assertEquals(ActionType.ITEM, ActionMenuAdaptor.resolveCreateType(item));
    ActionMenu dyn = new ActionMenu();
    dyn.setMenuType("dynamic");
    assertEquals(ActionType.DYNAMIC, ActionMenuAdaptor.resolveCreateType(dyn));
    ActionMenu bad = new ActionMenu();
    bad.setMenuType("nope");
    assertThrows(IllegalArgumentException.class, () -> ActionMenuAdaptor.resolveCreateType(bad));
  }

  private PSAction stubAction(String name, int id) {
    PSAction action = new PSAction(name, name);
    action.setGUID(new PSGuid(PSTypeEnum.ACTION, id));
    return action;
  }

  private void stubCatalogLoad(PSAction action) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(action.getGUID());
    when(sum.getName()).thenReturn(action.getName());
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of(sum));
    when(designWs.loadActions(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(action));
  }
}
