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

  @POST
  @Path("/{idOrName}/rename")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Rename a custom workflow",
      description =
          "Slice 60 Admin. Renames one custom workflow via IPSSteppedWorkflowService.updateWorkflow"
              + " (new name validated against the previous name). Description, steps, transitions,"
              + " and roles are unchanged. Packaged workflows (Default Workflow, Simple Workflow,"
              + " Local Content) and the current system default are 403 and are not renamed."
              + " Duplicate name is 409. Invalid name is 400. This does not change PUT"
              + " /{idOrName}, which still requires WorkflowUpdate.name to match the path."
              + " Jackson root wrap is WorkflowRename.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Renamed",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid new name"),
        @ApiResponse(responseCode = "403", description = "Admin required, or packaged/default"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "409", description = "A workflow with the new name exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary renameWorkflow(
      @PathParam("idOrName") String idOrName, WorkflowRename body) {
    if (body == null) {
      throw new WebApplicationException("Workflow name is required", 400);
    }
    try {
      return requireAdaptor().renameWorkflow(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to rename workflow ({}): {}", e.getClass().getName(), e.getMessage(), e);
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
          "Slice 21 Admin. Updates the description of an existing stepped workflow. The body's"
              + " `name` must match the path idOrName (a mismatch is 400). Renames use POST"
              + " /{idOrName}/rename and are not accepted on this body. A non-null description"
              + " (including the empty string) replaces the stored value; a missing/null"
              + " description leaves the stored value untouched. Jackson root wrap is"
              + " WorkflowUpdate.",
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

  @POST
  @Path("/{idOrName}/default")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set the system default workflow",
      description =
          "Slice 37 Admin. Marks this workflow as the single system default. A later call for"
              + " another workflow replaces the stored name so the previous workflow is no longer"
              + " default. Does not rewrite content items.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Default updated",
            content = @Content(schema = @Schema(implementation = WorkflowSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid idOrName"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowSummary setDefaultWorkflow(@PathParam("idOrName") String idOrName) {
    try {
      return requireAdaptor().setDefaultWorkflow(uriInfo.getBaseUri(), idOrName);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to set default workflow ({}): {}",
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

  @PUT
  @Path("/{idOrName}/transitions/comment-required")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set whether a transition requires a comment",
      description =
          "Slice 38 Admin. Sets the comment-required flag on one existing transition. Query"
              + " `from` is the source step and `label` is the transition label (or trigger)."
              + " Query `to` is required when more than one transition on the source step shares"
              + " the label. Does not create or delete the transition. Packaged default workflows"
              + " are forbidden (403). Jackson root wrap is WorkflowTransitionComment. Editor and"
              + " Explorer transition dialogs read this flag from item getTransitions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns the graph with commentRequired on the edge",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, from, or label, or the label is ambiguous, or the match is aging"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or transition not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph updateTransitionCommentRequired(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("label") String label,
      @QueryParam("to") String toStep,
      WorkflowTransitionComment body) {
    if (body == null) {
      throw new WebApplicationException("Workflow transition comment body is required", 400);
    }
    if (fromStep == null || fromStep.isBlank() || label == null || label.isBlank()) {
      throw new WebApplicationException("from and label are required", 400);
    }
    try {
      return requireAdaptor()
          .updateTransitionCommentRequired(
              uriInfo.getBaseUri(),
              idOrName,
              fromStep,
              label,
              toStep,
              body.isCommentRequired());
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update transition comment requirement ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/transitions/approvals-required")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set how many approvals a transition requires",
      description =
          "Slice 70 Admin. Sets TRANSITIONAPPROVALSREQUIRED on one existing regular transition."
              + " Query `from` is the source step and `label` is the transition label (or trigger)."
              + " Query `to` is required when more than one transition on the source step shares"
              + " the label. The body count must be a non-negative whole number. Does not create or"
              + " delete the transition and does not change the label, destination, comment flag,"
              + " or default flag. A stored negative count is each-role approval and is 409."
              + " Packaged default workflows are forbidden (403). Jackson root wrap is"
              + " WorkflowTransitionApprovals.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns the graph with approvalsRequired on the edge",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Missing body, from, or label, negative count, unchanged count, ambiguous label,"
                    + " or the match is aging"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or transition not found"),
        @ApiResponse(
            responseCode = "409",
            description = "The transition uses each-role approval and was not changed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph updateTransitionApprovalsRequired(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("label") String label,
      @QueryParam("to") String toStep,
      WorkflowTransitionApprovals body) {
    if (body == null || body.getApprovalsRequired() == null) {
      throw new WebApplicationException("Workflow transition approvals body is required", 400);
    }
    if (body.getApprovalsRequired() < 0) {
      throw new WebApplicationException("approvals required must be a non-negative whole number", 400);
    }
    if (fromStep == null || fromStep.isBlank() || label == null || label.isBlank()) {
      throw new WebApplicationException("from and label are required", 400);
    }
    try {
      return requireAdaptor()
          .updateTransitionApprovalsRequired(
              uriInfo.getBaseUri(),
              idOrName,
              fromStep,
              label,
              toStep,
              body.getApprovalsRequired());
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update transition approvals ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/transitions/default")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Mark one transition as the default",
      description =
          "Slice 71 Admin. Sets DEFAULTTRANSITION on one existing regular transition and clears"
              + " it on the other regular transitions from the same step. Query `from` is the"
              + " source step and `label` is the transition label (or trigger). Query `to` is"
              + " required when more than one transition on the source step shares the label."
              + " The body must be true to mark. False does not clear a stored default (409 when"
              + " that transition is already the default, 400 otherwise). Does not create or"
              + " delete the transition and does not change the label, destination, comment flag,"
              + " or approval count. Aging transitions are 400. Packaged default workflows are"
              + " forbidden (403). The previous default stays until this call succeeds. Jackson"
              + " root wrap is WorkflowTransitionDefault.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns the graph with defaultTransition on regular edges",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Missing body, from, or label, false on a transition that is not the default,"
                    + " already the only default, ambiguous label, or the match is aging"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or transition not found"),
        @ApiResponse(
            responseCode = "409",
            description = "The body would clear the current default and nothing was changed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph updateTransitionDefault(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("label") String label,
      @QueryParam("to") String toStep,
      WorkflowTransitionDefault body) {
    if (body == null || body.getDefaultTransition() == null) {
      throw new WebApplicationException("Workflow transition default body is required", 400);
    }
    if (fromStep == null || fromStep.isBlank() || label == null || label.isBlank()) {
      throw new WebApplicationException("from and label are required", 400);
    }
    try {
      return requireAdaptor()
          .updateTransitionDefault(
              uriInfo.getBaseUri(),
              idOrName,
              fromStep,
              label,
              toStep,
              body.getDefaultTransition());
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to mark the default transition ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/transitions")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create one workflow transition",
      description =
          "Slice 31 Admin. Inserts one transition between two existing steps. Body from, to,"
              + " and label are required. Does not create steps. A duplicate from/label/to edge"
              + " is 409. Packaged default workflows are forbidden (403). Jackson root wrap is"
              + " WorkflowTransitionWrite.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created; returns the updated graph",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(responseCode = "400", description = "Missing body or invalid from, to, or label"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow or step not found"),
        @ApiResponse(responseCode = "409", description = "That transition already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph createWorkflowTransition(
      @PathParam("idOrName") String idOrName, WorkflowTransitionWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow transition body is required", 400);
    }
    if (body.getFrom() == null
        || body.getFrom().isBlank()
        || body.getTo() == null
        || body.getTo().isBlank()
        || body.getLabel() == null
        || body.getLabel().isBlank()) {
      throw new WebApplicationException("from, to, and label are required", 400);
    }
    try {
      return requireAdaptor().createWorkflowTransition(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to create workflow transition ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/aging-transitions")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create one absolute aging transition",
      description =
          "Slice 57 Admin. Inserts one absolute aging transition between two existing steps."
              + " Body from, to, and a positive intervalMinutes (minutes, IPSAgingTransition"
              + " setInterval) are required. Does not create steps, change an existing interval,"
              + " or set comment-required. A duplicate absolute aging edge for that from, to, and"
              + " interval is 409. Packaged default workflows are forbidden (403). Jackson root"
              + " wrap is WorkflowAgingTransitionWrite.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created; returns the updated graph including the aging edge",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank from or to, or a non-positive interval"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow or step not found"),
        @ApiResponse(responseCode = "409", description = "That absolute aging transition already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph createAbsoluteAgingTransition(
      @PathParam("idOrName") String idOrName, WorkflowAgingTransitionWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow aging transition body is required", 400);
    }
    if (body.getFrom() == null
        || body.getFrom().isBlank()
        || body.getTo() == null
        || body.getTo().isBlank()) {
      throw new WebApplicationException("from and to are required", 400);
    }
    if (body.getIntervalMinutes() <= 0) {
      throw new WebApplicationException("interval must be a positive number of minutes", 400);
    }
    try {
      return requireAdaptor()
          .createAbsoluteAgingTransition(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to create aging transition ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/aging-transitions/interval")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Change one absolute aging interval",
      description =
          "Slice 58 Admin. Changes the minute interval on one existing absolute aging transition."
              + " Body from, to, and intervalMinutes identify the edge. newIntervalMinutes is the"
              + " replacement and must be a different positive number of minutes. Does not move"
              + " the destination step, change the aging type, delete the transition, or edit"
              + " packaged workflows. A duplicate absolute interval for that from and to is 409."
              + " Jackson root wrap is WorkflowAgingIntervalWrite.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns the graph with the new interval on that aging edge",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Missing body, blank from or to, a non-positive interval, or an unchanged interval"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(
            responseCode = "404",
            description = "Workflow, step, or absolute aging transition not found"),
        @ApiResponse(
            responseCode = "409",
            description = "That absolute aging interval already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph changeAbsoluteAgingInterval(
      @PathParam("idOrName") String idOrName, WorkflowAgingIntervalWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow aging interval body is required", 400);
    }
    if (body.getFrom() == null
        || body.getFrom().isBlank()
        || body.getTo() == null
        || body.getTo().isBlank()) {
      throw new WebApplicationException("from and to are required", 400);
    }
    if (body.getIntervalMinutes() <= 0 || body.getNewIntervalMinutes() <= 0) {
      throw new WebApplicationException("interval must be a positive number of minutes", 400);
    }
    if (body.getIntervalMinutes() == body.getNewIntervalMinutes()) {
      throw new WebApplicationException("new interval must differ from the current interval", 400);
    }
    try {
      return requireAdaptor().changeAbsoluteAgingInterval(uriInfo.getBaseUri(), idOrName, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to change aging interval ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}/aging-transitions")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete one absolute aging transition",
      description =
          "Slice 59 Admin. Deletes one absolute aging transition identified by query from, to,"
              + " and intervalMinutes. The edge disappears only after the delete succeeds. Does"
              + " not delete steps or regular transitions. A repeated or system-field aging"
              + " transition that uses the same interval is not deleted (409). Packaged default"
              + " workflows are forbidden (403).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Deleted; returns the updated graph without that aging edge",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing from or to, or a non-positive interval"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(
            responseCode = "404",
            description = "Workflow, step, or absolute aging transition not found"),
        @ApiResponse(
            responseCode = "409",
            description = "The matching aging transition is not absolute"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph deleteAbsoluteAgingTransition(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("to") String toStep,
      @QueryParam("intervalMinutes") String intervalMinutes) {
    if (fromStep == null || fromStep.isBlank() || toStep == null || toStep.isBlank()) {
      throw new WebApplicationException("from and to are required", 400);
    }
    long minutes = parsePositiveMinutes(intervalMinutes);
    try {
      return requireAdaptor()
          .deleteAbsoluteAgingTransition(
              uriInfo.getBaseUri(), idOrName, fromStep, toStep, minutes);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete aging transition ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  private static long parsePositiveMinutes(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new WebApplicationException("interval must be a positive number of minutes", 400);
    }
    final long minutes;
    try {
      minutes = Long.parseLong(raw.trim());
    } catch (NumberFormatException ex) {
      throw new WebApplicationException("interval must be a positive number of minutes", 400);
    }
    if (minutes <= 0) {
      throw new WebApplicationException("interval must be a positive number of minutes", 400);
    }
    return minutes;
  }

  @PUT
  @Path("/{idOrName}/transitions")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update one workflow transition",
      description =
          "Slice 31 Admin. Changes the label and destination of one existing transition. Query"
              + " from and label identify the edge; query to is required when the label is not"
              + " unique on the source step. Body label and to are the new values. Does not move"
              + " the source step or create steps. Packaged default workflows are forbidden (403)."
              + " Jackson root wrap is WorkflowTransitionWrite.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns the graph",
            content = @Content(schema = @Schema(implementation = WorkflowGraph.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, from, or label, or the label is ambiguous, or a name is invalid"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or transition not found"),
        @ApiResponse(responseCode = "409", description = "The new from/label/to edge already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowGraph updateWorkflowTransition(
      @PathParam("idOrName") String idOrName,
      @QueryParam("from") String fromStep,
      @QueryParam("label") String label,
      @QueryParam("to") String toStep,
      WorkflowTransitionWrite body) {
    if (body == null) {
      throw new WebApplicationException("Workflow transition body is required", 400);
    }
    if (fromStep == null || fromStep.isBlank() || label == null || label.isBlank()) {
      throw new WebApplicationException("from and label are required", 400);
    }
    if (body.getTo() == null || body.getTo().isBlank() || body.getLabel() == null || body.getLabel().isBlank()) {
      throw new WebApplicationException("label and to are required", 400);
    }
    try {
      return requireAdaptor()
          .updateWorkflowTransition(
              uriInfo.getBaseUri(), idOrName, fromStep, label, toStep, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update workflow transition ({}): {}",
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

  @GET
  @Path("/{idOrName}/role-assignments")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List step role assignment types",
      description =
          "Slice 61 Admin. Lists every role already assigned to a step and its stored assignment"
              + " type, including Reader. Does not mutate. Missing workflow is 404.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Assignment rows",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList listStepRoleAssignments(
      @PathParam("idOrName") String idOrName) {
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().listStepRoleAssignments(uriInfo.getBaseUri(), idOrName);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      if (list.getAssignments() == null) {
        list.setAssignments(List.of());
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to list step role assignments ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/steps/{stepName}/role-assignment")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set one step role assignment type",
      description =
          "Slice 61 Admin. Sets Reader or Assignee on one role already assigned to the path step."
              + " Does not rename the step, add or remove roles, or change notify and inbox flags."
              + " Packaged and system-default workflows are 403. An Admin or None role is 409 and"
              + " is not changed. An unchanged type, a blank role, or a type other than Reader or"
              + " Assignee is 400. Missing workflow, step, or role is 404. Jackson root wrap is"
              + " WorkflowStepRoleAssignmentWrite. Returns the assignment list after the write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns assignment rows including the new type",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank role, invalid type, or unchanged type"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Current assignment type is not Reader or Assignee"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList setStepRoleAssignment(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepRoleAssignmentWrite body) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (body == null) {
      throw new WebApplicationException("Workflow step role assignment body is required", 400);
    }
    if (body.getRoleName() == null || body.getRoleName().isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (!isReaderOrAssignee(body.getAssignmentType())) {
      throw new WebApplicationException("assignment type must be READER or ASSIGNEE", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor()
              .setStepRoleAssignment(uriInfo.getBaseUri(), idOrName, stepName, body);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to set step role assignment ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/steps/{stepName}/role-notify")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set notify on one step role",
      description =
          "Slice 65 Admin. Turns notify (ISNOTIFYON) on or off for one role already assigned to"
              + " the path step. Does not change the assignment type, add or remove roles, or edit"
              + " the inbox flag. This is not PUT role-assignment. Packaged and system-default"
              + " workflows are 403. An unchanged flag, a blank role, or a missing notify value is"
              + " 400. Missing workflow, step, or role is 404. Jackson root wrap is"
              + " WorkflowStepRoleNotifyWrite. Returns the assignment list after the write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns assignment rows including the stored notify flag",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank role, missing notify, or unchanged notify"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList setStepRoleNotify(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepRoleNotifyWrite body) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (body == null) {
      throw new WebApplicationException("Workflow step role notify body is required", 400);
    }
    if (body.getRoleName() == null || body.getRoleName().isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (body.getNotify() == null) {
      throw new WebApplicationException("notify is required", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().setStepRoleNotify(uriInfo.getBaseUri(), idOrName, stepName, body);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to set step role notify ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/steps/{stepName}/role-inbox")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set inbox on one step role",
      description =
          "Slice 66 Admin. Turns inbox (SHOWININBOX) on or off for one Reader or Assignee already"
              + " assigned to the path step. Does not change the assignment type, add or remove"
              + " roles, or edit notify. This is not PUT role-notify and not PUT role-assignment."
              + " Packaged and system-default workflows are 403. An Admin or None role is 409 and"
              + " is not changed. An unchanged flag, a blank role, or a missing inbox value is"
              + " 400. Missing workflow, step, or role is 404. Jackson root wrap is"
              + " WorkflowStepRoleInboxWrite. Returns the assignment list after the write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns assignment rows including the stored inbox flag",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank role, missing inbox, or unchanged inbox"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Current assignment type is not Reader or Assignee"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList setStepRoleInbox(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepRoleInboxWrite body) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (body == null) {
      throw new WebApplicationException("Workflow step role inbox body is required", 400);
    }
    if (body.getRoleName() == null || body.getRoleName().isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (body.getInbox() == null) {
      throw new WebApplicationException("inbox is required", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().setStepRoleInbox(uriInfo.getBaseUri(), idOrName, stepName, body);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to set step role inbox ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/steps/{stepName}/role-adhoc")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Set the adhoc type on one step role",
      description =
          "Slice 69 Admin. Sets the adhoc type (disabled, enabled, or anonymous) for one Reader"
              + " or Assignee already assigned to the path step. Does not change the assignment"
              + " type, add or remove roles, or edit notify or inbox. This is not PUT role-inbox"
              + " and not PUT role-notify. Packaged and system-default workflows are 403. An"
              + " Admin or None role is 409 and is not changed. An unchanged type, a blank role,"
              + " or a value other than disabled, enabled, or anonymous is 400. Missing workflow,"
              + " step, or role is 404. Jackson root wrap is WorkflowStepRoleAdhocWrite. Returns"
              + " the assignment list after the write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated; returns assignment rows including the stored adhoc type",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank role, invalid adhoc type, or unchanged adhoc type"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Current assignment type is not Reader or Assignee"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList setStepRoleAdhoc(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepRoleAdhocWrite body) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (body == null) {
      throw new WebApplicationException("Workflow step role adhoc body is required", 400);
    }
    if (body.getRoleName() == null || body.getRoleName().isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (!isAdhocType(body.getAdhocType())) {
      throw new WebApplicationException(
          "adhoc type must be disabled, enabled, or anonymous", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().setStepRoleAdhoc(uriInfo.getBaseUri(), idOrName, stepName, body);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to set step role adhoc ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/steps/{stepName}/roles")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add one role to a workflow step",
      description =
          "Slice 63 Admin. Adds one existing workflow role onto the path step. This is not PUT"
              + " role-assignment, which only sets Reader or Assignee on a role already assigned."
              + " Assignment type is Reader or Assignee. Notify and inbox stay at the entity"
              + " defaults and are not edited. Does not rename the step or change other roles."
              + " Packaged and system-default workflows are 403. A role already on the step is"
              + " 409 and is not added again. A blank role or a type other than Reader or Assignee"
              + " is 400. Missing workflow, step, or role is 404. Jackson root wrap is"
              + " WorkflowStepRoleAdd. Returns the assignment list after the write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Added; returns assignment rows including the new role",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing body, blank role, or invalid assignment type"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(responseCode = "409", description = "Role is already assigned to this step"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList addStepRole(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      WorkflowStepRoleAdd body) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (body == null) {
      throw new WebApplicationException("Workflow step role body is required", 400);
    }
    if (body.getRoleName() == null || body.getRoleName().isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (!isReaderOrAssignee(body.getAssignmentType())) {
      throw new WebApplicationException("assignment type must be READER or ASSIGNEE", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().addStepRole(uriInfo.getBaseUri(), idOrName, stepName, body);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to add step role ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}/steps/{stepName}/roles/{roleName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Remove one role from a workflow step",
      description =
          "Slice 64 Admin. Removes one Reader or Assignee role from the path step. This is not"
              + " PUT role-assignment, which only sets Reader or Assignee, and it is not POST"
              + " .../roles, which adds a role. Other steps keep the role. Notify and inbox flags"
              + " on remaining roles are not edited. Packaged and system-default workflows are"
              + " 403. An Admin or None assignment is 409 and is not removed. A blank step or role"
              + " is 400. Missing workflow, step, or role is 404. The role stays assigned until"
              + " this call succeeds. Returns the assignment list after the delete.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Removed; returns assignment rows without that step role",
            content =
                @Content(schema = @Schema(implementation = WorkflowStepRoleAssignmentList.class))),
        @ApiResponse(responseCode = "400", description = "Blank step or role name"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin required, or packaged/default workflow is protected"),
        @ApiResponse(responseCode = "404", description = "Workflow, step, or role not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Current assignment type is not Reader or Assignee"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public WorkflowStepRoleAssignmentList removeStepRole(
      @PathParam("idOrName") String idOrName,
      @PathParam("stepName") String stepName,
      @PathParam("roleName") String roleName) {
    if (stepName == null || stepName.isBlank()) {
      throw new WebApplicationException("Step name is required", 400);
    }
    if (roleName == null || roleName.isBlank()) {
      throw new WebApplicationException("Role name is required", 400);
    }
    try {
      WorkflowStepRoleAssignmentList list =
          requireAdaptor().removeStepRole(uriInfo.getBaseUri(), idOrName, stepName, roleName);
      if (list == null) {
        throw new WebApplicationException("Workflow not found: " + idOrName, 404);
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to remove step role ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  private static boolean isReaderOrAssignee(String raw) {
    if (raw == null) {
      return false;
    }
    String n = raw.trim();
    return n.equalsIgnoreCase("READER")
        || n.equalsIgnoreCase("Reader")
        || n.equalsIgnoreCase("ASSIGNEE")
        || n.equalsIgnoreCase("Assignee");
  }

  private static boolean isAdhocType(String raw) {
    if (raw == null) {
      return false;
    }
    String n = raw.trim();
    return n.equalsIgnoreCase("disabled")
        || n.equalsIgnoreCase("enabled")
        || n.equalsIgnoreCase("anonymous");
  }
}
