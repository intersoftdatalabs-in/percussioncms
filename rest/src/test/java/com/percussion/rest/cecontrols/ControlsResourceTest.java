/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.cecontrols;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ControlsResourceTest {

  private IControlAdaptor adaptor;
  private ControlsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IControlAdaptor.class);
    resource = new ControlsResource(adaptor);
  }

  @Test
  public void listControlsDelegates() {
    ControlDef c = new ControlDef();
    c.setName("sys_EditBox");
    when(adaptor.listControls()).thenReturn(List.of(c));
    List<ControlDef> out = resource.listControls();
    assertEquals(1, out.size());
    assertEquals("sys_EditBox", out.get(0).getName());
    verify(adaptor).listControls();
  }

  @Test
  public void listControlsNullSafe() {
    when(adaptor.listControls()).thenReturn(null);
    assertTrue(resource.listControls().isEmpty());
  }

  @Test
  public void listControlsWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listControls()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listControls());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getControlDelegates() {
    ControlDef c = new ControlDef();
    c.setName("sys_EditBox");
    when(adaptor.findControlByName(eq("sys_EditBox"))).thenReturn(c);
    assertEquals("sys_EditBox", resource.getControl("sys_EditBox").getName());
    verify(adaptor).findControlByName("sys_EditBox");
  }

  @Test
  public void getControlNotFoundIsGeneric404() {
    when(adaptor.findControlByName(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getControl("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Control not found", ex.getMessage());
  }

  @Test
  public void getControlWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findControlByName(eq("sys_EditBox"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getControl("sys_EditBox"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getControlRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findControlByName(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getControl("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ControlsResource bare = new ControlsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listControls);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getControl must rethrow WebApplicationException from requireAdaptor (not re-wrap as 500)
    ControlsResource bare = new ControlsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getControl("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void createControlDelegates() {
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    ControlDef created = new ControlDef();
    created.setName("myUserControl");
    created.setScope("user");
    when(adaptor.createControl(any())).thenReturn(created);

    ControlDef out = resource.createControl(body);

    assertEquals("myUserControl", out.getName());
    assertEquals("user", out.getScope());
    verify(adaptor).createControl(body);
  }

  @Test
  public void createControlBlankNameIs400() {
    when(adaptor.createControl(any())).thenThrow(new IllegalArgumentException("name is required"));
    ControlDef body = new ControlDef();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createControl(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createControlDuplicateIs409() {
    when(adaptor.createControl(any()))
        .thenThrow(new WebApplicationException("Control already exists: myUserControl", 409));
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createControl(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createControlNonAdminIs403() {
    when(adaptor.createControl(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createControl(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateControlDelegates() {
    ControlDef body = new ControlDef();
    body.setDisplayName("Updated");
    ControlDef updated = new ControlDef();
    updated.setName("myUserControl");
    updated.setDisplayName("Updated");
    when(adaptor.saveControl(eq("myUserControl"), eq(body))).thenReturn(updated);

    ControlDef out = resource.updateControl("myUserControl", body);

    assertEquals("Updated", out.getDisplayName());
    verify(adaptor).saveControl("myUserControl", body);
  }

  @Test
  public void updateControlUnknownIs404() {
    when(adaptor.saveControl(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateControl("missing", new ControlDef()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateControlNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateControl("myUserControl", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).saveControl(any(), any());
  }

  @Test
  public void updateControlSystemIs409() {
    when(adaptor.saveControl(eq("sys_EditBox"), any()))
        .thenThrow(new WebApplicationException("System controls cannot be updated", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateControl("sys_EditBox", new ControlDef()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteControlNoContent() {
    when(adaptor.deleteControl(eq("myUserControl"))).thenReturn(true);

    Response r = resource.deleteControl("myUserControl");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteControl("myUserControl");
  }

  @Test
  public void deleteControlUnknownIs404() {
    when(adaptor.deleteControl(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteControl("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteControlSystemIs409() {
    when(adaptor.deleteControl(eq("sys_EditBox")))
        .thenThrow(new WebApplicationException("System controls cannot be deleted", 409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteControl("sys_EditBox"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteControlNonAdminIs403() {
    when(adaptor.deleteControl(eq("myUserControl")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteControl("myUserControl"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnCreate() {
    ControlsResource bare = new ControlsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createControl(new ControlDef()));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
