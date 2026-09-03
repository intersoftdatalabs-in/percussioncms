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

package com.percussion.rest.relationshiptypes;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Relationship type catalog and Admin user-type write for the Developer module (SY-03).
 *
 * <p>Admin POST/PUT/DELETE persist <strong>user</strong> relationship types through {@link
 * IRelationshipTypeAdaptor} ({@code IPSSystemDesignWs}). System relationship types are read-only
 * (**409**). This resource is REST only — it does not add Developer SPA chrome.
 */
@PSSiteManageBean(value = "restRelationshipTypeResource")
@Path("/relationshiptypes")
@XmlRootElement
@Tag(
    name = "Relationship Types",
    description = "Relationship type design catalog and user-type write")
public class RelationshipTypeResource {

  private final IRelationshipTypeAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #RelationshipTypeResource(IRelationshipTypeAdaptor)}; catalog methods call {@link
   * #requireAdaptor()}.
   */
  public RelationshipTypeResource() {
    this.adaptor = null;
  }

  @Autowired
  public RelationshipTypeResource(IRelationshipTypeAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List relationship types",
      description =
          "Lists system and user relationship types with category, properties, and effects."
              + " Admin create/update/delete of user types are POST/PUT/DELETE on this resource.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = RelationshipType.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<RelationshipType> listRelationshipTypes() {
    try {
      List<RelationshipType> list = requireAdaptor().listRelationshipTypes();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get relationship type detail",
      description =
          "Loads one relationship type by name or GUID string (type-host-uuid). Admin write of"
              + " user types is POST/PUT/DELETE on this resource. System types cannot be mutated.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RelationshipType.class))),
        @ApiResponse(responseCode = "404", description = "Relationship type not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RelationshipType getRelationshipType(@PathParam("idOrName") String idOrName) {
    try {
      RelationshipType type = requireAdaptor().findRelationshipType(idOrName);
      if (type == null) {
        throw new WebApplicationException("Relationship type not found", 404);
      }
      return type;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create user relationship type",
      description =
          "Admin. Creates a user relationship type via IPSSystemDesignWs.createRelationshipTypes"
              + " + save. name is required (no whitespace). Provide category (code or label) or"
              + " copyFrom (existing name/GUID to copy mutable fields from — Workbench"
              + " copy-from-system). Duplicate name is 409. System types cannot be created as"
              + " system (always user). This is REST only — there is no Developer SPA create"
              + " chrome.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = RelationshipType.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "409", description = "Name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RelationshipType createRelationshipType(RelationshipType body) {
    try {
      RelationshipType created = requireAdaptor().createRelationshipType(body);
      if (created == null) {
        throw new WebApplicationException("Failed to create relationship type", 500);
      }
      return created;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update user relationship type",
      description =
          "Admin. Updates mutable fields of a user relationship type (label, description,"
              + " category, cloning/revision flags, user properties). Name is not renamed on PUT."
              + " System types are 409. Unknown key is 404. Prefer round-trip GET then PUT for"
              + " boolean flags.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = RelationshipType.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Relationship type not found"),
        @ApiResponse(responseCode = "409", description = "System relationship type cannot be mutated"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RelationshipType updateRelationshipType(
      @PathParam("idOrName") String idOrName, RelationshipType body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      RelationshipType updated = requireAdaptor().updateRelationshipType(idOrName, body);
      if (updated == null) {
        throw new WebApplicationException("Relationship type not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}")
  @Operation(
      summary = "Delete user relationship type",
      description =
          "Admin. Deletes a user relationship type by name or GUID. System types are 409 (not"
              + " deleted). Unknown key is 404. Following GET is 404 after a successful delete.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Relationship type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "System relationship type cannot be deleted"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteRelationshipType(@PathParam("idOrName") String idOrName) {
    try {
      boolean deleted = requireAdaptor().deleteRelationshipType(idOrName);
      if (!deleted) {
        throw new WebApplicationException("Relationship type not found", 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin 403, duplicate 409, and immutable 409 are
   * already {@link WebApplicationException}s from the adaptor.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    return new WebApplicationException(e, 500);
  }

  private IRelationshipTypeAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with ServerConfigs/Slots peers)
      throw new WebApplicationException(
          "Relationship type adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
