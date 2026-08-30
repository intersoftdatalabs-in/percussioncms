/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.itemfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.services.error.PSNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ItemFilterResourceTest {

  private IItemFilterAdaptor adaptor;
  private ItemFilterResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IItemFilterAdaptor.class);
    resource = new ItemFilterResource(adaptor);
  }

  @Test
  public void listItemFiltersDelegates() {
    ItemFilter f = new ItemFilter();
    f.setName("public");
    when(adaptor.getItemFilters()).thenReturn(List.of(f));

    List<ItemFilter> out = resource.listItemFilters();
    assertEquals(1, out.size());
    assertEquals("public", out.get(0).getName());
  }

  @Test
  public void listItemFiltersNullSafe() {
    when(adaptor.getItemFilters()).thenReturn(null);
    assertTrue(resource.listItemFilters().isEmpty());
  }

  @Test
  public void getItemFilterDelegates() {
    ItemFilter f = new ItemFilter();
    f.setName("public");
    when(adaptor.findItemFilter(eq("public"))).thenReturn(f);

    assertEquals("public", resource.getItemFilter("public").getName());
    verify(adaptor).findItemFilter("public");
  }

  @Test
  public void getItemFilterNotFoundIsGeneric404() {
    when(adaptor.findItemFilter(eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getItemFilter("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Item filter not found", ex.getMessage());
  }

  @Test
  public void getItemFilterWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findItemFilter(eq("public"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getItemFilter("public"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void createItemFilterClearsIdAndDelegates() {
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    body.setFilterId(guid("0-11-1"));
    ItemFilter created = new ItemFilter();
    created.setName("preview");
    created.setFilterId(guid("0-11-99"));
    when(adaptor.updateOrCreateItemFilter(any())).thenReturn(created);

    ItemFilter out = resource.createItemFilter(body);

    assertEquals("preview", out.getName());
    assertNull(body.getFilterId());
    verify(adaptor).updateOrCreateItemFilter(body);
  }

  @Test
  public void createItemFilterBlankNameIs400() {
    when(adaptor.updateOrCreateItemFilter(any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    ItemFilter body = new ItemFilter();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createItemFilter(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createItemFilterDuplicateIs409() {
    when(adaptor.updateOrCreateItemFilter(any()))
        .thenThrow(new WebApplicationException("Item filter already exists: preview", 409));
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createItemFilter(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createItemFilterNonAdminIs403() {
    when(adaptor.updateOrCreateItemFilter(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    ItemFilter body = new ItemFilter();
    body.setName("preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createItemFilter(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateItemFilterSetsIdFromLookup() {
    ItemFilter existing = new ItemFilter();
    existing.setName("preview");
    existing.setFilterId(guid("0-11-9"));
    when(adaptor.findItemFilter(eq("preview"))).thenReturn(existing);
    ItemFilter body = new ItemFilter();
    body.setDescription("updated");
    ItemFilterRuleDefinition rule = new ItemFilterRuleDefinition();
    rule.setName("sys_filterByPublishDate");
    body.setRules(Set.of(rule));
    ItemFilter updated = new ItemFilter();
    updated.setName("preview");
    updated.setDescription("updated");
    updated.setRules(Set.of(rule));
    when(adaptor.updateOrCreateItemFilter(any())).thenReturn(updated);

    ItemFilter out = resource.updateItemFilter("preview", body);

    assertEquals("updated", out.getDescription());
    assertEquals(1, out.getRules().size());
    assertEquals("0-11-9", body.getFilterId().getStringValue());
    verify(adaptor).updateOrCreateItemFilter(body);
  }

  @Test
  public void updateItemFilterUnknownIs404() {
    when(adaptor.findItemFilter(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateItemFilter("missing", new ItemFilter()));
    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).updateOrCreateItemFilter(any());
  }

  @Test
  public void updateItemFilterNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateItemFilter("preview", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteItemFilterNoContent() throws Exception {
    ItemFilter existing = new ItemFilter();
    existing.setName("preview");
    existing.setFilterId(guid("0-11-9"));
    when(adaptor.findItemFilter(eq("preview"))).thenReturn(existing);

    Response r = resource.deleteItemFilter("preview");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteItemFilter(existing.getFilterId());
  }

  @Test
  public void deleteItemFilterUnknownIs404() throws Exception {
    when(adaptor.findItemFilter(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteItemFilter("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).deleteItemFilter(any());
  }

  @Test
  public void deleteItemFilterInUseIs409() throws Exception {
    ItemFilter existing = new ItemFilter();
    existing.setName("preview");
    existing.setFilterId(guid("0-11-9"));
    when(adaptor.findItemFilter(eq("preview"))).thenReturn(existing);
    doThrow(
            new WebApplicationException(
                "Item filter is associated with a content list or other dependents", 409))
        .when(adaptor)
        .deleteItemFilter(any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteItemFilter("preview"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteItemFilterNotFoundFromAdaptorIs404() throws Exception {
    ItemFilter existing = new ItemFilter();
    existing.setName("preview");
    existing.setFilterId(guid("0-11-9"));
    when(adaptor.findItemFilter(eq("preview"))).thenReturn(existing);
    doThrow(new PSNotFoundException("gone")).when(adaptor).deleteItemFilter(any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteItemFilter("preview"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteItemFilterNonAdminIs403() throws Exception {
    ItemFilter existing = new ItemFilter();
    existing.setName("preview");
    existing.setFilterId(guid("0-11-9"));
    when(adaptor.findItemFilter(eq("preview"))).thenReturn(existing);
    doThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN))
        .when(adaptor)
        .deleteItemFilter(any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteItemFilter("preview"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    ItemFilterResource bare = new ItemFilterResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listItemFilters);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());

    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getItemFilter("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());

    WebApplicationException createEx =
        assertThrows(WebApplicationException.class, () -> bare.createItemFilter(new ItemFilter()));
    assertEquals(500, createEx.getResponse().getStatus());
  }

  private static Guid guid(String value) {
    Guid g = new Guid();
    g.setStringValue(value);
    return g;
  }
}
