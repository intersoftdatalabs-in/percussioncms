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

package com.percussion.rest.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.contenttypes.NamedObjectRefList;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class WorkflowsResourceTest {

  private IWorkflowsAdaptor adaptor;
  private WorkflowsResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @BeforeEach
  public void setUp() {
    previousLog = WorkflowsResource.log;
    mockLog = mock(Logger.class);
    WorkflowsResource.log = mockLog;

    adaptor = mock(IWorkflowsAdaptor.class);
    resource = new WorkflowsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @AfterEach
  public void restoreLog() {
    WorkflowsResource.log = previousLog;
  }

  @Test
  public void getAllowedContentTypesSuccess() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("percPage");
    when(adaptor.getAllowedContentTypes(any(), eq("Simple Workflow"))).thenReturn(List.of(ref));

    NamedObjectRefList out = resource.getAllowedContentTypes("Simple Workflow");
    assertEquals(1, out.size());
    assertEquals("percPage", out.get(0).getName());
    verify(adaptor).getAllowedContentTypes(any(), eq("Simple Workflow"));
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getAllowedContentTypesEmpty() {
    when(adaptor.getAllowedContentTypes(any(), eq("Simple Workflow"))).thenReturn(List.of());
    assertTrue(resource.getAllowedContentTypes("Simple Workflow").isEmpty());
  }

  @Test
  public void getAllowedContentTypesNotFound() {
    when(adaptor.getAllowedContentTypes(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getAllowedContentTypes("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getAllowedContentTypesForbidden() {
    when(adaptor.getAllowedContentTypes(any(), eq("Simple Workflow")))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getAllowedContentTypes("Simple Workflow"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void getAllowedContentTypesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("cms down");
    when(adaptor.getAllowedContentTypes(any(), eq("Simple Workflow"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getAllowedContentTypes("Simple Workflow"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
    verify(mockLog)
        .error(
            eq("Failed to list workflow allowed content types ({}): {}"),
            eq(IllegalStateException.class.getName()),
            eq("cms down"),
            same(boom));
  }

  @Test
  public void missingAdaptorReturns503OnGet() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.getAllowedContentTypes("Simple Workflow"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void setAllowedContentTypesSuccess() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("percPage");
    WorkflowContentTypes body = new WorkflowContentTypes();
    body.setAllowedContentTypes(List.of(ref));
    when(adaptor.setAllowedContentTypes(any(), eq("Simple Workflow"), any()))
        .thenReturn(List.of(ref));

    NamedObjectRefList out = resource.setAllowedContentTypes("Simple Workflow", body);
    assertEquals(1, out.size());
    assertEquals("percPage", out.get(0).getName());
    verify(adaptor).setAllowedContentTypes(any(), eq("Simple Workflow"), eq(List.of(ref)));
  }

  @Test
  public void setAllowedContentTypesEmptyClears() {
    WorkflowContentTypes body = new WorkflowContentTypes();
    body.setAllowedContentTypes(List.of());
    when(adaptor.setAllowedContentTypes(any(), eq("Simple Workflow"), eq(List.of())))
        .thenReturn(List.of());
    assertTrue(resource.setAllowedContentTypes("Simple Workflow", body).isEmpty());
  }

  @Test
  public void setAllowedContentTypesRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setAllowedContentTypes("Simple Workflow", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void setAllowedContentTypesRequiresList() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setAllowedContentTypes("Simple Workflow", new WorkflowContentTypes()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void setAllowedContentTypesNotFound() {
    WorkflowContentTypes body = new WorkflowContentTypes();
    body.setAllowedContentTypes(List.of());
    when(adaptor.setAllowedContentTypes(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.setAllowedContentTypes("missing", body));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void setAllowedContentTypesInvalidContentTypeIs400() {
    WorkflowContentTypes body = new WorkflowContentTypes();
    body.setAllowedContentTypes(List.of(new NamedObjectRef()));
    when(adaptor.setAllowedContentTypes(any(), eq("Simple Workflow"), any()))
        .thenThrow(new IllegalArgumentException("allowedContentTypes[0] requires name or guid"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setAllowedContentTypes("Simple Workflow", body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void setAllowedContentTypesLockConflictIs409() {
    WorkflowContentTypes body = new WorkflowContentTypes();
    body.setAllowedContentTypes(List.of());
    when(adaptor.setAllowedContentTypes(any(), eq("Simple Workflow"), any()))
        .thenThrow(new WorkflowContentTypesDesignLockException("locked by other"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setAllowedContentTypes("Simple Workflow", body));
    assertEquals(409, ex.getResponse().getStatus());
  }
}
