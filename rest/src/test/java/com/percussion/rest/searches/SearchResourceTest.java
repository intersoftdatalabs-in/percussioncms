/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    when(adaptor.listSearches()).thenReturn(List.of(s));
    List<SearchDef> out = resource.listSearches();
    assertEquals(1, out.size());
    assertEquals("All Content", out.get(0).getName());
    verify(adaptor).listSearches();
  }

  @Test
  public void listSearchesNullSafe() {
    when(adaptor.listSearches()).thenReturn(null);
    assertTrue(resource.listSearches().isEmpty());
  }

  @Test
  public void listSearchesWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listSearches()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listSearches());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    SearchResource bare = new SearchResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listSearches);
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
}
