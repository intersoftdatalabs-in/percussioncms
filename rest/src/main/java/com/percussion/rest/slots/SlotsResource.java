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

package com.percussion.rest.slots;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restSlotsResource")
@Path("/slots")
@XmlRootElement
@Tag(name = "Slots", description = "Assembly slot design catalog")
public class SlotsResource {

  private static final Logger log = LogManager.getLogger(SlotsResource.class);

  private final ISlotsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #SlotsResource(ISlotsAdaptor)}; list/detail methods call {@link #requireAdaptor()}.
   */
  public SlotsResource() {
    this.adaptor = null;
  }

  @Autowired
  public SlotsResource(ISlotsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List slots",
      description = "Lists all assembly template slots (design catalog).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = SlotSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<SlotSummary> listSlots() {
    try {
      return requireAdaptor().listSlots(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list slots ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get slot design detail",
      description =
          "Read-only slot detail including finder metadata and content-type/template"
              + " associations. Create/update/delete not supported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SlotDetail.class))),
        @ApiResponse(responseCode = "404", description = "Slot not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotDetail getSlot(@PathParam("idOrName") String idOrName) {
    SlotDetail detail;
    try {
      detail = requireAdaptor().getSlot(uriInfo.getBaseUri(), idOrName);
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      // 404 is thrown outside this try; do not re-wrap mapped HTTP errors here
      log.error(
          "Failed to load slot {} ({}): {}", idOrName, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
    if (detail == null) {
      throw new WebApplicationException("Slot not found: " + idOrName, 404);
    }
    return detail;
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update slot design fields",
      description =
          "Updates mutable slot fields: label and/or description. When associations is present"
              + " (including empty), replaces the full content-type/template association set."
              + " Omit associations to leave them unchanged. Name/id is immutable."
              + " Create/delete/lock remain unsupported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = SlotDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Slot not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SlotDetail updateSlot(@PathParam("idOrName") String idOrName, SlotDetail body) {
    try {
      SlotDetail detail = requireAdaptor().updateSlot(uriInfo.getBaseUri(), idOrName, body);
      if (detail == null) {
        throw new WebApplicationException("Slot not found: " + idOrName, 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      log.error(
          "Failed to update slot {} ({}): {}", idOrName, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  private ISlotsAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with KeywordsResource)
      throw new WebApplicationException(
          "Slots adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
