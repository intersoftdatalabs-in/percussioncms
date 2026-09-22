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
import static org.mockito.Mockito.doThrow;
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
  public void setAllowedContentTypesNullListClearsLikeEmpty() {
    // Live CXF often deserializes JSON [] as null under UNWRAP_ROOT_VALUE.
    when(adaptor.setAllowedContentTypes(any(), eq("Simple Workflow"), eq(List.of())))
        .thenReturn(List.of());
    assertTrue(
        resource.setAllowedContentTypes("Simple Workflow", new WorkflowContentTypes()).isEmpty());
    verify(adaptor).setAllowedContentTypes(any(), eq("Simple Workflow"), eq(List.of()));
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

  private static WorkflowCreate createBody(String name) {
    WorkflowCreate body = new WorkflowCreate();
    body.setName(name);
    return body;
  }

  private static WorkflowSummary createdSummary(String name) {
    WorkflowSummary summary = new WorkflowSummary();
    summary.setWorkflowName(name);
    summary.setWorkflowDescription("");
    summary.setDefaultWorkflow(false);
    return summary;
  }

  @Test
  public void createWorkflowSuccess() {
    when(adaptor.createWorkflow(any(), any())).thenReturn(createdSummary("Nightly QA"));
    WorkflowSummary out = resource.createWorkflow(createBody("Nightly QA"));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).createWorkflow(any(), any());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void createWorkflowRequiresBody() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createWorkflow(null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createWorkflow(any(), any());
  }

  @Test
  public void createWorkflowInvalidNameIs400() {
    when(adaptor.createWorkflow(any(), any()))
        .thenThrow(new IllegalArgumentException("Workflow name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createWorkflow(createBody("  ")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createWorkflowDuplicateIs409() {
    when(adaptor.createWorkflow(any(), any()))
        .thenThrow(new WebApplicationException("Workflow already exists: Simple Workflow", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createWorkflow(createBody("Simple Workflow")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createWorkflowForbidden() {
    when(adaptor.createWorkflow(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createWorkflow(createBody("Nightly QA")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503OnCreate() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createWorkflow(createBody("Nightly QA")));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static WorkflowUpdate updateBody(String name, String description) {
    WorkflowUpdate body = new WorkflowUpdate();
    body.setName(name);
    body.setDescription(description);
    return body;
  }

  @Test
  public void updateWorkflowSuccess() {
    when(adaptor.updateWorkflow(any(), eq("Simple Workflow"), any()))
        .thenReturn(createdSummary("Simple Workflow"));
    WorkflowSummary out = resource.updateWorkflow("Simple Workflow", updateBody("Simple Workflow", "Edited"));
    assertEquals("Simple Workflow", out.getWorkflowName());
    verify(adaptor).updateWorkflow(any(), eq("Simple Workflow"), any());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void updateWorkflowRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflow("Simple Workflow", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateWorkflow(any(), any(String.class), any());
  }

  @Test
  public void updateWorkflowInvalidNameIs400() {
    when(adaptor.updateWorkflow(any(), any(String.class), any()))
        .thenThrow(new IllegalArgumentException("Workflow update body name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflow("Simple Workflow", updateBody("Other", "Edited")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowNotFoundIs404() {
    when(adaptor.updateWorkflow(any(), eq("missing"), any()))
        .thenThrow(new WebApplicationException("Workflow not found: missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflow("missing", updateBody("missing", "Edited")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowForbidden() {
    when(adaptor.updateWorkflow(any(), any(String.class), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflow("Simple Workflow", updateBody("Simple Workflow", "Edited")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowWrapsUnexpectedAs500() {
    // mapMutationFailure maps RuntimeException to 500 without logging
    // (the create/update/delete handlers log only non-Runtime failures).
    IllegalStateException boom = new IllegalStateException("cms down");
    when(adaptor.updateWorkflow(any(), any(String.class), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflow("Simple Workflow", updateBody("Simple Workflow", "Edited")));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturns503OnUpdate() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.updateWorkflow("Simple Workflow", updateBody("Simple Workflow", "Edited")));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void deleteWorkflowSuccess() {
    resource.deleteWorkflow("Simple Workflow");
    verify(adaptor).deleteWorkflow(any(), eq("Simple Workflow"));
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void deleteWorkflowForwardsUuidAndGuidToAdaptor() {
    resource.deleteWorkflow("4");
    verify(adaptor).deleteWorkflow(any(), eq("4"));
    resource.deleteWorkflow("0-23-4");
    verify(adaptor).deleteWorkflow(any(), eq("0-23-4"));
  }

  @Test
  public void deleteWorkflowNotFoundIs404() {
    doThrow(new WebApplicationException("Workflow not found: missing", 404))
        .when(adaptor)
        .deleteWorkflow(any(), eq("missing"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflow("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteWorkflowConflictIs409() {
    doThrow(new WebApplicationException("Workflow is a system workflow", 409))
        .when(adaptor)
        .deleteWorkflow(any(), eq("LocalContent"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflow("LocalContent"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteWorkflowForbidden() {
    doThrow(new WebApplicationException("Admin role required", 403))
        .when(adaptor)
        .deleteWorkflow(any(), any(String.class));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflow("Simple Workflow"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void deleteWorkflowInvalidIdOrNameIs400() {
    doThrow(new IllegalArgumentException("idOrName must not contain wildcards"))
        .when(adaptor)
        .deleteWorkflow(any(), any(String.class));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflow("wild*card"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteWorkflowWrapsUnexpectedAs500() {
    // mapMutationFailure maps RuntimeException to 500 without logging
    // (the create/update/delete handlers log only non-Runtime failures).
    IllegalStateException boom = new IllegalStateException("cms down");
    doThrow(boom).when(adaptor).deleteWorkflow(any(), any(String.class));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflow("Simple Workflow"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturns503OnDelete() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.deleteWorkflow("Simple Workflow"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static WorkflowStepWrite stepBody(String name) {
    WorkflowStepWrite body = new WorkflowStepWrite();
    body.setName(name);
    body.setAfterStep("Draft");
    return body;
  }

  @Test
  public void createWorkflowStepSuccess() {
    when(adaptor.createWorkflowStep(any(), eq("Nightly QA"), any()))
        .thenReturn(createdSummary("Nightly QA"));
    WorkflowSummary out = resource.createWorkflowStep("Nightly QA", stepBody("Review"));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).createWorkflowStep(any(), eq("Nightly QA"), any());
  }

  @Test
  public void createWorkflowStepRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createWorkflowStep("Nightly QA", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createWorkflowStep(any(), any(), any());
  }

  @Test
  public void createWorkflowStepInvalidIs400() {
    when(adaptor.createWorkflowStep(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Step name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createWorkflowStep("Nightly QA", stepBody("  ")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createWorkflowStepPackagedIs403() {
    when(adaptor.createWorkflowStep(any(), any(), any()))
        .thenThrow(new WebApplicationException("Packaged or default workflows cannot be modified from this surface", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createWorkflowStep("Default Workflow", stepBody("Review")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowStepSuccess() {
    when(adaptor.updateWorkflowStep(any(), eq("Nightly QA"), eq("Review"), any()))
        .thenReturn(createdSummary("Nightly QA"));
    WorkflowSummary out =
        resource.updateWorkflowStep("Nightly QA", "Review", stepBody("Review 2"));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).updateWorkflowStep(any(), eq("Nightly QA"), eq("Review"), any());
  }

  @Test
  public void updateWorkflowStepRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflowStep("Nightly QA", "Review", null));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
