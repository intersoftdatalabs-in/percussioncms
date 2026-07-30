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

package com.percussion.rest.relationshiptypes;

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
public class RelationshipTypeResourceTest {

  private IRelationshipTypeAdaptor adaptor;
  private RelationshipTypeResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IRelationshipTypeAdaptor.class);
    resource = new RelationshipTypeResource(adaptor);
  }

  @Test
  public void listRelationshipTypesDelegates() {
    RelationshipType t = new RelationshipType();
    t.setName("ActiveAssembly");
    when(adaptor.listRelationshipTypes()).thenReturn(List.of(t));

    List<RelationshipType> out = resource.listRelationshipTypes();
    assertEquals(1, out.size());
    assertEquals("ActiveAssembly", out.get(0).getName());
  }

  @Test
  public void listRelationshipTypesNullSafe() {
    when(adaptor.listRelationshipTypes()).thenReturn(null);
    assertTrue(resource.listRelationshipTypes().isEmpty());
  }

  @Test
  public void getRelationshipTypeDelegates() {
    RelationshipType t = new RelationshipType();
    t.setName("ActiveAssembly");
    when(adaptor.findRelationshipType(eq("ActiveAssembly"))).thenReturn(t);

    assertEquals("ActiveAssembly", resource.getRelationshipType("ActiveAssembly").getName());
    verify(adaptor).findRelationshipType("ActiveAssembly");
  }

  @Test
  public void getRelationshipTypeNotFoundIsGeneric404() {
    when(adaptor.findRelationshipType(eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getRelationshipType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Relationship type not found", ex.getMessage());
  }

  @Test
  public void getRelationshipTypeWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findRelationshipType(eq("ActiveAssembly"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getRelationshipType("ActiveAssembly"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    RelationshipTypeResource bare = new RelationshipTypeResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, bare::listRelationshipTypes);
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());

    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getRelationshipType("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());
  }
}
