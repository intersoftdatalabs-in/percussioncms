/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ExtensionsResourceTest {

  private IExtensionAdaptor adaptor;
  private ExtensionsResource resource;
  private UriInfo uriInfo;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IExtensionAdaptor.class);
    uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/"));
    resource = new ExtensionsResource(adaptor);
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void listExtensionsCatalogDelegates() {
    Extension e = new Extension();
    e.setExtensionName("sys_add");
    when(adaptor.listExtensions(any())).thenReturn(List.of(e));
    List<Extension> out = resource.listExtensionsCatalog();
    assertEquals(1, out.size());
    assertEquals("sys_add", out.get(0).getExtensionName());
    verify(adaptor).listExtensions(any());
  }

  @Test
  public void listExtensionsCatalogNullSafe() {
    when(adaptor.listExtensions(any())).thenReturn(null);
    assertTrue(resource.listExtensionsCatalog().isEmpty());
  }

  @Test
  public void listExtensionsCatalogWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listExtensions(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listExtensionsCatalog());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ExtensionsResource bare = new ExtensionsResource();
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listExtensionsCatalog);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getExtensionCatalogItem must rethrow WebApplicationException from requireAdaptor (not
    // re-wrap as 500)
    ExtensionsResource bare = new ExtensionsResource();
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getExtensionCatalogItem("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getExtensionCatalogItemRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findExtensionByKey(any(), eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getExtensionCatalogItem("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getExtensionCatalogItemDelegates() {
    Extension e = new Extension();
    e.setExtensionName("sys_add");
    when(adaptor.findExtensionByKey(any(), eq("sys_add"))).thenReturn(e);
    assertEquals("sys_add", resource.getExtensionCatalogItem("sys_add").getExtensionName());
    verify(adaptor).findExtensionByKey(any(), eq("sys_add"));
  }

  @Test
  public void getExtensionCatalogItemNotFoundIsGeneric404() {
    when(adaptor.findExtensionByKey(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getExtensionCatalogItem("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Extension not found", ex.getMessage());
  }

  @Test
  public void getExtensionCatalogItemWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findExtensionByKey(any(), eq("sys_add"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getExtensionCatalogItem("sys_add"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }
}
