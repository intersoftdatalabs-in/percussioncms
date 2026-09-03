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
import com.percussion.rest.actions.ActionMenuList;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.RxmActionMenuConstants;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.data.ActionType;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    ActionMenuAdaptor.clearRequestHibernateIndex();
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
  void update_pathResolutionFailure_is409FailClosed() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.objectIdToPath(any())).thenThrow(new PSErrorsException());
    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveActionMenu("MyMenu", body));
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
  void updateChildren_loadsWithLockNoStealAndSavesOrder() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    PSAction childA = stubAction("ChildA", 100);
    PSAction childB = stubAction("ChildB", 101);
    stubCatalogLoad(parent, childA, childB);
    PSAction locked = stubCascadingMenu("MyMenu", 42);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ActionMenuList children = new ActionMenuList();
    ActionMenu first = new ActionMenu();
    first.setName("ChildB");
    ActionMenu second = new ActionMenu();
    second.setId(100);
    children.add(first);
    children.add(second);

    ActionMenu out = adaptor.saveActionMenuChildren("MyMenu", children);

    assertEquals("MyMenu", out.getName());
    assertEquals(2, out.getChildren().size());
    assertEquals("ChildB", out.getChildren().get(0).getName());
    assertEquals("ChildA", out.getChildren().get(1).getName());
    assertEquals(2, childCount(locked));
    assertEquals(1, childB.getSortRank());
    assertEquals(2, childA.getSortRank());
    verify(designWs).saveActions(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs).loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void updateChildren_unknownParent_returnsNull() throws Exception {
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(List.of());
    assertNull(adaptor.saveActionMenuChildren("missing", new ActionMenuList()));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateChildren_systemParent_is409() throws Exception {
    PSAction system = stubAction("Edit", 42);
    stubCatalogLoad(system);
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/System/Edit");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.saveActionMenuChildren("Edit", new ActionMenuList()));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("system"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateChildren_nonAdmin_is403() {
    adaptor = new ActionMenuAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.saveActionMenuChildren("MyMenu", new ActionMenuList()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void updateChildren_unknownChild_is400() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    stubCatalogLoad(parent);
    ActionMenuList children = new ActionMenuList();
    ActionMenu missing = new ActionMenu();
    missing.setName("NoSuchChild");
    children.add(missing);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.saveActionMenuChildren("MyMenu", children));
    assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_partialUnknownChild_doesNotPersist() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    PSAction childA = stubAction("ChildA", 100);
    stubCatalogLoad(parent, childA);
    ActionMenuList children = new ActionMenuList();
    ActionMenu first = new ActionMenu();
    first.setName("ChildA");
    ActionMenu missing = new ActionMenu();
    missing.setName("NoSuchChild");
    children.add(first);
    children.add(missing);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.saveActionMenuChildren("MyMenu", children));
    assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_duplicateChild_is400() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    PSAction childA = stubAction("ChildA", 100);
    stubCatalogLoad(parent, childA);
    ActionMenuList children = new ActionMenuList();
    ActionMenu first = new ActionMenu();
    first.setName("ChildA");
    ActionMenu second = new ActionMenu();
    second.setId(100);
    children.add(first);
    children.add(second);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.saveActionMenuChildren("MyMenu", children));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_parentAsOwnChild_is400() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    stubCatalogLoad(parent);
    ActionMenuList children = new ActionMenuList();
    ActionMenu self = new ActionMenu();
    self.setName("MyMenu");
    children.add(self);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.saveActionMenuChildren("MyMenu", children));
    assertTrue(ex.getMessage().toLowerCase().contains("cycle"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_transitiveCycle_is400() throws Exception {
    PSAction parent = stubCascadingMenu("MenuA", 42);
    PSAction childB = stubCascadingMenu("MenuB", 100);
    childB.getChildren().add(parent);
    stubCatalogLoad(parent, childB);
    ActionMenuList children = new ActionMenuList();
    ActionMenu b = new ActionMenu();
    b.setName("MenuB");
    children.add(b);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.saveActionMenuChildren("MenuA", children));
    assertTrue(ex.getMessage().toLowerCase().contains("cycle"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_menuItemParent_is400() throws Exception {
    PSAction parent = stubAction("MyItem", 42);
    parent.setMenuType(PSAction.TYPE_MENUITEM);
    stubCatalogLoad(parent);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.saveActionMenuChildren("MyItem", new ActionMenuList()));
    assertTrue(ex.getMessage().toLowerCase().contains("cascading"));
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void updateChildren_emptyClearsAssociations() throws Exception {
    PSAction parent = stubCascadingMenu("MyMenu", 42);
    stubCatalogLoad(parent);
    PSAction locked = stubCascadingMenu("MyMenu", 42);
    locked.getChildren().add(stubAction("OldChild", 99));
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ActionMenu out = adaptor.saveActionMenuChildren("MyMenu", new ActionMenuList());

    assertEquals("MyMenu", out.getName());
    assertTrue(out.getChildren() == null || out.getChildren().isEmpty());
    assertEquals(0, childCount(locked));
    verify(designWs).saveActions(anyList(), eq(true), eq("test-session"), eq("Admin"));
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
  void delete_system_whenDesignWsMissesHibernateRow_is409() throws Exception {
    PSActionMenu edit = new PSActionMenu("Edit", "Edit", PSAction.TYPE_MENUITEM, "", "SERVER", 0);
    edit.setActionId(7);
    adaptor = new ActionMenuAdaptor(designWs, () -> true, () -> List.of(edit));
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/System/Edit");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("Edit"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void delete_packagedCopy_whenDesignWsMissesHibernateRow_is409() throws Exception {
    PSActionMenu copy = new PSActionMenu("Copy", "Copy", PSAction.TYPE_MENUITEM, "", "SERVER", 0);
    copy.setActionId(11);
    adaptor = new ActionMenuAdaptor(designWs, () -> true, () -> List.of(copy));
    when(designWs.objectIdToPath(any())).thenReturn("//ContentExplorer/Menus/System/Copy");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("Copy"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadActions(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void delete_restUser_whenDesignWsLoadMisses_stillDeletes() throws Exception {
    PSActionMenu user = new PSActionMenu("QaMenu", "QaMenu", PSAction.TYPE_MENU, "", "SERVER", 0);
    user.setActionId(42);
    user.addProperty(
        new com.percussion.services.menus.PSActionMenuProperty(
            42, ActionMenuAdaptor.REST_USER_MENU_PROP, PSAction.YES));
    adaptor = new ActionMenuAdaptor(designWs, () -> true, () -> List.of(user));
    when(designWs.loadActions(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of());

    assertTrue(adaptor.deleteActionMenu("QaMenu"));
    verify(designWs).deleteActions(anyList(), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void create_duplicateName_fromHibernateCatalog_is409() {
    PSActionMenu existing = new PSActionMenu("MyMenu", "My Menu", PSAction.TYPE_MENU, "", "SERVER", 0);
    existing.setActionId(9);
    adaptor = new ActionMenuAdaptor(designWs, () -> true, () -> List.of(existing));
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createActionMenu(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).createActions(anyList(), anyList(), any(), any());
  }

  @Test
  void matchMenuInTree_findsNestedChild() {
    ActionMenu root = new ActionMenu();
    root.setName("File");
    root.setId(1);
    ActionMenu child = new ActionMenu();
    child.setName("MyMenu");
    child.setId(42);
    ActionMenuList kids = new ActionMenuList();
    kids.add(child);
    root.setChildren(kids);
    assertEquals("MyMenu", ActionMenuAdaptor.matchMenuInTree(List.of(root), "MyMenu").getName());
    assertEquals(42, ActionMenuAdaptor.matchMenuInTree(List.of(root), "42").getId());
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
  void delete_pathResolutionFailure_is409FailClosed() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    when(designWs.objectIdToPath(any()))
        .thenThrow(new RuntimeException("path lookup failed"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("MyMenu"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("system"));
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
    assertTrue(ActionMenuAdaptor.isUserMenuPath("//ContentExplorer/Menus/User/MyMenu"));
    assertFalse(ActionMenuAdaptor.isUserMenuPath("//ContentExplorer/Menus/System/Edit"));
  }

  @Test
  void delete_blankPathWithoutRestMarker_is409() throws Exception {
    PSAction existing = stubAction("Edit", 101);
    stubCatalogLoad(existing);
    when(designWs.objectIdToPath(any())).thenReturn("");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteActionMenu("Edit"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_blankPathWithRestMarker_succeeds() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    existing.getProperties().setProperty(ActionMenuAdaptor.REST_USER_MENU_PROP, PSAction.YES);
    stubCatalogLoad(existing);
    when(designWs.objectIdToPath(any())).thenReturn("");
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(existing));
    assertTrue(adaptor.deleteActionMenu("MyMenu"));
    verify(designWs)
        .deleteActions(eq(List.of(existing.getGUID())), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void isSystemMenu_nullGuid_isFailClosed() {
    PSAction action = new PSAction("NoGuid", "NoGuid");
    assertTrue(adaptor.isSystemMenu(action));
  }

  @Test
  void create_catalogPsErrorsException_is500() throws Exception {
    when(designWs.findActions(eq("DupMenu"), isNull(), isNull()))
        .thenThrow(new PSErrorsException());
    ActionMenu body = new ActionMenu();
    body.setName("DupMenu");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createActionMenu(body));
    assertTrue(ex.getMessage().toLowerCase().contains("catalog"));
  }

  @Test
  void create_catalogRuntimeFailure_is500() throws Exception {
    when(designWs.findActions(eq("DupMenu"), isNull(), isNull()))
        .thenThrow(new IllegalStateException("catalog exploded"));
    ActionMenu body = new ActionMenu();
    body.setName("DupMenu");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createActionMenu(body));
    assertTrue(ex.getMessage().toLowerCase().contains("catalog"));
  }

  @Test
  void indexHibernateMenus_indexesNameAndChildId() {
    PSActionMenu child = new PSActionMenu("Child", "c", "MENUITEM", "", "server", 0);
    child.setActionId(8);
    PSActionMenu root = new PSActionMenu("Root", "r", "MENU", "", "server", 0);
    root.setActionId(7);
    root.setChildren(List.of(child));
    Map<String, PSActionMenu> index = ActionMenuAdaptor.indexHibernateMenus(List.of(root));
    assertEquals(root, index.get("root"));
    assertEquals(child, index.get("child"));
    assertEquals(root, index.get("7"));
    assertEquals(child, index.get("8"));
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

  @Test
  void restUserMenuProperty_matchesSharedConstant() {
    assertEquals(RxmActionMenuConstants.REST_USER_MENU_PROP, ActionMenuAdaptor.REST_USER_MENU_PROP);
    assertEquals("sys_restUserMenu", RxmActionMenuConstants.REST_USER_MENU_PROP);
  }

  @Test
  void findMenuByKey_clearsRequestHibernateIndexOnUnsafeKey() {
    ActionMenuAdaptor indexed =
        new ActionMenuAdaptor(designWs, () -> true, () -> List.of());
    indexed.requestHibernateIndex();
    assertTrue(ActionMenuAdaptor.isRequestHibernateIndexBound());
    assertNull(indexed.findMenuByKey("../escape"));
    assertFalse(ActionMenuAdaptor.isRequestHibernateIndexBound());
  }

  @Test
  void findAllowedTransitions_clearsRequestHibernateIndex() {
    ActionMenuAdaptor indexed =
        new ActionMenuAdaptor(designWs, () -> true, () -> List.of());
    indexed.requestHibernateIndex();
    assertTrue(ActionMenuAdaptor.isRequestHibernateIndexBound());
    assertTrue(indexed.findAllowedTransitions(null, null).isEmpty());
    assertFalse(ActionMenuAdaptor.isRequestHibernateIndexBound());
  }

  private PSAction stubAction(String name, int id) {
    PSAction action = new PSAction(name, name);
    action.setGUID(new PSGuid(PSTypeEnum.ACTION, id));
    return action;
  }

  private PSAction stubCascadingMenu(String name, int id) {
    PSAction action = stubAction(name, id);
    action.setMenuType(PSAction.TYPE_MENU);
    action.setMenuDynamic(false);
    return action;
  }

  private static int childCount(PSAction action) {
    return action == null || action.getChildren() == null ? 0 : action.getChildren().size();
  }

  private void stubCatalogLoad(PSAction... actions) throws Exception {
    List<IPSCatalogSummary> sums = new ArrayList<>();
    Map<Integer, PSAction> byUuid = new HashMap<>();
    for (PSAction action : actions) {
      IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
      when(sum.getGUID()).thenReturn(action.getGUID());
      when(sum.getName()).thenReturn(action.getName());
      sums.add(sum);
      byUuid.put(action.getGUID().getUUID(), action);
    }
    when(designWs.findActions(
            nullable(String.class), nullable(String.class), nullable(List.class)))
        .thenReturn(sums);
    when(designWs.loadActions(anyList(), eq(false), eq(false), any(), any()))
        .thenAnswer(
            inv -> {
              List<?> ids = inv.getArgument(0);
              if (ids == null || ids.isEmpty()) {
                return List.of();
              }
              Object first = ids.get(0);
              int uuid = first instanceof IPSGuid guid ? guid.getUUID() : -1;
              PSAction hit = byUuid.get(uuid);
              return hit == null ? List.of() : List.of(hit);
            });
  }
}
