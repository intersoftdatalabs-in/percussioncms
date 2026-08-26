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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
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
}
