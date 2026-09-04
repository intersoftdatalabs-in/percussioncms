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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
}
