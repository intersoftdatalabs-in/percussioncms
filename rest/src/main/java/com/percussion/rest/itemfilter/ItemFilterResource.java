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

import com.percussion.services.error.PSNotFoundException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Assembly item filter catalog for the Developer module (AS-07).
 *
 * <p>Admin POST/PUT/DELETE persist through {@link IItemFilterAdaptor#updateOrCreateItemFilter} and
 * {@link IItemFilterAdaptor#deleteItemFilter} (system design WS / filter service). SPA item-filter
 * editor chrome is out of scope.
 */
@PSSiteManageBean(value = "restItemFilterResource")
@Path("/itemfilters")
@XmlRootElement
@Tag(name = "Item Filters", description = "Item filter design catalog (AS-07 CRUD)")
public class ItemFilterResource {

  private static final Logger log = LogManager.getLogger(ItemFilterResource.class);

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
      description = "Lists assembly item filters with rules and parent linkage.",
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
      description = "Loads one item filter by name or GUID string (type-host-uuid).",
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

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create item filter",
      description =
          "Admin. Creates and persists an assembly item filter via"
              + " IPSSystemDesignWs.createItemFilters then saveItemFilters (held design lock,"
              + " released on save). Name is required, unique (case-insensitive), and must not"
              + " contain whitespace or wildcards. Optional description, rules, parent filter, and"
              + " legacyAuthtype are applied before save. Duplicate name is 409. Workbench catalog"
              + " label is an alias of name.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = ItemFilter.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "409", description = "A filter with that name already exists"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ItemFilter createItemFilter(ItemFilter body) {
    try {
      if (body != null) {
        body.setFilterId(null);
      }
      ItemFilter created = requireAdaptor().updateOrCreateItemFilter(body);
      if (created == null) {
        throw new WebApplicationException("Failed to create item filter", 500);
      }
      return created;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error("Failed to create item filter ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update item filter",
      description =
          "Admin. Updates description, rules, parent filter, and/or legacyAuthtype by name or"
              + " GUID. Name is the catalog key (Workbench label aliases name) and is not renamed"
              + " on PUT. Loads with a design lock and releases on save. Unknown id is 404."
              + " Lock/dependency conflict is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = ItemFilter.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Filter not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ItemFilter updateItemFilter(@PathParam("idOrName") String idOrName, ItemFilter body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      ItemFilter existing = requireAdaptor().findItemFilter(idOrName);
      if (existing == null) {
        throw new WebApplicationException("Item filter not found", 404);
      }
      body.setFilterId(existing.getFilterId());
      ItemFilter updated = requireAdaptor().updateOrCreateItemFilter(body);
      if (updated == null) {
        throw new WebApplicationException("Item filter not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update item filter {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}")
  @Operation(
      summary = "Delete item filter",
      description =
          "Admin. Deletes an item filter by name or GUID via IPSSystemDesignWs.deleteItemFilters"
              + " (ignoreDependencies=false). Unknown id is 404. In-use (associated with a content"
              + " list) or lock conflict is 409. Following GET is 404 after a successful delete.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Filter not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Filter is associated with a content list, or design lock conflict"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteItemFilter(@PathParam("idOrName") String idOrName) {
    try {
      ItemFilter existing = requireAdaptor().findItemFilter(idOrName);
      if (existing == null || existing.getFilterId() == null) {
        throw new WebApplicationException("Item filter not found", 404);
      }
      requireAdaptor().deleteItemFilter(existing.getFilterId());
      return Response.noContent().build();
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Item filter not found", 404);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete item filter {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin/session 403 and duplicate/in-use 409 are
   * already {@link WebApplicationException}s from the adaptor.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    if (e instanceof IllegalStateException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      String lower = msg.toLowerCase();
      if (lower.contains("lock") || lower.contains("depend")) {
        return new WebApplicationException(msg, 409);
      }
      return new WebApplicationException(e, 500);
    }
    return new WebApplicationException(e, 500);
  }

  private IItemFilterAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Item filter adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
