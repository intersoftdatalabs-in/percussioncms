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

package com.percussion.rest.sharedfields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
public class SharedFieldsResourceTest {

  private ISharedFieldsAdaptor adaptor;
  private SharedFieldsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISharedFieldsAdaptor.class);
    resource = new SharedFieldsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void listGroupsDelegatesToAdaptor() {
    SharedFieldGroupSummary g = new SharedFieldGroupSummary();
    g.setName("shared");
    when(adaptor.listGroups(any())).thenReturn(List.of(g));

    List<SharedFieldGroupSummary> out = resource.listGroups();

    assertEquals(1, out.size());
    assertEquals("shared", out.get(0).getName());
    verify(adaptor).listGroups(any());
  }

  @Test
  public void listGroupsWrapsFailures() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listGroups(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listGroups());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listGroupsWithoutInjectionFailsWithDiagnostic() {
    SharedFieldsResource bare = new SharedFieldsResource();
    WebApplicationException ex = assertThrows(WebApplicationException.class, bare::listGroups);
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }

  @Test
  public void getGroupDelegatesToAdaptor() {
    SharedFieldGroupDetail d = new SharedFieldGroupDetail();
    d.setName("shared");
    when(adaptor.getGroup(any(), eq("shared"))).thenReturn(d);

    assertEquals("shared", resource.getGroup("shared").getName());
    verify(adaptor).getGroup(any(), eq("shared"));
  }

  @Test
  public void getGroupNotFoundIsGeneric404() {
    when(adaptor.getGroup(any(), eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getGroup("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Shared field group not found", ex.getMessage());
  }

  @Test
  public void getGroupWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("object store down");
    when(adaptor.getGroup(any(), eq("shared"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getGroup("shared"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listGroupsForbiddenWhenNotAdmin() {
    when(adaptor.listGroups(any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listGroups());
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void getGroupForbiddenWhenNotAdmin() {
    when(adaptor.getGroup(any(), eq("shared")))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getGroup("shared"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void createGroupDelegatesToAdaptor() {
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    SharedFieldGroupDetail created = new SharedFieldGroupDetail();
    created.setName("custom");
    created.setFilename("custom.xml");
    when(adaptor.createGroup(any(), any())).thenReturn(created);

    assertEquals("custom", resource.createGroup(body).getName());
    verify(adaptor).createGroup(any(), eq(body));
  }

  @Test
  public void createGroupInvalidNameIs400() {
    when(adaptor.createGroup(any(), any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createGroup(new SharedFieldGroupDetail()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createGroupDuplicateIs409() {
    when(adaptor.createGroup(any(), any()))
        .thenThrow(new WebApplicationException("Shared field group already exists: custom", 409));
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createGroup(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createGroupLockConflictIs409() {
    when(adaptor.createGroup(any(), any()))
        .thenThrow(new SharedFieldDesignLockException("Could not save shared field group; locked by other"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createGroup(new SharedFieldGroupDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createGroupForbiddenWhenNotAdmin() {
    when(adaptor.createGroup(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createGroup(new SharedFieldGroupDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void createGroupWrapsUnexpectedFailures() {
    IllegalStateException boom = new IllegalStateException("design ws down");
    when(adaptor.createGroup(any(), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createGroup(new SharedFieldGroupDetail()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void updateGroupDelegatesToAdaptor() {
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setFilename("renamed.xml");
    SharedFieldGroupDetail updated = new SharedFieldGroupDetail();
    updated.setName("shared");
    updated.setFilename("renamed.xml");
    when(adaptor.updateGroup(any(), eq("shared"), any())).thenReturn(updated);

    assertEquals("renamed.xml", resource.updateGroup("shared", body).getFilename());
    verify(adaptor).updateGroup(any(), eq("shared"), eq(body));
  }

  @Test
  public void updateGroupNotFoundIsGeneric404() {
    when(adaptor.updateGroup(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateGroup("missing", new SharedFieldGroupDetail()));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Shared field group not found", ex.getMessage());
  }

  @Test
  public void updateGroupLockConflictIs409() {
    when(adaptor.updateGroup(any(), eq("shared"), any()))
        .thenThrow(new SharedFieldDesignLockException("design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateGroup("shared", new SharedFieldGroupDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void updateGroupUnknownFieldIs400() {
    when(adaptor.updateGroup(any(), eq("shared"), any()))
        .thenThrow(new IllegalArgumentException("Unknown field: missing"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateGroup("shared", new SharedFieldGroupDetail()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteGroupReturns204() {
    Response out = resource.deleteGroup("shared");
    assertEquals(204, out.getStatus());
    verify(adaptor).deleteGroup(any(), eq("shared"));
  }

  @Test
  public void deleteGroupNotFoundIs404() {
    doThrow(new SharedFieldNotFoundException("Shared field group not found"))
        .when(adaptor)
        .deleteGroup(any(), eq("missing"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteGroup("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteGroupLockConflictIs409() {
    doThrow(new SharedFieldDesignLockException("locked by other"))
        .when(adaptor)
        .deleteGroup(any(), eq("shared"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteGroup("shared"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteGroupForbiddenWhenNotAdmin() {
    doThrow(new WebApplicationException("Admin role required", 403))
        .when(adaptor)
        .deleteGroup(any(), eq("shared"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteGroup("shared"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailureLockMessageOnIllegalStateIs409() {
    WebApplicationException ex =
        SharedFieldsResource.mapWriteFailure(new IllegalStateException("object is not locked"));
    assertEquals(409, ex.getResponse().getStatus());
  }
}
