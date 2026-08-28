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

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Catalog and write of content-editor shared field groups for the Developer module (CD-15).
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources.
 */
@PSSiteManageBean(value = "restSharedFieldsResource")
@Path("/sharedfields")
@XmlRootElement
@Tag(name = "SharedFields", description = "Shared field group design catalog and write")
public class SharedFieldsResource {

  private final ISharedFieldsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public SharedFieldsResource() {
    this.adaptor = null;
  }

  @Autowired
  public SharedFieldsResource(ISharedFieldsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List shared field groups",
      description =
          "Lists shared field groups from the content-editor shared definition. Admin (Design)"
              + " only. Create uses POST /sharedfields; save uses PUT /sharedfields/{name}; delete"
              + " uses DELETE /sharedfields/{name}.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = SharedFieldGroupSummary.class)))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<SharedFieldGroupSummary> listGroups() {
    try {
      return requireAdaptor().listGroups(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get shared field group detail",
      description =
          "Loads one shared field group by name (Admin/Design only). Includes field catalog."
              + " Control/choice write and system-def remain unsupported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SharedFieldGroupDetail.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SharedFieldGroupDetail getGroup(@PathParam("name") String name) {
    try {
      SharedFieldGroupDetail detail = requireAdaptor().getGroup(uriInfo.getBaseUri(), name);
      if (detail == null) {
        // Generic body: do not echo raw name (path probing).
        throw new WebApplicationException("Shared field group not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create a shared field group",
      description =
          "Admin. Creates and persists an empty shared field group via"
              + " IPSContentDesignWs.loadContentEditorSharedDef (lock) then"
              + " saveContentEditorSharedDef (release). Name is required, must be unique"
              + " (case-insensitive), and must not contain spaces or path characters. Optional"
              + " filename defaults to {name}.xml. Duplicate name is 409. Lock held by another"
              + " user is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created and saved",
            content = @Content(schema = @Schema(implementation = SharedFieldGroupDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name or filename"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "A group with that name exists, or shared def locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SharedFieldGroupDetail createGroup(SharedFieldGroupDetail body) {
    try {
      return requireAdaptor().createGroup(uriInfo.getBaseUri(), body);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update a shared field group",
      description =
          "Admin. Saves filename, optional rename (body.name different from path), and patches"
              + " to existing fields (searchable, occurrence/required). Null fields leaves the"
              + " catalog unchanged. Does not create or delete fields. Acquires the shared-def"
              + " design lock for this request and releases it on save. Missing group is 404;"
              + " lock held by another user is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = SharedFieldGroupDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or unknown field name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Rename target exists, or shared def locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SharedFieldGroupDetail updateGroup(
      @PathParam("name") String name, SharedFieldGroupDetail body) {
    try {
      SharedFieldGroupDetail detail = requireAdaptor().updateGroup(uriInfo.getBaseUri(), name, body);
      if (detail == null) {
        throw new WebApplicationException("Shared field group not found", 404);
      }
      return detail;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{name}")
  @Operation(
      summary = "Delete a shared field group",
      description =
          "Admin. Removes the group from the content-editor shared definition and saves via"
              + " IPSContentDesignWs.saveContentEditorSharedDef (releases the request lock)."
              + " Missing group is 404; lock held by another user is 409.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(responseCode = "409", description = "Shared def locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteGroup(@PathParam("name") String name) {
    try {
      requireAdaptor().deleteGroup(uriInfo.getBaseUri(), name);
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Lock conflicts are always 409 via {@link
   * SharedFieldDesignLockException}.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof SharedFieldDesignLockException) {
      return new WebApplicationException(e.getMessage(), 409);
    }
    if (e instanceof SharedFieldNotFoundException) {
      return new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Shared field group not found", 404);
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    if (e instanceof IllegalStateException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      if (msg.toLowerCase().contains("lock")) {
        return new WebApplicationException(msg, 409);
      }
      return new WebApplicationException(e, 500);
    }
    return new WebApplicationException(e, 500);
  }

  private ISharedFieldsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Shared fields adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
