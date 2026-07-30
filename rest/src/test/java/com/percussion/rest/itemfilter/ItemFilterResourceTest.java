/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  }
}
