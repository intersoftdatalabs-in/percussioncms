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

package com.percussion.rest.relationshiptypes;

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
    verify(adaptor).listRelationshipTypes();
  }

  @Test
  public void listRelationshipTypesNullSafe() {
    when(adaptor.listRelationshipTypes()).thenReturn(null);
    assertTrue(resource.listRelationshipTypes().isEmpty());
  }

  @Test
  public void listRelationshipTypesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listRelationshipTypes()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listRelationshipTypes());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
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
        assertThrows(WebApplicationException.class, () -> resource.getRelationshipType("missing"));
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
  public void getRelationshipTypeRethrowsWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("from adaptor", 404);
    when(adaptor.findRelationshipType(eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getRelationshipType("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    RelationshipTypeResource bare = new RelationshipTypeResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listRelationshipTypes);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getRelationshipType must rethrow WebApplicationException from requireAdaptor (not re-wrap as
    // 500)
    RelationshipTypeResource bare = new RelationshipTypeResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getRelationshipType("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void createRelationshipTypeDelegates() {
    RelationshipType body = userBody("MyUserRel");
    RelationshipType created = userBody("MyUserRel");
    created.setUserType(true);
    when(adaptor.createRelationshipType(eq(body))).thenReturn(created);

    RelationshipType out = resource.createRelationshipType(body);

    assertEquals("MyUserRel", out.getName());
    assertTrue(out.isUserType());
    verify(adaptor).createRelationshipType(eq(body));
  }

  @Test
  public void createRelationshipTypeBlankNameIs400() {
    when(adaptor.createRelationshipType(any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createRelationshipType(new RelationshipType()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createRelationshipTypeDuplicateIs409() {
    when(adaptor.createRelationshipType(any()))
        .thenThrow(new WebApplicationException("Relationship type already exists: MyUserRel", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createRelationshipType(userBody("MyUserRel")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createRelationshipTypeNonAdminIs403() {
    when(adaptor.createRelationshipType(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createRelationshipType(userBody("MyUserRel")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateRelationshipTypeDelegates() {
    RelationshipType body = userBody("MyUserRel");
    body.setLabel("Updated");
    RelationshipType updated = userBody("MyUserRel");
    updated.setLabel("Updated");
    when(adaptor.updateRelationshipType(eq("MyUserRel"), eq(body))).thenReturn(updated);

    RelationshipType out = resource.updateRelationshipType("MyUserRel", body);

    assertEquals("Updated", out.getLabel());
    verify(adaptor).updateRelationshipType(eq("MyUserRel"), eq(body));
  }

  @Test
  public void updateRelationshipTypeUnknownIs404() {
    when(adaptor.updateRelationshipType(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRelationshipType("missing", new RelationshipType()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateRelationshipTypeNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateRelationshipType("MyUserRel", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateRelationshipType(any(), any());
  }

  @Test
  public void updateRelationshipTypeSystemIs409() {
    when(adaptor.updateRelationshipType(eq("ActiveAssembly"), any()))
        .thenThrow(
            new WebApplicationException(
                "System relationship types cannot be updated or deleted", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRelationshipType("ActiveAssembly", new RelationshipType()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteRelationshipTypeDelegates() {
    when(adaptor.deleteRelationshipType(eq("MyUserRel"))).thenReturn(true);
    Response resp = resource.deleteRelationshipType("MyUserRel");
    assertEquals(204, resp.getStatus());
    verify(adaptor).deleteRelationshipType("MyUserRel");
  }

  @Test
  public void deleteRelationshipTypeUnknownIs404() {
    when(adaptor.deleteRelationshipType(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteRelationshipType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteRelationshipTypeSystemIs409() {
    when(adaptor.deleteRelationshipType(eq("ActiveAssembly")))
        .thenThrow(
            new WebApplicationException(
                "System relationship types cannot be updated or deleted", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteRelationshipType("ActiveAssembly"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static RelationshipType userBody(String name) {
    RelationshipType t = new RelationshipType();
    t.setName(name);
    t.setCategory("rs_activeassembly");
    t.setLabel(name);
    return t;
  }
}
