/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
public class ViewResourceTest {

  private IViewAdaptor adaptor;
  private ViewResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IViewAdaptor.class);
    resource = new ViewResource(adaptor);
  }

  @Test
  public void listViewsSuccess() {
    ViewDef v = new ViewDef();
    v.setName("My View");
    when(adaptor.listViews()).thenReturn(List.of(v));
    List<ViewDef> out = resource.listViews();
    assertEquals(1, out.size());
    assertEquals("My View", out.get(0).getName());
    verify(adaptor).listViews();
  }

  @Test
  public void listViewsNullSafe() {
    when(adaptor.listViews()).thenReturn(null);
    assertTrue(resource.listViews().isEmpty());
  }

  @Test
  public void listViewsWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listViews()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listViews());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ViewResource bare = new ViewResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listViews);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getView must rethrow WebApplicationException from requireAdaptor (not re-wrap as 500)
    ViewResource bare = new ViewResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getView("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getViewRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findViewByKey(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getView("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
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
  public void executeViewSuccess() {
    ViewExecuteRequest req = new ViewExecuteRequest();
    req.setStartIndex(1);
    req.setMaxResults(10);
    ViewExecuteResult expected = new ViewExecuteResult();
    expected.setViewName("All Content");
    expected.setTotalCount(0);
    when(adaptor.executeView(eq("All Content"), eq(req))).thenReturn(expected);

    ViewExecuteResult out = resource.executeView("All Content", req);
    assertSame(expected, out);
    assertEquals("All Content", out.getViewName());
    verify(adaptor).executeView("All Content", req);
  }

  @Test
  public void executeViewNullBodyDelegates() {
    ViewExecuteResult expected = new ViewExecuteResult();
    expected.setViewName("All Content");
    when(adaptor.executeView(eq("All Content"), isNull())).thenReturn(expected);

    assertEquals("All Content", resource.executeView("All Content", null).getViewName());
    verify(adaptor).executeView("All Content", null);
  }

  @Test
  public void executeViewNotFoundIsGeneric404() {
    when(adaptor.executeView(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeView("missing", new ViewExecuteRequest()));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("View not found", ex.getMessage());
  }

  @Test
  public void executeViewInboxCustomUrlSuccess() {
    ViewExecuteResult expected = new ViewExecuteResult();
    expected.setViewName("Inbox");
    ViewResultItem row = new ViewResultItem();
    row.setId("guid-1");
    row.setName("Assignment");
    row.setTitle("Assignment");
    row.setFolderPath("//Sites/Demo");
    row.setType("Page");
    expected.setChildren(List.of(row));
    expected.setTotalCount(1);
    when(adaptor.executeView(eq("Inbox"), any())).thenReturn(expected);

    ViewExecuteResult out = resource.executeView("Inbox", new ViewExecuteRequest());
    assertSame(expected, out);
    assertEquals(1, out.getChildren().size());
    assertEquals("Assignment", out.getChildren().get(0).getTitle());
    verify(adaptor).executeView(eq("Inbox"), any());
  }

  @Test
  public void executeViewMapsIllegalArgumentTo400() {
    when(adaptor.executeView(eq("Custom"), any()))
        .thenThrow(new IllegalArgumentException("Unsupported custom URL view"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeView("Custom", new ViewExecuteRequest()));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Unsupported custom URL"));
  }

  @Test
  public void executeViewRethrowsServiceUnavailable() {
    WebApplicationException mapped =
        new WebApplicationException("View execute backend unavailable", 503);
    when(adaptor.executeView(eq("Inbox"), any())).thenThrow(mapped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeView("Inbox", new ViewExecuteRequest()));
    assertSame(mapped, ex);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void executeViewWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("engine down");
    when(adaptor.executeView(eq("All Content"), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeView("All Content", new ViewExecuteRequest()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnExecute() {
    ViewResource bare = new ViewResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.executeView("any", new ViewExecuteRequest()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void createViewClearsIdAndDelegates() {
    ViewDef body = new ViewDef();
    body.setName("MyView");
    body.setId(9);
    ViewDef created = new ViewDef();
    created.setName("MyView");
    created.setId(42);
    when(adaptor.createView(any())).thenReturn(created);

    ViewDef out = resource.createView(body);

    assertEquals("MyView", out.getName());
    assertEquals(0, body.getId());
    assertNull(body.getGuid());
    verify(adaptor).createView(body);
  }

  @Test
  public void createViewCustomUrlDelegates() {
    ViewDef body = new ViewDef();
    body.setName("MyCustom");
    body.setType("CustomView");
    body.setCustomView(true);
    body.setUrl("../myApp/page.xml");
    ViewDef created = new ViewDef();
    created.setName("MyCustom");
    created.setUrl("../myApp/page.xml");
    created.setCustomView(true);
    when(adaptor.createView(any())).thenReturn(created);

    ViewDef out = resource.createView(body);

    assertEquals("MyCustom", out.getName());
    assertEquals("../myApp/page.xml", out.getUrl());
    assertTrue(out.isCustomView());
    verify(adaptor).createView(body);
  }

  @Test
  public void createViewBlankUrlIs400() {
    when(adaptor.createView(any()))
        .thenThrow(new IllegalArgumentException("Custom URL view requires a non-blank url"));
    ViewDef body = new ViewDef();
    body.setName("MyCustom");
    body.setType("CustomView");
    body.setUrl("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createView(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createViewBlankNameIs400() {
    when(adaptor.createView(any())).thenThrow(new IllegalArgumentException("name is required"));
    ViewDef body = new ViewDef();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createView(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createViewDuplicateIs409() {
    when(adaptor.createView(any()))
        .thenThrow(new WebApplicationException("View already exists: MyView", 409));
    ViewDef body = new ViewDef();
    body.setName("MyView");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createView(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createViewNonAdminIs403() {
    when(adaptor.createView(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    ViewDef body = new ViewDef();
    body.setName("MyView");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createView(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateViewDelegates() {
    ViewDef body = new ViewDef();
    body.setLabel("Updated");
    body.setDescription("desc");
    ViewDef updated = new ViewDef();
    updated.setName("MyView");
    updated.setLabel("Updated");
    updated.setDescription("desc");
    when(adaptor.saveView(eq("MyView"), eq(body))).thenReturn(updated);

    ViewDef out = resource.updateView("MyView", body);

    assertEquals("Updated", out.getLabel());
    assertEquals("desc", out.getDescription());
    verify(adaptor).saveView("MyView", body);
  }

  @Test
  public void updateViewUnknownIs404() {
    when(adaptor.saveView(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateView("missing", new ViewDef()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateViewNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateView("MyView", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).saveView(any(), any());
  }

  @Test
  public void updateViewCustomUrlDelegates() {
    ViewDef body = new ViewDef();
    body.setUrl("../myApp/updated.xml");
    body.setCustomView(true);
    ViewDef updated = new ViewDef();
    updated.setName("MyCustom");
    updated.setUrl("../myApp/updated.xml");
    updated.setCustomView(true);
    when(adaptor.saveView(eq("MyCustom"), eq(body))).thenReturn(updated);

    ViewDef out = resource.updateView("MyCustom", body);

    assertEquals("../myApp/updated.xml", out.getUrl());
    assertTrue(out.isCustomView());
    verify(adaptor).saveView("MyCustom", body);
  }

  @Test
  public void updateViewInboxIs409() {
    when(adaptor.saveView(eq("Inbox"), any()))
        .thenThrow(
            new WebApplicationException(
                "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
                409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateView("Inbox", new ViewDef()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteViewNoContent() {
    when(adaptor.deleteView(eq("MyView"))).thenReturn(true);

    Response r = resource.deleteView("MyView");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteView("MyView");
  }

  @Test
  public void deleteViewUnknownIs404() {
    when(adaptor.deleteView(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteView("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteViewNonAdminIs403() {
    when(adaptor.deleteView(eq("MyView")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteView("MyView"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void deleteViewInboxIs409() {
    when(adaptor.deleteView(eq("Inbox")))
        .thenThrow(
            new WebApplicationException(
                "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
                409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteView("Inbox"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnCreate() {
    ViewResource bare = new ViewResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createView(new ViewDef()));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
