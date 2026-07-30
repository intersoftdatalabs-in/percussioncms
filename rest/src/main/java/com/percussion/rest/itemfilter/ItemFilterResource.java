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

package com.percussion.rest.itemfilter;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only item filter catalog for the Developer module (AS-07 list/detail).
 *
 * <p>Write endpoints remain unimplemented at the adaptor layer (later slice).
 */
@PSSiteManageBean(value = "restItemFilterResource")
@Path("/itemfilters")
@XmlRootElement
@Tag(name = "Item Filters", description = "Item filter design catalog (read-only)")
public class ItemFilterResource {

  private final IItemFilterAdaptor adaptor;

  public ItemFilterResource() {
    this.adaptor = null;
  }

  @Autowired
  public ItemFilterResource(IItemFilterAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List item filters",
      description =
          "Lists assembly item filters with rules and parent linkage. Create/edit/delete are later"
              + " slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ItemFilter.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ItemFilter> listItemFilters() {
    try {
      List<ItemFilter> list = requireAdaptor().getItemFilters();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get item filter detail",
      description =
          "Loads one item filter by name or GUID string (type-host-uuid). Write remains"
              + " unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ItemFilter.class))),
        @ApiResponse(responseCode = "404", description = "Filter not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ItemFilter getItemFilter(@PathParam("idOrName") String idOrName) {
    try {
      ItemFilter filter = requireAdaptor().findItemFilter(idOrName);
      if (filter == null) {
        throw new WebApplicationException("Item filter not found", 404);
      }
      return filter;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IItemFilterAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Item filter adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
