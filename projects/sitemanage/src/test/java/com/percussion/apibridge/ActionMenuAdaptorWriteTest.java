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
import com.percussion.cms.objectstore.PSActionParameter;
import com.percussion.cms.objectstore.PSActionVisibilityContext;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuList;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import com.percussion.rest.actions.ActionMenuParameter;
import com.percussion.rest.actions.ActionMenuProperty;
import com.percussion.rest.actions.ActionMenuVisibilityContext;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.RxmActionMenuConstants;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.data.ActionType;
import jakarta.ws.rs.WebApplicationException;
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
  void update_usageAndCommand_roundTripsOnPutAndDto() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    existing.getProperties().setProperty(ActionMenuAdaptor.REST_USER_MENU_PROP, PSAction.YES);
    existing.getParameters().setParameter("oldParam", "old");
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    locked.getProperties().setProperty(ActionMenuAdaptor.REST_USER_MENU_PROP, PSAction.YES);
    locked.getParameters().setParameter("oldParam", "old");
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ActionMenu body = new ActionMenu();
    body.setHandler(PSAction.HANDLER_CLIENT);
    body.setUrl("/sys_cxSupport/foo.xml");
    ActionMenuParameter param = new ActionMenuParameter();
    param.setName("sys_contentid");
    param.setValue("PSX_CONTENTID");
    param.setDescription("content id");
    body.setParameters(new ActionMenuParameter[] {param});
    ActionMenuProperty accel = new ActionMenuProperty();
    accel.setName(PSAction.PROP_ACCEL_KEY);
    accel.setValue("ctrl S");
    ActionMenuProperty mnem = new ActionMenuProperty();
    mnem.setName(PSAction.PROP_MNEM_KEY);
    mnem.setValue("S");
    ActionMenuProperty launch = new ActionMenuProperty();
    launch.setName(PSAction.PROP_LAUNCH_NEW_WND);
    launch.setValue(PSAction.YES);
    ActionMenuProperty marker = new ActionMenuProperty();
    marker.setName(ActionMenuAdaptor.REST_USER_MENU_PROP);
    marker.setValue(PSAction.NO);
    body.setProperties(new ActionMenuProperty[] {accel, mnem, launch, marker});

    ActionMenu out = adaptor.saveActionMenu("MyMenu", body);

    assertEquals(PSAction.HANDLER_CLIENT, out.getHandler());
    assertEquals("/sys_cxSupport/foo.xml", out.getUrl());
    assertEquals(1, out.getParameters().length);
    assertEquals("sys_contentid", out.getParameters()[0].getName());
    assertEquals("PSX_CONTENTID", out.getParameters()[0].getValue());
    assertEquals("content id", out.getParameters()[0].getDescription());
    assertEquals("ctrl S", propertyValue(out, PSAction.PROP_ACCEL_KEY));
    assertEquals("S", propertyValue(out, PSAction.PROP_MNEM_KEY));
    assertEquals(PSAction.YES, propertyValue(out, PSAction.PROP_LAUNCH_NEW_WND));
    assertEquals(PSAction.YES, propertyValue(out, ActionMenuAdaptor.REST_USER_MENU_PROP));
    assertTrue(locked.isClientAction());
    assertEquals("/sys_cxSupport/foo.xml", locked.getURL());
    assertEquals("PSX_CONTENTID", locked.getParameters().getParameter("sys_contentid"));
    assertNull(locked.getParameters().getParameter("oldParam"));
    assertEquals("ctrl S", locked.getProperty(PSAction.PROP_ACCEL_KEY));
    assertEquals(PSAction.YES, locked.getProperty(ActionMenuAdaptor.REST_USER_MENU_PROP));
    verify(designWs).saveActions(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_visibilityAndUiContexts_roundTripsOnPutAndDto() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    existing.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE, "old");
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    locked.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE, "old");
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ActionMenu body = new ActionMenu();
    ActionMenuVisibilityContext community = new ActionMenuVisibilityContext();
    community.setName("community");
    community.setValue("100");
    community.setDescription("community id");
    ActionMenuVisibilityContext community2 = new ActionMenuVisibilityContext();
    community2.setName(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY);
    community2.setValue("200");
    ActionMenuVisibilityContext contentType = new ActionMenuVisibilityContext();
    contentType.setName("contentType");
    contentType.setValue("5");
    body.setVisibilityContexts(
        new ActionMenuVisibilityContext[] {community, community2, contentType});
    ActionMenuModeUIContext ui = new ActionMenuModeUIContext();
    ui.setModeId("1");
    ui.setModeName("CX");
    ui.setContextId("2");
    ui.setContextName("Folder");
    body.setUiContexts(new ActionMenuModeUIContext[] {ui});

    ActionMenu out = adaptor.saveActionMenu("MyMenu", body);

    assertEquals(3, out.getVisibilityContexts().length);
    assertEquals(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, out.getVisibilityContexts()[0].getName());
    assertEquals("100", out.getVisibilityContexts()[0].getValue());
    assertEquals("community id", out.getVisibilityContexts()[0].getDescription());
    assertEquals("200", out.getVisibilityContexts()[1].getValue());
    assertEquals(PSActionVisibilityContext.VIS_CONTEXT_CONTENT_TYPE, out.getVisibilityContexts()[2].getName());
    assertEquals("5", out.getVisibilityContexts()[2].getValue());
    assertEquals(1, out.getUiContexts().length);
    assertEquals("1", out.getUiContexts()[0].getModeId());
    assertEquals("CX", out.getUiContexts()[0].getModeName());
    assertEquals("2", out.getUiContexts()[0].getContextId());
    assertEquals("Folder", out.getUiContexts()[0].getContextName());
    assertNull(locked.getVisibilityContexts().getContext(PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE));
    assertEquals(
        "100",
        firstVisibilityValue(locked, PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY));
    assertEquals(1, locked.getModeUIContexts().size());
    verify(designWs).saveActions(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_invalidVisibility_is400BeforeSave() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));
    ActionMenu body = new ActionMenu();
    ActionMenuVisibilityContext vis = new ActionMenuVisibilityContext();
    vis.setName("not-a-context");
    vis.setValue("x");
    body.setVisibilityContexts(new ActionMenuVisibilityContext[] {vis});
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveActionMenu("MyMenu", body));
    assertTrue(ex.getMessage().toLowerCase().contains("visibility"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_invalidUiContext_is400BeforeSave() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));
    ActionMenu body = new ActionMenu();
    ActionMenuModeUIContext ui = new ActionMenuModeUIContext();
    ui.setModeId("cx");
    ui.setContextId("2");
    body.setUiContexts(new ActionMenuModeUIContext[] {ui});
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveActionMenu("MyMenu", body));
    assertTrue(ex.getMessage().toLowerCase().contains("modeid"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void applyVisibilityContexts_emptyArrayClears() {
    PSAction domain = stubAction("MyMenu", 42);
    domain.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, "1");
    ActionMenuAdaptor.applyVisibilityContexts(domain, new ActionMenuVisibilityContext[0]);
    assertEquals(0, domain.getVisibilityContexts().size());
  }

  @Test
  void applyWritableFields_nullVisibilityLeavesExisting() {
    PSAction domain = stubAction("MyMenu", 42);
    domain.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, "keep");
    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    ActionMenuAdaptor.applyWritableFields(domain, body);
    assertEquals("Updated", domain.getLabel());
    assertEquals(
        "keep",
        firstVisibilityValue(domain, PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY));
  }

  @Test
  void overlayDesignVisibility_copiesFromUnlockedLoad() throws Exception {
    ActionMenu catalog = new ActionMenu();
    catalog.setName("MyMenu");
    catalog.setId(42);
    PSAction loaded = stubAction("MyMenu", 42);
    loaded.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, "100");
    PSMenuModeContextMapping mapping = new PSMenuModeContextMapping("1", "2", "42");
    mapping.setModeName("CX");
    mapping.setContextName("Folder");
    loaded.getModeUIContexts().add(mapping);
    when(designWs.loadActions(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loaded));

    adaptor.overlayDesignVisibility(catalog);

    assertEquals(1, catalog.getVisibilityContexts().length);
    assertEquals(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, catalog.getVisibilityContexts()[0].getName());
    assertEquals("100", catalog.getVisibilityContexts()[0].getValue());
    assertEquals(1, catalog.getUiContexts().length);
    assertEquals("1", catalog.getUiContexts()[0].getModeId());
    assertEquals("Folder", catalog.getUiContexts()[0].getContextName());
  }

  @Test
  void update_invalidHandler_is400BeforeSave() throws Exception {
    PSAction existing = stubAction("MyMenu", 42);
    stubCatalogLoad(existing);
    PSAction locked = stubAction("MyMenu", 42);
    when(designWs.loadActions(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));
    ActionMenu body = new ActionMenu();
    body.setHandler("neither");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveActionMenu("MyMenu", body));
    assertTrue(ex.getMessage().toLowerCase().contains("handler"));
    verify(designWs, never()).saveActions(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void applyWritableFields_nullParametersLeavesExisting() {
    PSAction domain = stubAction("MyMenu", 42);
    domain.getParameters().setParameter("keep", "1");
    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    ActionMenuAdaptor.applyWritableFields(domain, body);
    assertEquals("1", domain.getParameters().getParameter("keep"));
    assertEquals("Updated", domain.getLabel());
  }

  @Test
  void applyParameters_emptyArrayClears() {
    PSAction domain = stubAction("MyMenu", 42);
    domain.getParameters().setParameter("gone", "x");
    ActionMenuAdaptor.applyParameters(domain, new ActionMenuParameter[0]);
    assertEquals(0, domain.getParameters().size());
  }

  @Test
  void toDto_mapsHandlerParametersAndProperties() {
    PSAction action = stubAction("MyMenu", 42);
    action.setClientAction(false);
    action.setURL("/cmd");
    action.getParameters().add(new PSActionParameter("p1", "v1", "d1"));
    action.getProperties().setProperty(PSAction.PROP_SHORT_DESC, "tip");
    action.getVisibilityContexts().addContext(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, "9");
    PSMenuModeContextMapping mapping = new PSMenuModeContextMapping("3", "4", "42");
    mapping.setModeName("Mode");
    action.getModeUIContexts().add(mapping);
    ActionMenu dto = ActionMenuAdaptor.toDto(action);
    assertEquals(PSAction.HANDLER_SERVER, dto.getHandler());
    assertEquals("/cmd", dto.getUrl());
    assertEquals(1, dto.getParameters().length);
    assertEquals("p1", dto.getParameters()[0].getName());
    assertEquals("v1", dto.getParameters()[0].getValue());
    assertEquals("d1", dto.getParameters()[0].getDescription());
    assertEquals("tip", propertyValue(dto, PSAction.PROP_SHORT_DESC));
    assertEquals(1, dto.getVisibilityContexts().length);
    assertEquals(PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY, dto.getVisibilityContexts()[0].getName());
    assertEquals("9", dto.getVisibilityContexts()[0].getValue());
    assertEquals("3", dto.getUiContexts()[0].getModeId());
    assertEquals("Mode", dto.getUiContexts()[0].getModeName());
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

  private static String firstVisibilityValue(PSAction action, String name) {
    PSActionVisibilityContext ctx = action.getVisibilityContexts().getContext(name);
    if (ctx == null) {
      return null;
    }
    var it = ctx.iterator();
    return it.hasNext() ? it.next() : null;
  }

  private static String propertyValue(ActionMenu menu, String name) {
    if (menu.getProperties() == null) {
      return null;
    }
    for (ActionMenuProperty prop : menu.getProperties()) {
      if (prop != null && name.equalsIgnoreCase(prop.getName())) {
        return prop.getValue();
      }
    }
    return null;
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
