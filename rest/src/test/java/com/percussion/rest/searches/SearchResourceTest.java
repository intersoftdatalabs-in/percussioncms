/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

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
public class SearchResourceTest {

  private ISearchAdaptor adaptor;
  private SearchResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISearchAdaptor.class);
    resource = new SearchResource(adaptor);
  }

  @Test
  public void listSearchesSuccess() {
    SearchDef s = new SearchDef();
    s.setName("All Content");
    when(adaptor.listSearches(false)).thenReturn(List.of(s));
    List<SearchDef> out = resource.listSearches(false);
    assertEquals(1, out.size());
    assertEquals("All Content", out.get(0).getName());
    verify(adaptor).listSearches(false);
  }

  @Test
  public void listSearchesIncludeViewsDelegates() {
    SearchDef view = new SearchDef();
    view.setName("View_All");
    when(adaptor.listSearches(true)).thenReturn(List.of(view));
    List<SearchDef> out = resource.listSearches(true);
    assertEquals(1, out.size());
    assertEquals("View_All", out.get(0).getName());
    verify(adaptor).listSearches(true);
  }

  @Test
  public void listSearchesNullSafe() {
    when(adaptor.listSearches(false)).thenReturn(null);
    assertTrue(resource.listSearches(false).isEmpty());
  }

  @Test
  public void listSearchesWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listSearches(false)).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listSearches(false));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    SearchResource bare = new SearchResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listSearches(false));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getSearch must rethrow WebApplicationException from requireAdaptor (not re-wrap as 500)
    SearchResource bare = new SearchResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getSearch("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getSearchRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findSearchByKey(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSearch("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getSearchDelegates() {
    SearchDef s = new SearchDef();
    s.setName("All Content");
    when(adaptor.findSearchByKey(eq("All Content"))).thenReturn(s);
    assertEquals("All Content", resource.getSearch("All Content").getName());
    verify(adaptor).findSearchByKey("All Content");
  }

  @Test
  public void getSearchNotFoundIsGeneric404() {
    when(adaptor.findSearchByKey(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSearch("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Search not found", ex.getMessage());
  }

  @Test
  public void getSearchWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findSearchByKey(eq("All Content"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSearch("All Content"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void executeSearchSuccess() {
    SearchExecuteRequest req = new SearchExecuteRequest();
    req.setStartIndex(1);
    req.setMaxResults(10);
    SearchExecuteResult expected = new SearchExecuteResult();
    expected.setSearchName("All Content");
    expected.setTotalCount(0);
    when(adaptor.executeSearch(eq("All Content"), eq(req))).thenReturn(expected);

    SearchExecuteResult out = resource.executeSearch("All Content", req);
    assertSame(expected, out);
    assertEquals("All Content", out.getSearchName());
    verify(adaptor).executeSearch("All Content", req);
  }

  @Test
  public void executeSearchNullBodyDelegates() {
    SearchExecuteResult expected = new SearchExecuteResult();
    expected.setSearchName("All Content");
    when(adaptor.executeSearch(eq("All Content"), isNull())).thenReturn(expected);

    assertEquals("All Content", resource.executeSearch("All Content", null).getSearchName());
    verify(adaptor).executeSearch("All Content", null);
  }

  @Test
  public void executeSearchNotFoundIsGeneric404() {
    when(adaptor.executeSearch(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeSearch("missing", new SearchExecuteRequest()));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Search not found", ex.getMessage());
  }

  @Test
  public void executeSearchMapsIllegalArgumentTo400() {
    when(adaptor.executeSearch(eq("Custom"), any()))
        .thenThrow(new IllegalArgumentException("Custom URL searches cannot be executed"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeSearch("Custom", new SearchExecuteRequest()));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Custom URL"));
  }

  @Test
  public void executeSearchWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("engine down");
    when(adaptor.executeSearch(eq("All Content"), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.executeSearch("All Content", new SearchExecuteRequest()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnExecute() {
    SearchResource bare = new SearchResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.executeSearch("any", new SearchExecuteRequest()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void createSearchClearsIdAndDelegates() {
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    body.setId(9);
    SearchDef created = new SearchDef();
    created.setName("MySearch");
    created.setId(42);
    when(adaptor.createSearch(any())).thenReturn(created);

    SearchDef out = resource.createSearch(body);

    assertEquals("MySearch", out.getName());
    assertEquals(0, body.getId());
    assertNull(body.getGuid());
    verify(adaptor).createSearch(body);
  }

  @Test
  public void createSearchBlankNameIs400() {
    when(adaptor.createSearch(any())).thenThrow(new IllegalArgumentException("name is required"));
    SearchDef body = new SearchDef();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createSearch(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createSearchDuplicateIs409() {
    when(adaptor.createSearch(any()))
        .thenThrow(new WebApplicationException("Search already exists: MySearch", 409));
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createSearch(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createSearchNonAdminIs403() {
    when(adaptor.createSearch(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createSearch(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateSearchDelegates() {
    SearchDef body = new SearchDef();
    body.setLabel("Updated");
    body.setDescription("desc");
    SearchDef updated = new SearchDef();
    updated.setName("MySearch");
    updated.setLabel("Updated");
    updated.setDescription("desc");
    when(adaptor.saveSearch(eq("MySearch"), eq(body))).thenReturn(updated);

    SearchDef out = resource.updateSearch("MySearch", body);

    assertEquals("Updated", out.getLabel());
    assertEquals("desc", out.getDescription());
    verify(adaptor).saveSearch("MySearch", body);
  }

  @Test
  public void updateSearchUnknownIs404() {
    when(adaptor.saveSearch(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateSearch("missing", new SearchDef()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateSearchNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateSearch("MySearch", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).saveSearch(any(), any());
  }

  @Test
  public void deleteSearchNoContent() {
    when(adaptor.deleteSearch(eq("MySearch"))).thenReturn(true);

    Response r = resource.deleteSearch("MySearch");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteSearch("MySearch");
  }

  @Test
  public void deleteSearchUnknownIs404() {
    when(adaptor.deleteSearch(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteSearch("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteSearchNonAdminIs403() {
    when(adaptor.deleteSearch(eq("MySearch")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteSearch("MySearch"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnCreate() {
    SearchResource bare = new SearchResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createSearch(new SearchDef()));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
