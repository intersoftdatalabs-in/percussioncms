/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.cecontrols;

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
    assertEquals("sys_EditBox", resource.listControls().get(0).getName());
  }

  @Test
  public void listControlsNullSafe() {
    when(adaptor.listControls()).thenReturn(null);
    assertTrue(resource.listControls().isEmpty());
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
  public void withoutInjectionFailsWithDiagnostic() {
    ControlsResource bare = new ControlsResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listControls);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());
  }
}
