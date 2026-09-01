/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CX search design catalog (UI-06 list/detail/write) plus design-search execute façade for
 * Explorer.
 *
 * <p>Developer list is searches-only. Explorer saved-search picker uses {@code includeViews=true}
 * so CX views (including {@code View_All}) appear. Dedicated view management remains on {@code
 * /services/views}. Admin POST/PUT/DELETE persist through {@link ISearchAdaptor} ({@code
 * IPSUiDesignWs} create/save/delete searches). Execute is a separate façade and is not invoked
 * from write.
 */
@PSSiteManageBean(value = "restSearchResource")
@Path("/searches")
@XmlRootElement
@Tag(name = "Searches", description = "CX search design catalog, write, and design-search execute")
public class SearchResource {

  private final ISearchAdaptor adaptor;

  public SearchResource() {
    this.adaptor = null;
  }

  @Autowired
  public SearchResource(ISearchAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List search definitions",
      description =
          "Lists Content Explorer search definitions. Pass includeViews=true to also return CX"
              + " views (Explorer saved-search picker, including the default All / View_All"
              + " view). Admin create/save/delete are POST/PUT/DELETE on this resource.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = SearchDef.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<SearchDef> listSearches(
      @QueryParam("includeViews") @DefaultValue("false") boolean includeViews) {
    try {
      List<SearchDef> list = requireAdaptor().listSearches(includeViews);
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
      summary = "Get search detail",
      description =
          "Loads one search by name or GUID string. Field criteria included when present. Admin"
              + " write is POST/PUT/DELETE on this resource.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SearchDef.class))),
        @ApiResponse(responseCode = "404", description = "Search not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SearchDef getSearch(@PathParam("idOrName") String idOrName) {
    try {
      SearchDef search = requireAdaptor().findSearchByKey(idOrName);
      if (search == null) {
        throw new WebApplicationException("Search not found", 404);
      }
      return search;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Execute a saved/standard design search with full field operators. Path is multi-segment so it
   * does not collide with {@code GET /{idOrName}} catalog detail.
   */
  @POST
  @Path("/{idOrName}/execute")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Execute a design search",
      description =
          "Loads the CX search design by name, GUID, or id and executes it server-side with"
              + " design field operators, display format, max results, and case sensitivity."
              + " Optional body may override folder scope, paging, and sort. Custom URL searches"
              + " return 400.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SearchExecuteResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body or unsupported search type"),
        @ApiResponse(responseCode = "404", description = "Search not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SearchExecuteResult executeSearch(
      @PathParam("idOrName") String idOrName, SearchExecuteRequest body) {
    try {
      SearchExecuteResult result = requireAdaptor().executeSearch(idOrName, body);
      if (result == null) {
        throw new WebApplicationException("Search not found", 404);
      }
      return result;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid execute request", 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create search",
      description =
          "Admin. Creates and persists a CX search via IPSUiDesignWs.createSearches then"
              + " saveSearches (held design lock, released on save). Name is required, unique"
              + " (case-insensitive), and must not contain whitespace or wildcards. Optional"
              + " label, description, type (StandardSearch default; CustomSearch; Search), and"
              + " displayFormatId are applied before save. Duplicate name is 409. Views are not"
              + " created here (use /services/views). Execute is not invoked on write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = SearchDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "409", description = "A search with that name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SearchDef createSearch(SearchDef body) {
    try {
      if (body != null) {
        body.setGuid(null);
        body.setId(0);
      }
      SearchDef created = requireAdaptor().createSearch(body);
      if (created == null) {
        throw new WebApplicationException("Failed to create search", 500);
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
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update search",
      description =
          "Admin. Updates label, description, type, displayFormatId, and/or field criteria by name"
              + " or GUID. Name is the catalog key and is not renamed on PUT. Omitted fields leave"
              + " stored criteria unchanged. Unknown field name is 400. Packaged/system searches"
              + " reject field mutation with 409 (lock is not stolen). Loads with a design lock"
              + " (overrideLock=false) and releases on save. Unknown id is 404. Lock/dependency"
              + " conflict is 409. Views are not saved here. Execute is not invoked on write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = SearchDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Search not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SearchDef updateSearch(@PathParam("idOrName") String idOrName, SearchDef body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      SearchDef updated = requireAdaptor().saveSearch(idOrName, body);
      if (updated == null) {
        throw new WebApplicationException("Search not found", 404);
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
      summary = "Delete search",
      description =
          "Admin. Deletes a CX search by name or GUID via IPSUiDesignWs.deleteSearches"
              + " (ignoreDependencies=false). Unknown id is 404. Lock/dependency conflict is 409."
              + " Following GET is 404 after a successful delete. Views are not deleted here.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Search not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteSearch(@PathParam("idOrName") String idOrName) {
    try {
      boolean deleted = requireAdaptor().deleteSearch(idOrName);
      if (!deleted) {
        throw new WebApplicationException("Search not found", 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin/session 403 and duplicate 409 are already
   * {@link WebApplicationException}s from the adaptor.
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

  private ISearchAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Slots/Locales/Keywords)
      throw new WebApplicationException(
          "Search adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
