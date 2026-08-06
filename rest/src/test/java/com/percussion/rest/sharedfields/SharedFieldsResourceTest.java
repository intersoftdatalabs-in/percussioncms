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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
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
  public void setUp() throws Exception {
    adaptor = mock(ISharedFieldsAdaptor.class);
    resource = new SharedFieldsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = SharedFieldsResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
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
}
