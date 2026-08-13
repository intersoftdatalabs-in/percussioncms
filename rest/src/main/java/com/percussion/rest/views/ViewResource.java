/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * CX view design catalog (UI-07 list/detail) plus view execute façade for Explorer.
 *
 * <p>Searches (UI-06) remain a separate catalog. Inbox-family custom-URL views ({@code
 * sys_cxViews/inbox} and documented peers) are executed on this same path; other custom URLs
 * return 400.
 */
@PSSiteManageBean(value = "restViewResource")
@Path("/views")
@XmlRootElement
@Tag(name = "Views", description = "CX view design catalog and view execute")
public class ViewResource {

  private final IViewAdaptor adaptor;

  public ViewResource() {
    this.adaptor = null;
  }

  @Autowired
  public ViewResource(IViewAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List view definitions",
      description = "Lists Content Explorer view definitions. Create/edit/delete are later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = ViewDef.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ViewDef> listViews() {
    try {
      List<ViewDef> list = requireAdaptor().listViews();
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
      summary = "Get view detail",
      description =
          "Loads one view by name or GUID string. Field criteria included when present. Write"
              + " remains unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ViewDef.class))),
        @ApiResponse(responseCode = "404", description = "View not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ViewDef getView(@PathParam("idOrName") String idOrName) {
    try {
      ViewDef view = requireAdaptor().findViewByKey(idOrName);
      if (view == null) {
        throw new WebApplicationException("View not found", 404);
      }
      return view;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Execute a design view (standard field-criteria or Inbox-family custom URL). Path is
   * multi-segment so it does not collide with {@code GET /{idOrName}} catalog detail.
   */
  @POST
  @Path("/{idOrName}/execute")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Execute a design view",
      description =
          "Loads the CX view design by name, GUID, or id from the views catalog (not searches)"
              + " and executes it server-side. Standard views use design field operators, display"
              + " format, max results, and case sensitivity. Custom URL views in the Inbox family"
              + " (sys_cxViews/inbox, outbox, recent, session, checkedoutbyme,"
              + " duplicatefolderpaths) invoke the classic app resource and return Explorer rows."
              + " Optional body may override folder scope (standard views), paging, and sort."
              + " Unsupported custom URLs return 400.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ViewExecuteResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body or unsupported view type"),
        @ApiResponse(responseCode = "404", description = "View not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ViewExecuteResult executeView(
      @PathParam("idOrName") String idOrName, ViewExecuteRequest body) {
    try {
      ViewExecuteResult result = requireAdaptor().executeView(idOrName, body);
      if (result == null) {
        throw new WebApplicationException("View not found", 404);
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

  private IViewAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Slots/Locales/Keywords/Searches)
      throw new WebApplicationException(
          "View adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
