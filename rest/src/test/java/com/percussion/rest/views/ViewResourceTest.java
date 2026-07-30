/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ViewResourceTest {

  private IViewAdaptor adaptor;
  private ViewResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IViewAdaptor.class);
    resource = new ViewResource(adaptor);
  }

  @Test
  public void listViewsDelegates() {
    ViewDef v = new ViewDef();
    v.setName("My View");
    when(adaptor.listViews()).thenReturn(List.of(v));
    assertEquals("My View", resource.listViews().get(0).getName());
  }

  @Test
  public void listViewsNullSafe() {
    when(adaptor.listViews()).thenReturn(null);
    assertTrue(resource.listViews().isEmpty());
  }

  @Test
  public void getViewDelegates() {
    ViewDef v = new ViewDef();
    v.setName("My View");
    when(adaptor.findViewByKey(eq("My View"))).thenReturn(v);
    assertEquals("My View", resource.getView("My View").getName());
    verify(adaptor).findViewByKey("My View");
  }

  @Test
  public void getViewNotFoundIsGeneric404() {
    when(adaptor.findViewByKey(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getView("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("View not found", ex.getMessage());
  }

  @Test
  public void getViewWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findViewByKey(eq("My View"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getView("My View"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    ViewResource bare = new ViewResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listViews);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());
    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getView("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());
  }
}
