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

import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.contenttypes.NamedObjectRefList;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST for workflow → content-type associations (SY-06).
 *
 * <p>Peer of {@code ContentTypesResource} {@code .../allowedWorkflows} (CD-08). Distinct from the
 * stepped-workflow catalog under {@code /services/workflowmanagement/workflows}.
 */
@PSSiteManageBean(value = "restWorkflowsResource")
@Path("/workflows")
@XmlRootElement
@Tag(name = "Workflows", description = "Workflow association operations (SY-06)")
public class WorkflowsResource {

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger} and assert
   * unexpected-failure diagnostics.
   */
  static Logger log = LogManager.getLogger(WorkflowsResource.class);

  private final IWorkflowsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public WorkflowsResource() {
    this.adaptor = null;
  }

  @Autowired
  public WorkflowsResource(IWorkflowsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  private IWorkflowsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Workflows adaptor not configured (resource constructed without injection)", 503);
    }
    return adaptor;
  }

  private static NamedObjectRefList asNamedObjectRefList(List<NamedObjectRef> items) {
    return items instanceof NamedObjectRefList list
        ? list
        : new NamedObjectRefList(items != null ? items : List.of());
  }

  private static WebApplicationException mapMutationFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof WorkflowContentTypesDesignLockException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      return new WebApplicationException(msg, 409);
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    return new WebApplicationException(e, 500);
  }

  @GET
  @Path("/{idOrName}/allowedContentTypes")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List allowed content types for a workflow",
      description =
          "SY-06 Admin GET: content types associated with the workflow (workflow → CT)."
              + " No design lock required. Empty list means none. Peer of CD-08 CT → workflow"
              + " associations on ContentTypeDetail.allowedWorkflows / PUT"
              + " /contenttypes/{id}/allowedWorkflows.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = NamedObjectRefList.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public NamedObjectRefList getAllowedContentTypes(@PathParam("idOrName") String idOrName) {
    try {
      List<NamedObjectRef> items =
          requireAdaptor().getAllowedContentTypes(uriInfo.getBaseUri(), idOrName);
      if (items == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return asNamedObjectRefList(items);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to list workflow allowed content types ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/allowedContentTypes")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace allowed content types for a workflow",
      description =
          "SY-06 Admin full-replace of content types associated with the workflow. Empty"
              + " allowedContentTypes clears associations for this workflow. Acquires a design"
              + " lock on each affected content type and releases it on save (unlike CD-08 CT →"
              + " workflow PUT, which requires a pre-held CT lock). Jackson root wrap is"
              + " WorkflowContentTypes. After PUT, GET on this path lists the new set.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced",
            content = @Content(schema = @Schema(implementation = NamedObjectRefList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "allowedContentTypes is required, or a content-type id is invalid"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock conflict on an affected content type"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public NamedObjectRefList setAllowedContentTypes(
      @PathParam("idOrName") String idOrName, WorkflowContentTypes body) {
    if (body == null) {
      throw new WebApplicationException("allowedContentTypes is required", 400);
    }
    // CXF Jackson UNWRAP of {"WorkflowContentTypes":{"allowedContentTypes":[]}} often yields
    // null for the list field; treat null as empty (clear associations) per SY-06.
    List<NamedObjectRef> allowed =
        body.getAllowedContentTypes() != null ? body.getAllowedContentTypes() : List.of();
    try {
      List<NamedObjectRef> items =
          requireAdaptor().setAllowedContentTypes(uriInfo.getBaseUri(), idOrName, allowed);
      if (items == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return asNamedObjectRefList(items);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to replace workflow allowed content types ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create a workflow",
      description =
          "Slice 21 Admin. Creates and persists a stepped workflow via"
              + " IPSSteppedWorkflowService.createWorkflow (same backend the workflow-admin"
              + " editor uses). Name is required, must be unique (case-insensitive), and must"
              + " match workflow-admin rules (letters, digits, underscore, hyphen, space; max 50"
              + " chars). Optional description is stored on the new workflow. States,"
              + " transitions, and roles come from the product base-workflow template. Returns"
              + " the new WorkflowSummary (GET workflowmanagement metadata then lists it)."
              + " Jackson root wrap is WorkflowCreate. Full graph design stays outside this"
              + " surface.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created and saved",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid name (blank, too long, or invalid characters)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "A workflow with that name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary createWorkflow(WorkflowCreate body) {
    if (body == null) {
      throw new WebApplicationException("Workflow name is required", 400);
    }
    try {
      return requireAdaptor().createWorkflow(uriInfo.getBaseUri(), body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to create workflow ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/copy")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Copy a workflow",
      description =
          "Slice 36 Admin. Copies an existing workflow, including states and transitions, under"
              + " a new unique name. The source is not modified. Name rules match create"
              + " (letters, digits, underscore, hyphen, space; max 50). A duplicate name is 409"
              + " and does not overwrite. Optional description replaces the copied description;"
              + " omit it to keep the source description. Jackson root wrap is WorkflowCreate.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Copied",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid new name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Source workflow not found"),
        @ApiResponse(responseCode = "409", description = "A workflow with the new name exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary copyWorkflow(
      @PathParam("idOrName") String idOrName, WorkflowCreate body) {
    if (body == null) {
      throw new WebApplicationException("Workflow name is required", 400);
    }
    try {
      return requireAdaptor().copyWorkflow(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to copy workflow ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update a workflow's description",
      description =
          "Slice 21 Admin. Updates the description of an existing stepped workflow (full graph"
              + " design and renaming stay outside this surface). The body's `name` must match"
              + " the path idOrName. A non-null description (including the empty string) replaces"
              + " the stored value; a missing/null description leaves the stored value untouched."
              + " Jackson root wrap is WorkflowUpdate.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, mismatched name, or invalid idOrName"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary updateWorkflow(
      @PathParam("idOrName") String idOrName, WorkflowUpdate body) {
    if (body == null) {
      throw new WebApplicationException("Workflow update body is required", 400);
    }
    try {
      return requireAdaptor().updateWorkflow(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update workflow ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete a workflow",
      description =
          "Slice 21 Admin. Deletes a stepped workflow via IPSSteppedWorkflowService.deleteWorkflow"
              + " (same backend the workflow-admin editor uses). Path idOrName is name, numeric"
              + " uuid, or rest guid (same resolution as PUT). System workflows and workflows"
              + " that still own content items return 409; a missing workflow returns 404.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "Missing or invalid idOrName (blank, wildcards)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Workflow is a system workflow or still owns content items"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public void deleteWorkflow(@PathParam("idOrName") String idOrName) {
    try {
      requireAdaptor().deleteWorkflow(uriInfo.getBaseUri(), idOrName);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete workflow ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}/graph")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Read a workflow state/transition graph",
      description =
          "Slice 32 Admin. Read-only projection of workflow states and transitions (including"
              + " aging transitions). Packaged workflows (Default Workflow, Simple Workflow,"
              + " Local Content, or the default flag) are marked packaged=true and remain"
              + " readable. Does not create or edit steps or transitions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Graph",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(responseCode = "400", description = "Invalid idOrName"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph getWorkflowGraph(@PathParam("idOrName") String idOrName) {
    try {
      return requireAdaptor().getWorkflowGraph(uriInfo.getBaseUri(), idOrName);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to read workflow graph ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/steps")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create a workflow step",
      description =
          "Slice 30 Admin. Inserts a step after `afterStep` (or the first existing step) via"
              + " IPSSteppedWorkflowService.createStep. Packaged default workflows (Default"
              + " Workflow, Simple Workflow, Local Content) are forbidden (403). Invalid names"
              + " are 400. Jackson root wrap is WorkflowStepWrite. Transition graph design stays"
              + " outside this surface.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid step or afterStep name"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "409", description = "Step name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary createWorkflowStep(
      @PathParam("idOrName") String idOrName, WorkflowStepWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow step body is required", 400);
    }
    try {
      return requireAdaptor().createWorkflowStep(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to create workflow step ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/steps/{stepName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update a workflow step",
      description =
          "Slice 30 Admin. Renames a step and/or replaces assigned roles via"
              + " IPSSteppedWorkflowService.updateStep. Path stepName is the current name. Body"
              + " `name` is the new name (may match). Packaged default workflows are 403.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid step name or missing body"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow or step not found"),
        @ApiResponse(responseCode = "409", description = "Target step name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary updateWorkflowStep(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow step body is required", 400);
    }
    try {
      return requireAdaptor()
          .updateWorkflowStep(uriInfo.getBaseUri(), idOrName, stepName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update workflow step ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}/transitions")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete one workflow transition",
      description =
          "Slice 33 Admin. Deletes a single transition between existing steps. Query `from` is"
              + " the source step and `label` is the transition label (or trigger). Query `to`"
              + " is the destination step and is required when more than one transition on the"
              + " source step shares the label. Does not delete steps. Packaged default workflows"
              + " (Default Workflow, Simple Workflow, Local Content) are forbidden (403).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Deleted; returns the updated graph",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing from/label, or label is ambiguous without to"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or transition not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph deleteWorkflowTransition(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("label") String label,
      @QueryParam("to") String toStep) {
    if (fromStep == null || fromStep.isBlank() || label == null || label.isBlank()) {
      throw new WebApplicationException("from and label are required", 400);
    }
    try {
      return requireAdaptor()
          .deleteWorkflowTransition(uriInfo.getBaseUri(), idOrName, fromStep, label, toStep);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete workflow transition ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}/steps/{stepName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete one workflow step",
      description =
          "Slice 34 Admin. Deletes a single step only when no regular or aging transition still"
              + " uses it (outgoing or incoming). Does not rewire neighboring steps. Packaged"
              + " default workflows (Default Workflow, Simple Workflow, Local Content) are"
              + " forbidden (403). A step that is still referenced returns 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Deleted; returns the updated graph",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid step name"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow or step not found"),
        @ApiResponse(
            responseCode = "409",
            description = "A transition still references the step"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph deleteWorkflowStep(
      @PathParam("idOrName") String idOrName, @PathParam("stepName") String stepName) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("stepName is required", 400);
    }
    try {
      return requireAdaptor().deleteWorkflowStep(uriInfo.getBaseUri(), idOrName, stepName);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete workflow step ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }
}
