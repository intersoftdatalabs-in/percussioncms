/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.serverconfigs;

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
    List<ServerConfigSummary> out = resource.listConfigs();
    assertEquals(1, out.size());
    assertEquals("LOG_CONFIG", out.get(0).getName());
    verify(adaptor).listConfigs();
  }

  @Test
  public void listConfigsNullSafe() {
    when(adaptor.listConfigs()).thenReturn(null);
    assertTrue(resource.listConfigs().isEmpty());
  }

  @Test
  public void listConfigsWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listConfigs()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listConfigs());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
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
  public void getConfigRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findConfigByName(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getConfig("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ServerConfigsResource bare = new ServerConfigsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listConfigs);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getConfig must rethrow WebApplicationException from requireAdaptor (not re-wrap as 500)
    ServerConfigsResource bare = new ServerConfigsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getConfig("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void updateConfigDelegates() {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("rootLogger=INFO");
    ServerConfigSummary updated = new ServerConfigSummary();
    updated.setName("LOG_CONFIG");
    updated.setContent("rootLogger=INFO");
    when(adaptor.updateConfig(eq("LOG_CONFIG"), eq(body))).thenReturn(updated);

    ServerConfigSummary out = resource.updateConfig("LOG_CONFIG", body);

    assertEquals("LOG_CONFIG", out.getName());
    assertEquals("rootLogger=INFO", out.getContent());
    verify(adaptor).updateConfig("LOG_CONFIG", body);
  }

  @Test
  public void updateConfigUnknownIs404() {
    when(adaptor.updateConfig(eq("NOT_A_REAL_CONFIG"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateConfig("NOT_A_REAL_CONFIG", bodyWithContent("x")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateConfigUnsafeNameIs404() {
    when(adaptor.updateConfig(eq("../etc/passwd"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateConfig("../etc/passwd", bodyWithContent("x")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateConfigNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateConfig("LOG_CONFIG", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateConfig(any(), any());
  }

  @Test
  public void updateConfigNonAdminIs403() {
    when(adaptor.updateConfig(eq("LOG_CONFIG"), any()))
        .thenThrow(
            new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateConfig("LOG_CONFIG", bodyWithContent("x")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateConfigWrapsIllegalArgumentAs400() {
    when(adaptor.updateConfig(eq("LOG_CONFIG"), any()))
        .thenThrow(new IllegalArgumentException("content is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateConfig("LOG_CONFIG", new ServerConfigSummary()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnUpdate() {
    ServerConfigsResource bare = new ServerConfigsResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.updateConfig("LOG_CONFIG", bodyWithContent("x")));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static ServerConfigSummary bodyWithContent(String content) {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent(content);
    return body;
  }
}
