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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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
        assertThrows(
            WebApplicationException.class, () -> bare.getExtensionCatalogItem("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getExtensionCatalogItemRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findExtensionByKey(any(), eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getExtensionCatalogItem("xx"));
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

  @Test
  public void registerExtensionDelegates() {
    Extension body = userBody("my_user_ext");
    Extension created = userBody("my_user_ext");
    created.setContext("user/");
    when(adaptor.registerExtension(any(), eq(body))).thenReturn(created);

    Extension out = resource.registerExtension(body);

    assertEquals("my_user_ext", out.getExtensionName());
    assertEquals("user/", out.getContext());
    verify(adaptor).registerExtension(any(), eq(body));
  }

  @Test
  public void registerExtensionBlankNameIs400() {
    when(adaptor.registerExtension(any(), any()))
        .thenThrow(new IllegalArgumentException("extensionName is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.registerExtension(new Extension()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void registerExtensionDuplicateIs409() {
    when(adaptor.registerExtension(any(), any()))
        .thenThrow(new WebApplicationException("Extension already exists: Java/user/my_user_ext", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.registerExtension(userBody("my_user_ext")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void registerExtensionNonAdminIs403() {
    when(adaptor.registerExtension(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.registerExtension(userBody("my_user_ext")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateExtensionDelegates() {
    Extension body = userBody("my_user_ext");
    body.setDeprecated(true);
    Extension updated = userBody("my_user_ext");
    updated.setDeprecated(true);
    when(adaptor.updateExtension(any(), eq("my_user_ext"), eq(body))).thenReturn(updated);

    Extension out = resource.updateExtension("my_user_ext", body);

    assertTrue(out.isDeprecated());
    verify(adaptor).updateExtension(any(), eq("my_user_ext"), eq(body));
  }

  @Test
  public void updateExtensionUnknownIs404() {
    when(adaptor.updateExtension(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateExtension("missing", new Extension()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateExtensionNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateExtension("my_user_ext", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateExtension(any(), any(), any());
  }

  @Test
  public void updateExtensionSystemIs409() {
    when(adaptor.updateExtension(any(), eq("sys_add"), any()))
        .thenThrow(new WebApplicationException("System or handler-owned extensions cannot be updated", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateExtension("sys_add", new Extension()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteExtensionNoContent() {
    when(adaptor.deleteExtension(any(), eq("my_user_ext"))).thenReturn(true);

    Response r = resource.deleteExtension("my_user_ext");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteExtension(any(), eq("my_user_ext"));
  }

  @Test
  public void deleteExtensionUnknownIs404() {
    when(adaptor.deleteExtension(any(), eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteExtension("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteExtensionSystemIs409() {
    when(adaptor.deleteExtension(any(), eq("sys_add")))
        .thenThrow(new WebApplicationException("System or handler-owned extensions cannot be deleted", 409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteExtension("sys_add"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteExtensionNonAdminIs403() {
    when(adaptor.deleteExtension(any(), eq("my_user_ext")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteExtension("my_user_ext"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnRegister() {
    ExtensionsResource bare = new ExtensionsResource();
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.registerExtension(userBody("x")));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static Extension userBody(String name) {
    Extension e = new Extension();
    e.setExtensionName(name);
    e.setHandlerName("Java");
    e.setSupportedInterfaces(List.of("com.percussion.extension.IPSUdfProcessor"));
    e.setInitParameters(java.util.Map.of("className", "com.example.MyExt"));
    return e;
  }
}
