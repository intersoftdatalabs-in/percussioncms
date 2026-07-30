/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.searches;

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
public class SearchResourceTest {

  private ISearchAdaptor adaptor;
  private SearchResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISearchAdaptor.class);
    resource = new SearchResource(adaptor);
  }

  @Test
  public void listSearchesDelegates() {
    SearchDef s = new SearchDef();
    s.setName("All Content");
    when(adaptor.listSearches()).thenReturn(List.of(s));
    List<SearchDef> out = resource.listSearches();
    assertEquals(1, out.size());
    assertEquals("All Content", out.get(0).getName());
  }

  @Test
  public void listSearchesNullSafe() {
    when(adaptor.listSearches()).thenReturn(null);
    assertTrue(resource.listSearches().isEmpty());
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
  public void withoutInjectionFailsWithDiagnostic() {
    SearchResource bare = new SearchResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listSearches);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());
    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getSearch("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());
  }
}
