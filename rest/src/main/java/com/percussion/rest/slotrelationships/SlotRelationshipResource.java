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
package com.percussion.rest.slotrelationships;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Active Assembly slot relationships for the modern Explorer AA host. Replaces Data Flow
 * {@code variantlistwithslots.html} / {@code itemassembly.html} navigation.
 */
@PSSiteManageBean(value = "restSlotRelationshipResource")
@Path("/assembly/slot-relationships")
@XmlRootElement
@Tag(
    name = "Assembly Slot Relationships",
    description = "Add, arrange, and remove Active Assembly slot relationships")
public class SlotRelationshipResource {

  private final ISlotRelationshipAdaptor adaptor;

  /** No-arg constructor for bean-discovery edge cases. */
  public SlotRelationshipResource() {
    this.adaptor = null;
  }

  /**
   * Production constructor.
   *
   * @param adaptor slot relationship adaptor, never {@code null} in the live webapp
   */
  @Autowired
  public SlotRelationshipResource(ISlotRelationshipAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/canvas")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List slots and relationships for an assembled owner",
      description =
          "Returns template slots and current Active Assembly relationships for the owner item.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotCanvas.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid ownerId"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotCanvas canvas(
      @Parameter(required = true, description = "Owner content id") @QueryParam("ownerId")
          Integer ownerId,
      @Parameter(description = "Page or snippet template id") @QueryParam("templateId")
          Integer templateId) {
    try {
      if (ownerId == null || ownerId <= 0) {
        throw new WebApplicationException("ownerId is required", 400);
      }
      return requireAdaptor().canvas(ownerId, templateId);
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
      summary = "Add an item to a slot",
      description = "Creates an Active Assembly relationship (Content Browser add).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotRelationship.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid ids"),
        @ApiResponse(responseCode = "404", description = "Owner not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotRelationship add(SlotAddRequest request) {
    try {
      if (request == null
          || request.getOwnerId() <= 0
          || request.getDependentId() <= 0
          || request.getSlotId() <= 0
          || request.getTemplateId() <= 0) {
        throw new WebApplicationException("ownerId, dependentId, slotId, and templateId are required", 400);
      }
      SlotRelationship created = requireAdaptor().add(request);
      if (created == null) {
        throw new WebApplicationException("Item not found", 404);
      }
      return created;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{relationshipId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Remove a slot relationship",
      description = "Deletes the Active Assembly relationship (Arrange Remove).",
      responses = {
        @ApiResponse(responseCode = "204", description = "Removed"),
        @ApiResponse(responseCode = "400", description = "Invalid relationship id"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response remove(
      @Parameter(required = true, description = "Relationship id") @PathParam("relationshipId")
          int relationshipId) {
    try {
      if (relationshipId <= 0) {
        throw new WebApplicationException("relationshipId is required", 400);
      }
      requireAdaptor().remove(relationshipId);
      return Response.noContent().build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{relationshipId}/move")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Move a slot item",
      description = "Moves the relationship up, down, or to an index (Arrange Move).",
      responses = {
        @ApiResponse(responseCode = "204", description = "Moved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response move(
      @Parameter(required = true, description = "Relationship id") @PathParam("relationshipId")
          int relationshipId,
      SlotMoveRequest request) {
    try {
      if (relationshipId <= 0 || request == null || isBlank(request.getDirection())) {
        throw new WebApplicationException("relationshipId and direction are required", 400);
      }
      requireAdaptor().move(relationshipId, request);
      return Response.noContent().build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{relationshipId}/template-slot")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Change template and/or slot",
      description = "Moves the relationship to another slot and/or snippet template.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotRelationship.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Relationship not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotRelationship changeTemplateSlot(
      @Parameter(required = true, description = "Relationship id") @PathParam("relationshipId")
          int relationshipId,
      SlotTemplateSlotRequest request) {
    try {
      if (relationshipId <= 0
          || request == null
          || request.getSlotId() <= 0
          || request.getTemplateId() <= 0) {
        throw new WebApplicationException("relationshipId, slotId, and templateId are required", 400);
      }
      SlotRelationship updated = requireAdaptor().changeTemplateSlot(relationshipId, request);
      if (updated == null) {
        throw new WebApplicationException("Relationship not found", 404);
      }
      return updated;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/allowed-types")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Allowed content types for a slot",
      description = "Types that may be created or added into the slot.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotAllowedChoiceList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid slot id"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotAllowedChoiceList allowedTypes(
      @Parameter(required = true, description = "Slot id") @QueryParam("slotId") Integer slotId) {
    try {
      if (slotId == null || slotId <= 0) {
        throw new WebApplicationException("slotId is required", 400);
      }
      return requireAdaptor().allowedTypes(slotId);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/allowed-templates")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Allowed snippet templates for a slot",
      description = "Templates associated with the slot, optionally filtered by content type.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotAllowedChoiceList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid slot id"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotAllowedChoiceList allowedTemplates(
      @Parameter(required = true, description = "Slot id") @QueryParam("slotId") Integer slotId,
      @Parameter(description = "Optional content type id") @QueryParam("contentTypeId")
          Integer contentTypeId) {
    try {
      if (slotId == null || slotId <= 0) {
        throw new WebApplicationException("slotId is required", 400);
      }
      return requireAdaptor().allowedTemplates(slotId, contentTypeId);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private ISlotRelationshipAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Slot relationship adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
