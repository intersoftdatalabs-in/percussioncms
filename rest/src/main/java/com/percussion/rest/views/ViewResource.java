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
 * CX view design catalog (UI-07 list/detail/write) plus view execute façade for Explorer.
 *
 * <p>Searches (UI-06) remain a separate catalog. Admin POST/PUT/DELETE persist through {@link
 * IViewAdaptor} ({@code IPSUiDesignWs} create/save/delete views). Execute is a separate façade
 * and is not invoked from write. Inbox-family custom-URL views ({@code sys_cxViews/inbox} and
 * documented peers) are executed on this same path; other custom URLs return 400. Inbox/system
 * custom-URL views cannot be updated or deleted here.
 */
@PSSiteManageBean(value = "restViewResource")
@Path("/views")
@XmlRootElement
@Tag(name = "Views", description = "CX view design catalog, write, and view execute")
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
      description =
          "Lists Content Explorer view definitions. Admin create/save/delete are POST/PUT/DELETE"
              + " on this resource.",
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
          "Loads one view by name or GUID string. Field criteria included when present. Admin"
              + " write is POST/PUT/DELETE on this resource.",
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
              + " Optional body may override folder scope (standard views), paging, and sort"
              + " as a ViewExecuteRequest envelope or a flat startIndex/maxResults object."
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

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create view",
      description =
          "Admin. Creates and persists a CX standard (field-criteria) view via"
              + " IPSUiDesignWs.createViews then saveViews (held design lock, released on save)."
              + " Name is required, unique (case-insensitive), and must not contain whitespace"
              + " or wildcards. Optional label, description, type (View default), and"
              + " displayFormatId are applied before save. Duplicate name is 409. Custom URL"
              + " views are not created here. Searches are not created here (use"
              + " /services/searches). Execute is not invoked on write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = ViewDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "409", description = "A view with that name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ViewDef createView(ViewDef body) {
    try {
      if (body != null) {
        body.setGuid(null);
        body.setId(0);
      }
      ViewDef created = requireAdaptor().createView(body);
      if (created == null) {
        throw new WebApplicationException("Failed to create view", 500);
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
      summary = "Update view",
      description =
          "Admin. Updates label, description, type, displayFormatId, and/or field criteria by"
              + " name or GUID. Name is the catalog key and is not renamed on PUT. Omitted"
              + " fields leave existing criteria unchanged; an empty fields array clears them."
              + " Unknown field names are 400. Loads with a design lock (overrideLock=false)"
              + " and releases on save. Unknown id is 404. Lock/dependency conflict is 409."
              + " Inbox-family and custom URL views are 409 (not mutated). Searches are not"
              + " saved here. Execute is not invoked on write.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = ViewDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "View not found"),
        @ApiResponse(
            responseCode = "409",
            description =
                "Design lock, dependency conflict, or Inbox/custom URL view cannot be mutated"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ViewDef updateView(@PathParam("idOrName") String idOrName, ViewDef body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      ViewDef updated = requireAdaptor().saveView(idOrName, body);
      if (updated == null) {
        throw new WebApplicationException("View not found", 404);
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
      summary = "Delete view",
      description =
          "Admin. Deletes a CX view by name or GUID via IPSUiDesignWs.deleteViews"
              + " (ignoreDependencies=false). Unknown id is 404. Lock/dependency conflict is 409."
              + " Inbox-family and custom URL views are 409 (not deleted). Following GET is 404"
              + " after a successful delete. Searches are not deleted here.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "View not found"),
        @ApiResponse(
            responseCode = "409",
            description =
                "Design lock, dependency conflict, or Inbox/custom URL view cannot be deleted"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteView(@PathParam("idOrName") String idOrName) {
    try {
      boolean deleted = requireAdaptor().deleteView(idOrName);
      if (!deleted) {
        throw new WebApplicationException("View not found", 404);
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

  private IViewAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Slots/Locales/Keywords/Searches)
      throw new WebApplicationException(
          "View adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
