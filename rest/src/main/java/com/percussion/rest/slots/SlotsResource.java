/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restSlotsResource")
@Path("/slots")
@XmlRootElement
@Tag(name = "Slots", description = "Assembly slot design catalog")
public class SlotsResource {

  private final ISlotsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public SlotsResource() {
    this.adaptor = null;
  }

  @Autowired
  public SlotsResource(ISlotsAdaptor adaptor) {
    this.adaptor = adaptor;
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
      return adaptor.listSlots(uriInfo.getBaseUri());
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
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
    try {
      SlotDetail detail = adaptor.getSlot(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        throw new WebApplicationException("Slot not found: " + idOrName, 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }
}
