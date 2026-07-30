/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.serverconfigs;

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
public class ServerConfigsResourceTest {

  private IServerConfigAdaptor adaptor;
  private ServerConfigsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IServerConfigAdaptor.class);
    resource = new ServerConfigsResource(adaptor);
  }

  @Test
  public void listConfigsDelegates() {
    ServerConfigSummary s = new ServerConfigSummary();
    s.setName("LOG_CONFIG");
    when(adaptor.listConfigs()).thenReturn(List.of(s));
    assertEquals("LOG_CONFIG", resource.listConfigs().get(0).getName());
  }

  @Test
  public void listConfigsNullSafe() {
    when(adaptor.listConfigs()).thenReturn(null);
    assertTrue(resource.listConfigs().isEmpty());
  }

  @Test
  public void getConfigDelegates() {
    ServerConfigSummary s = new ServerConfigSummary();
    s.setName("LOG_CONFIG");
    when(adaptor.findConfigByName(eq("LOG_CONFIG"))).thenReturn(s);
    assertEquals("LOG_CONFIG", resource.getConfig("LOG_CONFIG").getName());
    verify(adaptor).findConfigByName("LOG_CONFIG");
  }

  @Test
  public void getConfigNotFoundIsGeneric404() {
    when(adaptor.findConfigByName(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getConfig("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Configuration not found", ex.getMessage());
  }

  @Test
  public void getConfigWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findConfigByName(eq("LOG_CONFIG"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getConfig("LOG_CONFIG"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    ServerConfigsResource bare = new ServerConfigsResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listConfigs);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());
  }
}
