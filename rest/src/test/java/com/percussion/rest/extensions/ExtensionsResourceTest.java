/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.extensions;

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
  public void setUp() throws Exception {
    adaptor = mock(IExtensionAdaptor.class);
    uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/"));
    resource = new ExtensionsResource();
    var af = ExtensionsResource.class.getDeclaredField("adaptor");
    af.setAccessible(true);
    af.set(resource, adaptor);
    var uf = ExtensionsResource.class.getDeclaredField("uriInfo");
    uf.setAccessible(true);
    uf.set(resource, uriInfo);
  }

  @Test
  public void listExtensionsCatalogDelegates() {
    Extension e = new Extension();
    e.setExtensionName("sys_add");
    when(adaptor.listExtensions(any())).thenReturn(List.of(e));
    List<Extension> out = resource.listExtensionsCatalog();
    assertEquals(1, out.size());
    assertEquals("sys_add", out.get(0).getExtensionName());
  }

  @Test
  public void listExtensionsCatalogNullSafe() {
    when(adaptor.listExtensions(any())).thenReturn(null);
    assertTrue(resource.listExtensionsCatalog().isEmpty());
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

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    ExtensionsResource bare = new ExtensionsResource();
    try {
      var uf = ExtensionsResource.class.getDeclaredField("uriInfo");
      uf.setAccessible(true);
      uf.set(bare, uriInfo);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listExtensionsCatalog);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());
  }
}
