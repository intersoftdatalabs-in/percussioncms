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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
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
  public void copyWorkflowSuccess() {
    when(adaptor.copyWorkflow(any(), eq("Simple Workflow"), any()))
        .thenReturn(createdSummary("Nightly Copy"));
    WorkflowSummary out = resource.copyWorkflow("Simple Workflow", createBody("Nightly Copy"));
    assertEquals("Nightly Copy", out.getWorkflowName());
    verify(adaptor).copyWorkflow(any(), eq("Simple Workflow"), any());
  }

  @Test
  public void copyWorkflowRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.copyWorkflow("Simple Workflow", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).copyWorkflow(any(), any(), any());
  }

  @Test
  public void copyWorkflowDuplicateIs409() {
    when(adaptor.copyWorkflow(any(), any(), any()))
        .thenThrow(new WebApplicationException("Workflow already exists: Nightly Copy", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.copyWorkflow("Simple Workflow", createBody("Nightly Copy")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void copyWorkflowMissingSourceIs404() {
    when(adaptor.copyWorkflow(any(), any(), any()))
        .thenThrow(new WebApplicationException("Workflow not found: Missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.copyWorkflow("Missing", createBody("Nightly Copy")));
    assertEquals(404, ex.getResponse().getStatus());
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
  public void setDefaultWorkflowSuccess() {
    when(adaptor.setDefaultWorkflow(any(), eq("Simple Workflow")))
        .thenReturn(createdSummary("Simple Workflow"));
    WorkflowSummary out = resource.setDefaultWorkflow("Simple Workflow");
    assertEquals("Simple Workflow", out.getWorkflowName());
    verify(adaptor).setDefaultWorkflow(any(), eq("Simple Workflow"));
  }

  @Test
  public void setDefaultWorkflowNotFound() {
    when(adaptor.setDefaultWorkflow(any(), eq("missing")))
        .thenThrow(new WebApplicationException("Workflow not found: missing", 404));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.setDefaultWorkflow("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503OnSetDefault() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.setDefaultWorkflow("Simple Workflow"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static WorkflowRename renameBody(String name) {
    WorkflowRename body = new WorkflowRename();
    body.setName(name);
    return body;
  }

  @Test
  public void renameWorkflowSuccess() {
    when(adaptor.renameWorkflow(any(), eq("Nightly QA"), any()))
        .thenReturn(createdSummary("Nightly QA 2"));
    WorkflowSummary out = resource.renameWorkflow("Nightly QA", renameBody("Nightly QA 2"));
    assertEquals("Nightly QA 2", out.getWorkflowName());
    verify(adaptor).renameWorkflow(any(), eq("Nightly QA"), any());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void renameWorkflowRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.renameWorkflow("Nightly QA", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).renameWorkflow(any(), any(String.class), any());
  }

  @Test
  public void renameWorkflowInvalidNameIs400() {
    when(adaptor.renameWorkflow(any(), any(String.class), any()))
        .thenThrow(new IllegalArgumentException("Invalid character in workflow name"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameWorkflow("Nightly QA", renameBody("Bad!")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void renameWorkflowDuplicateIs409() {
    when(adaptor.renameWorkflow(any(), any(String.class), any()))
        .thenThrow(new WebApplicationException("Workflow already exists: Simple Workflow", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameWorkflow("Nightly QA", renameBody("Simple Workflow")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void renameWorkflowPackagedIs403() {
    when(adaptor.renameWorkflow(any(), any(String.class), any()))
        .thenThrow(
            new WebApplicationException(
                "Packaged or default workflows cannot be modified from this surface", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameWorkflow("Default Workflow", renameBody("Other")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void renameWorkflowNotFoundIs404() {
    when(adaptor.renameWorkflow(any(), eq("missing"), any()))
        .thenThrow(new WebApplicationException("Workflow not found: missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameWorkflow("missing", renameBody("Other")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503OnRename() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.renameWorkflow("Nightly QA", renameBody("Nightly QA 2")));
    assertEquals(503, ex.getResponse().getStatus());
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
  public void getWorkflowGraphSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Simple Workflow");
    graph.setPackaged(true);
    when(adaptor.getWorkflowGraph(any(), eq("Simple Workflow"))).thenReturn(graph);
    WorkflowGraph out = resource.getWorkflowGraph("Simple Workflow");
    assertEquals("Simple Workflow", out.getWorkflowName());
    assertTrue(out.isPackaged());
    verify(adaptor).getWorkflowGraph(any(), eq("Simple Workflow"));
  }

  @Test
  public void getWorkflowGraphNotFoundIs404() {
    when(adaptor.getWorkflowGraph(any(), any()))
        .thenThrow(new WebApplicationException("Workflow not found", 404));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getWorkflowGraph("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getWorkflowGraphInvalidIs400() {
    when(adaptor.getWorkflowGraph(any(), any()))
        .thenThrow(new IllegalArgumentException("idOrName must not contain wildcards"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getWorkflowGraph("a*b"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503OnGraph() {
    WorkflowsResource bare = new WorkflowsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getWorkflowGraph("Simple Workflow"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowStepRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateWorkflowStep("Nightly QA", "Review", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteTransitionRequiresFromAndLabel() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.deleteWorkflowTransition("Nightly QA", " ", "Submit", "Review"));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).deleteWorkflowTransition(any(), any(), any(), any(), any());
  }

  @Test
  public void deleteTransitionMapsAmbiguousLabelTo400() {
    when(adaptor.deleteWorkflowTransition(any(), eq("Nightly QA"), eq("Draft"), eq("Submit"), eq(null)))
        .thenThrow(new IllegalArgumentException("specify to"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.deleteWorkflowTransition("Nightly QA", "Draft", "Submit", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteTransitionPassesThrough404() {
    WebApplicationException missing = new WebApplicationException("missing", 404);
    when(adaptor.deleteWorkflowTransition(any(), eq("Missing"), eq("Draft"), eq("Submit"), eq("Review")))
        .thenThrow(missing);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.deleteWorkflowTransition("Missing", "Draft", "Submit", "Review"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteStepRequiresName() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflowStep("Nightly QA", " "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).deleteWorkflowStep(any(), any(), any());
  }

  @Test
  public void deleteStepSuccessReturnsGraph() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.deleteWorkflowStep(any(), eq("Nightly QA"), eq("Orphan")))
        .thenReturn(graph);
    WorkflowGraph out = resource.deleteWorkflowStep("Nightly QA", "Orphan");
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).deleteWorkflowStep(any(), eq("Nightly QA"), eq("Orphan"));
  }

  @Test
  public void deleteStepConflictIs409() {
    when(adaptor.deleteWorkflowStep(any(), eq("Nightly QA"), eq("Draft")))
        .thenThrow(new WebApplicationException("still referenced", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteWorkflowStep("Nightly QA", "Draft"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowAgingTransitionWrite agingBody(String from, String to, long minutes) {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setIntervalMinutes(minutes);
    return body;
  }

  @Test
  public void createAgingTransitionSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.createAbsoluteAgingTransition(any(), eq("Nightly QA"), any())).thenReturn(graph);
    WorkflowGraph out =
        resource.createAbsoluteAgingTransition("Nightly QA", agingBody("Draft", "Review", 15));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).createAbsoluteAgingTransition(any(), eq("Nightly QA"), any());
  }

  @Test
  public void createAgingTransitionRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createAbsoluteAgingTransition(any(), any(), any());
  }

  @Test
  public void createAgingTransitionBlankDestinationIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", agingBody("Draft", " ", 15)));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createAbsoluteAgingTransition(any(), any(), any());
  }

  @Test
  public void createAgingTransitionNonPositiveIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", agingBody("Draft", "Review", 0)));
    assertEquals(400, ex.getResponse().getStatus());
    WebApplicationException negative =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", agingBody("Draft", "Review", -5)));
    assertEquals(400, negative.getResponse().getStatus());
    verify(adaptor, never()).createAbsoluteAgingTransition(any(), any(), any());
  }

  @Test
  public void createAgingTransitionPackagedIs403() {
    when(adaptor.createAbsoluteAgingTransition(any(), any(), any()))
        .thenThrow(new WebApplicationException("packaged", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.createAbsoluteAgingTransition(
                    "Default Workflow", agingBody("Draft", "Review", 15)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void createAgingTransitionConflictIs409() {
    when(adaptor.createAbsoluteAgingTransition(any(), any(), any()))
        .thenThrow(new WebApplicationException("exists", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", agingBody("Draft", "Review", 15)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createRepeatedAgingPassesTypeOnTheSameResource() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.createAbsoluteAgingTransition(any(), eq("Nightly QA"), any())).thenReturn(graph);
    WorkflowAgingTransitionWrite body = agingBody("Draft", "Review", 15);
    body.setType("REPEATED");
    WorkflowGraph out = resource.createAbsoluteAgingTransition("Nightly QA", body);
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor)
        .createAbsoluteAgingTransition(
            any(), eq("Nightly QA"), argThat(written -> "REPEATED".equals(written.getType())));
  }

  @Test
  public void createAgingRejectsBlankSystemFieldAndUnknownType() {
    WorkflowAgingTransitionWrite system = agingBody("Draft", "Review", 0);
    system.setType("SYSTEM_FIELD");
    WebApplicationException blank =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", system));
    assertEquals(400, blank.getResponse().getStatus());
    WorkflowAgingTransitionWrite unknownField = agingBody("Draft", "Review", 0);
    unknownField.setType("SYSTEM_FIELD");
    unknownField.setSystemField("sys_title");
    WebApplicationException unknownName =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", unknownField));
    assertEquals(400, unknownName.getResponse().getStatus());
    WorkflowAgingTransitionWrite unknown = agingBody("Draft", "Review", 15);
    unknown.setType("nope");
    WebApplicationException rejected =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createAbsoluteAgingTransition("Nightly QA", unknown));
    assertEquals(400, rejected.getResponse().getStatus());
    verify(adaptor, never()).createAbsoluteAgingTransition(any(), any(), any());
  }

  @Test
  public void createSystemFieldAgingPassesFieldOnTheSameResource() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.createAbsoluteAgingTransition(any(), eq("Nightly QA"), any())).thenReturn(graph);
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setType("SYSTEM_FIELD");
    body.setSystemField("contentstartdate");
    WorkflowGraph out = resource.createAbsoluteAgingTransition("Nightly QA", body);
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor)
        .createAbsoluteAgingTransition(
            any(),
            eq("Nightly QA"),
            argThat(
                written ->
                    "SYSTEM_FIELD".equals(written.getType())
                        && "contentstartdate".equals(written.getSystemField())));
  }

  private static WorkflowAgingIntervalWrite intervalBody(
      String from, String to, long currentMinutes, long newMinutes) {
    WorkflowAgingIntervalWrite body = new WorkflowAgingIntervalWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setIntervalMinutes(currentMinutes);
    body.setNewIntervalMinutes(newMinutes);
    return body;
  }

  @Test
  public void changeAgingIntervalSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.changeAbsoluteAgingInterval(any(), eq("Nightly QA"), any())).thenReturn(graph);
    WorkflowGraph out =
        resource.changeAbsoluteAgingInterval("Nightly QA", intervalBody("Draft", "Review", 15, 30));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).changeAbsoluteAgingInterval(any(), eq("Nightly QA"), any());
  }

  @Test
  public void changeAgingIntervalRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.changeAbsoluteAgingInterval("Nightly QA", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).changeAbsoluteAgingInterval(any(), any(), any());
  }

  @Test
  public void changeAgingIntervalRejectsBlankOrNonPositiveOrUnchanged() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.changeAbsoluteAgingInterval(
                        "Nightly QA", intervalBody("Draft", " ", 15, 30)))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.changeAbsoluteAgingInterval(
                        "Nightly QA", intervalBody("Draft", "Review", 15, 0)))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.changeAbsoluteAgingInterval(
                        "Nightly QA", intervalBody("Draft", "Review", 15, -5)))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.changeAbsoluteAgingInterval(
                        "Nightly QA", intervalBody("Draft", "Review", 15, 15)))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).changeAbsoluteAgingInterval(any(), any(), any());
  }

  @Test
  public void changeAgingIntervalPackagedIs403() {
    when(adaptor.changeAbsoluteAgingInterval(any(), any(), any()))
        .thenThrow(new WebApplicationException("packaged", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.changeAbsoluteAgingInterval(
                    "Default Workflow", intervalBody("Draft", "Review", 15, 30)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void changeAgingIntervalConflictIs409() {
    when(adaptor.changeAbsoluteAgingInterval(any(), any(), any()))
        .thenThrow(new WebApplicationException("exists", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.changeAbsoluteAgingInterval(
                    "Nightly QA", intervalBody("Draft", "Review", 15, 30)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteAgingTransitionSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.deleteAbsoluteAgingTransition(
            any(), eq("Nightly QA"), eq("Draft"), eq("Review"), eq(15L)))
        .thenReturn(graph);
    WorkflowGraph out =
        resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", "Review", "15");
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor)
        .deleteAbsoluteAgingTransition(any(), eq("Nightly QA"), eq("Draft"), eq("Review"), eq(15L));
  }

  @Test
  public void deleteAgingTransitionRejectsBlankOrNonPositive() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.deleteAbsoluteAgingTransition("Nightly QA", " ", "Review", "15"))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", " ", "15"))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", "Review", "0"))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", "Review", "-5"))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", "Review", "no"))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).deleteAbsoluteAgingTransition(any(), any(), any(), any(), anyLong());
  }

  @Test
  public void deleteAgingTransitionPackagedIs403() {
    when(adaptor.deleteAbsoluteAgingTransition(any(), any(), any(), any(), anyLong()))
        .thenThrow(new WebApplicationException("packaged", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.deleteAbsoluteAgingTransition("Default Workflow", "Draft", "Review", "15"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void deleteAgingTransitionNotAbsoluteIs409() {
    when(adaptor.deleteAbsoluteAgingTransition(any(), any(), any(), any(), anyLong()))
        .thenThrow(new WebApplicationException("repeated", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.deleteAbsoluteAgingTransition("Nightly QA", "Draft", "Review", "15"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowTransitionWrite transitionBody(String from, String to, String label) {
    WorkflowTransitionWrite body = new WorkflowTransitionWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setLabel(label);
    return body;
  }

  @Test
  public void createWorkflowTransitionSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.createWorkflowTransition(any(), eq("Nightly QA"), any())).thenReturn(graph);
    WorkflowGraph out =
        resource.createWorkflowTransition("Nightly QA", transitionBody("Draft", "Review", "Send"));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor).createWorkflowTransition(any(), eq("Nightly QA"), any());
  }

  @Test
  public void createWorkflowTransitionRequiresBody() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createWorkflowTransition("Nightly QA", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createWorkflowTransition(any(), any(), any());
  }

  @Test
  public void createWorkflowTransitionBlankIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createWorkflowTransition("Nightly QA", transitionBody("Draft", " ", "Send")));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createWorkflowTransition(any(), any(), any());
  }

  @Test
  public void createWorkflowTransitionInvalidIs400() {
    when(adaptor.createWorkflowTransition(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Invalid character in label"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createWorkflowTransition("Nightly QA", transitionBody("Draft", "Review", "Send")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createWorkflowTransitionPackagedIs403() {
    when(adaptor.createWorkflowTransition(any(), any(), any()))
        .thenThrow(new WebApplicationException("packaged", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.createWorkflowTransition(
                    "Default Workflow", transitionBody("Draft", "Review", "Send")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowTransitionSuccess() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    when(adaptor.updateWorkflowTransition(
            any(), eq("Nightly QA"), eq("Draft"), eq("Submit"), eq("Review"), any()))
        .thenReturn(graph);
    WorkflowGraph out =
        resource.updateWorkflowTransition(
            "Nightly QA", "Draft", "Submit", "Review", transitionBody(null, "Pending", "Send"));
    assertEquals("Nightly QA", out.getWorkflowName());
    verify(adaptor)
        .updateWorkflowTransition(any(), eq("Nightly QA"), eq("Draft"), eq("Submit"), eq("Review"), any());
  }

  @Test
  public void updateWorkflowTransitionRequiresIdentity() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateWorkflowTransition(
                    "Nightly QA", " ", "Submit", "Review", transitionBody(null, "Pending", "Send")));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateWorkflowTransition(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void updateWorkflowTransitionAmbiguousIs400() {
    when(adaptor.updateWorkflowTransition(any(), any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("specify to"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateWorkflowTransition(
                    "Nightly QA", "Draft", "Submit", null, transitionBody(null, "Pending", "Send")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateWorkflowTransitionMissingIs404() {
    when(adaptor.updateWorkflowTransition(any(), any(), any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateWorkflowTransition(
                    "Missing", "Draft", "Submit", "Review", transitionBody(null, "Pending", "Send")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteStepInvalidNameIs400() {
    when(adaptor.deleteWorkflowStep(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("wildcards"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.deleteWorkflowStep("Nightly QA", "bad*name"));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
