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

package com.percussion.rest.systemdef;

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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Content-editor system definition catalog and field-property write for the Developer module
 * (CD-16).
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources.
 */
@PSSiteManageBean(value = "restSystemDefResource")
@Path("/systemdef")
@XmlRootElement
@Tag(
    name = "SystemDef",
    description = "Content editor system definition catalog, field-property write, and field create/delete")
public class SystemDefResource {

  private final ISystemDefAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public SystemDefResource() {
    this.adaptor = null;
  }

  @Autowired
  public SystemDefResource(ISystemDefAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get system definition field catalog",
      description =
          "Loads the content-editor system definition field catalog (global system fields)."
              + " Admin (Design) only. Save uses PUT /systemdef to patch existing field properties."
              + " Add a field with POST /systemdef/fields; remove one with DELETE"
              + " /systemdef/fields/{fieldName}.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SystemDefDetail.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SystemDefDetail getSystemDef() {
    try {
      return requireAdaptor().getSystemDef(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update system definition field properties",
      description =
          "Admin. Saves patches to existing system fields (searchable, occurrence/required) via"
              + " IPSContentDesignWs.loadContentEditorSystemDef (lock) then"
              + " saveContentEditorSystemDef (release). Null or empty fields leaves the catalog"
              + " unchanged. Does not create or delete fields — use nested POST/DELETE .../fields."
              + " When a field patch includes both occurrence and required they must agree (else"
              + " 400); occurrence is applied when present. Lock held by another user, or no lock"
              + " for this session, is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = SystemDefDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or unknown field name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "System def locked by another user, or design lock required"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SystemDefDetail updateSystemDef(SystemDefDetail body) {
    try {
      return requireAdaptor().updateSystemDef(uriInfo.getBaseUri(), body);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/fields")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add a field to the system definition",
      description =
          "Admin. Adds a persistable TYPE_SYSTEM field (backend column + display mapping) via"
              + " IPSContentDesignWs.loadContentEditorSystemDef (lock) then"
              + " saveContentEditorSystemDef (release). Body name is required, unique"
              + " case-insensitive, and must be a letter followed by letters, digits, or"
              + " underscore. Optional dataType defaults to text. Optional searchable and"
              + " occurrence/required use the same rules as PUT field patches. Duplicate field is"
              + " 409. Lock held by another user is 409. Control/stylesheet/flow remain"
              + " unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Field added and saved",
            content = @Content(schema = @Schema(implementation = SystemDefDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid field input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "A field with that name exists, or system def locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SystemDefDetail addField(SystemDefFieldSummary body) {
    try {
      return requireAdaptor().addField(uriInfo.getBaseUri(), body);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/fields/{fieldName}")
  @Operation(
      summary = "Delete a field from the system definition",
      description =
          "Admin. Removes the field and its display mapping and saves via"
              + " IPSContentDesignWs.saveContentEditorSystemDef (releases the request lock)."
              + " Unknown, blank, system-mandatory, or system-internal field is 400; lock held by"
              + " another user is 409.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid, unknown, system-mandatory, or system-internal field name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "409", description = "System def locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteField(@PathParam("fieldName") String fieldName) {
    try {
      requireAdaptor().deleteField(uriInfo.getBaseUri(), fieldName);
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Lock conflicts are always 409 via {@link
   * SystemDefDesignLockException}.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof SystemDefDesignLockException) {
      return new WebApplicationException(e.getMessage(), 409);
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

  private ISystemDefAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "System def adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
