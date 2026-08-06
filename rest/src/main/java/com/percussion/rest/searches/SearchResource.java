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
import jakarta.ws.rs.GET;
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
 * Read-only CX search catalog for the Developer module (UI-06 list/detail).
 *
 * <p>Views (UI-07) remain a separate later slice.
 */
@PSSiteManageBean(value = "restSearchResource")
@Path("/searches")
@XmlRootElement
@Tag(name = "Searches", description = "CX search design catalog (read-only)")
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
          "Lists Content Explorer search definitions (not views). Create/edit/delete are later"
              + " slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = SearchDef.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<SearchDef> listSearches() {
    try {
      List<SearchDef> list = requireAdaptor().listSearches();
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
          "Loads one search by name or GUID string. Field criteria included when present. Write"
              + " remains unsupported.",
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

  private ISearchAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Slots/Locales/Keywords)
      throw new WebApplicationException(
          "Search adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
