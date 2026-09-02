/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.actions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ActionMenuResourceTest {

  private IActionMenuAdaptor adaptor;
  private ActionMenuResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IActionMenuAdaptor.class);
    resource = new ActionMenuResource();
    // field injection for tests
    try {
      var f = ActionMenuResource.class.getDeclaredField("adaptor");
      f.setAccessible(true);
      f.set(resource, adaptor);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void listActionMenusDelegates() throws Exception {
    ActionMenu m = new ActionMenu();
    m.setName("Edit");
    when(adaptor.findMenus(null, null, null, null, null)).thenReturn(List.of(m));

    List<ActionMenu> out = resource.listActionMenus();
    assertEquals(1, out.size());
    assertEquals("Edit", out.get(0).getName());
  }

  @Test
  public void listActionMenusNullSafe() throws Exception {
    when(adaptor.findMenus(null, null, null, null, null)).thenReturn(null);
    assertTrue(resource.listActionMenus().isEmpty());
  }

  @Test
  public void getActionMenuDelegates() {
    ActionMenu m = new ActionMenu();
    m.setName("Edit");
    when(adaptor.findMenuByKey(eq("Edit"))).thenReturn(m);

    assertEquals("Edit", resource.getActionMenu("Edit").getName());
    verify(adaptor).findMenuByKey("Edit");
  }

  @Test
  public void getActionMenuNotFoundIsGeneric404() {
    when(adaptor.findMenuByKey(eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getActionMenu("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Action menu not found", ex.getMessage());
  }

  @Test
  public void getActionMenuWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findMenuByKey(eq("Edit"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getActionMenu("Edit"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    ActionMenuResource bare = new ActionMenuResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listActionMenus);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());

    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getActionMenu("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());

    IllegalStateException typesEx =
        assertThrows(IllegalStateException.class, () -> bare.getAllowedContentTypeMenus(null));
    assertTrue(typesEx.getMessage().contains("not configured"));

    IllegalStateException templatesEx =
        assertThrows(IllegalStateException.class, () -> bare.getAllowedTemplateMenus(551, false));
    assertTrue(templatesEx.getMessage().contains("not configured"));
  }

  @Test
  public void getAllowedContentTypeMenusPassesContentIdsArray() {
    ActionMenu menu = new ActionMenu();
    menu.setName("New");
    when(adaptor.findAllowedContentTypes(any())).thenReturn(List.of(menu));

    AllowedContentTypeMenusRequest request = new AllowedContentTypeMenusRequest();
    request.setContentIds(new int[] {101, 102});
    ActionMenuList out = resource.getAllowedContentTypeMenus(request);

    assertEquals(1, out.size());
    assertEquals("New", out.get(0).getName());
    ArgumentCaptor<Integer[]> captor = ArgumentCaptor.forClass(Integer[].class);
    verify(adaptor).findAllowedContentTypes(captor.capture());
    assertArrayEquals(new Integer[] {101, 102}, captor.getValue());
  }

  @Test
  public void getAllowedContentTypeMenusTreatsNullContentIdsAsEmpty() {
    when(adaptor.findAllowedContentTypes(any())).thenReturn(List.of());

    AllowedContentTypeMenusRequest request = new AllowedContentTypeMenusRequest();
    ActionMenuList out = resource.getAllowedContentTypeMenus(request);

    assertTrue(out.isEmpty());
    ArgumentCaptor<Integer[]> captor = ArgumentCaptor.forClass(Integer[].class);
    verify(adaptor).findAllowedContentTypes(captor.capture());
    assertArrayEquals(new Integer[0], captor.getValue());
  }

  @Test
  public void getAllowedContentTypeMenusNullRequestIsEmptyNot500() {
    when(adaptor.findAllowedContentTypes(any())).thenReturn(List.of());
    ActionMenuList out = resource.getAllowedContentTypeMenus(null);
    assertTrue(out.isEmpty());
  }

  @Test
  public void getAllowedContentTypeMenusAdaptorFailureIsEmptyNot500() {
    when(adaptor.findAllowedContentTypes(any())).thenThrow(new IllegalStateException("down"));
    AllowedContentTypeMenusRequest request = new AllowedContentTypeMenusRequest();
    request.setContentIds(new int[] {551});
    ActionMenuList out = resource.getAllowedContentTypeMenus(request);
    assertTrue(out.isEmpty());
  }

  @Test
  public void getAllowedTemplateMenusDelegates() {
    ActionMenu menu = new ActionMenu();
    menu.setName("rffHome");
    when(adaptor.findAllowedTemplates(eq(551), eq(false))).thenReturn(List.of(menu));
    ActionMenuList out = resource.getAllowedTemplateMenus(551, false);
    assertEquals(1, out.size());
    assertEquals("rffHome", out.get(0).getName());
  }

  @Test
  public void getAllowedTemplateMenusNullAdaptorResultIsEmpty() {
    when(adaptor.findAllowedTemplates(eq(551), eq(true))).thenReturn(null);
    ActionMenuList out = resource.getAllowedTemplateMenus(551, true);
    assertTrue(out.isEmpty());
  }

  @Test
  public void getAllowedTemplateMenusAdaptorFailureIsEmptyNot500() {
    when(adaptor.findAllowedTemplates(eq(551), eq(false)))
        .thenThrow(new IllegalArgumentException("Request can't be null"));
    ActionMenuList out = resource.getAllowedTemplateMenus(551, false);
    assertTrue(out.isEmpty());
  }

  @Test
  public void createActionMenuClearsIdAndDelegates() {
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    body.setId(9);
    ActionMenu created = new ActionMenu();
    created.setName("MyMenu");
    created.setId(42);
    when(adaptor.createActionMenu(any())).thenReturn(created);

    ActionMenu out = resource.createActionMenu(body);

    assertEquals("MyMenu", out.getName());
    assertEquals(-1, body.getId());
    assertEquals(null, body.getGuid());
    verify(adaptor).createActionMenu(body);
  }

  @Test
  public void createActionMenuBlankNameIs400() {
    when(adaptor.createActionMenu(any())).thenThrow(new IllegalArgumentException("name is required"));
    ActionMenu body = new ActionMenu();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createActionMenu(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createActionMenuDuplicateIs409() {
    when(adaptor.createActionMenu(any()))
        .thenThrow(new WebApplicationException("Action menu already exists: MyMenu", 409));
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createActionMenu(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createActionMenuNonAdminIs403() {
    when(adaptor.createActionMenu(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    ActionMenu body = new ActionMenu();
    body.setName("MyMenu");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createActionMenu(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateActionMenuDelegates() {
    ActionMenu body = new ActionMenu();
    body.setLabel("Updated");
    body.setDescription("desc");
    body.setMenuType("MENU");
    body.setUrl("/run");
    ActionMenu updated = new ActionMenu();
    updated.setName("MyMenu");
    updated.setLabel("Updated");
    updated.setDescription("desc");
    updated.setMenuType("MENU");
    updated.setUrl("/run");
    when(adaptor.saveActionMenu(eq("MyMenu"), eq(body))).thenReturn(updated);

    ActionMenu out = resource.updateActionMenu("MyMenu", body);

    assertEquals("Updated", out.getLabel());
    assertEquals("desc", out.getDescription());
    assertEquals("MENU", out.getMenuType());
    assertEquals("/run", out.getUrl());
    verify(adaptor).saveActionMenu("MyMenu", body);
  }

  @Test
  public void updateActionMenuUsageCommandDelegates() {
    ActionMenu body = new ActionMenu();
    body.setHandler("CLIENT");
    body.setUrl("/sys_cxSupport/foo.xml");
    ActionMenuParameter param = new ActionMenuParameter();
    param.setName("sys_contentid");
    param.setValue("PSX_CONTENTID");
    body.setParameters(new ActionMenuParameter[] {param});
    ActionMenuProperty accel = new ActionMenuProperty();
    accel.setName("AcceleratorKey");
    accel.setValue("ctrl S");
    body.setProperties(new ActionMenuProperty[] {accel});
    ActionMenu updated = new ActionMenu();
    updated.setName("MyMenu");
    updated.setHandler("CLIENT");
    updated.setUrl("/sys_cxSupport/foo.xml");
    updated.setParameters(body.getParameters());
    updated.setProperties(body.getProperties());
    when(adaptor.saveActionMenu(eq("MyMenu"), eq(body))).thenReturn(updated);

    ActionMenu out = resource.updateActionMenu("MyMenu", body);

    assertEquals("CLIENT", out.getHandler());
    assertEquals("/sys_cxSupport/foo.xml", out.getUrl());
    assertEquals("sys_contentid", out.getParameters()[0].getName());
    assertEquals("ctrl S", out.getProperties()[0].getValue());
    verify(adaptor).saveActionMenu("MyMenu", body);
  }

  @Test
  public void updateActionMenuUnknownIs404() {
    when(adaptor.saveActionMenu(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateActionMenu("missing", new ActionMenu()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateActionMenuNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateActionMenu("MyMenu", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).saveActionMenu(any(), any());
  }

  @Test
  public void updateActionMenuSystemIs409() {
    when(adaptor.saveActionMenu(eq("Edit"), any()))
        .thenThrow(
            new WebApplicationException(
                "System action menus cannot be updated or deleted via this API", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateActionMenu("Edit", new ActionMenu()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteActionMenuNoContent() {
    when(adaptor.deleteActionMenu(eq("MyMenu"))).thenReturn(true);

    Response r = resource.deleteActionMenu("MyMenu");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteActionMenu("MyMenu");
  }

  @Test
  public void deleteActionMenuUnknownIs404() {
    when(adaptor.deleteActionMenu(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteActionMenu("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteActionMenuNonAdminIs403() {
    when(adaptor.deleteActionMenu(eq("MyMenu")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteActionMenu("MyMenu"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void deleteActionMenuSystemIs409() {
    when(adaptor.deleteActionMenu(eq("Edit")))
        .thenThrow(
            new WebApplicationException(
                "System action menus cannot be updated or deleted via this API", 409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteActionMenu("Edit"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns500OnCreate() {
    ActionMenuResource bare = new ActionMenuResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createActionMenu(new ActionMenu()));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailurePreservesAdaptorWebApplicationException() {
    WebApplicationException lock = new WebApplicationException("locked", 409);
    assertSame(lock, ActionMenuResource.mapWriteFailure(lock));
  }

  @Test
  public void mapWriteFailureIllegalArgumentIs400() {
    WebApplicationException mapped =
        ActionMenuResource.mapWriteFailure(new IllegalArgumentException("name is required"));
    assertEquals(400, mapped.getResponse().getStatus());
  }

  @Test
  public void getAllowedTransitionsStaysEmpty() {
    ActionMenuList out = resource.getAllowedTransitions(new AllowedWorkflowTransitionsRequest());
    assertTrue(out.isEmpty());
  }
}
