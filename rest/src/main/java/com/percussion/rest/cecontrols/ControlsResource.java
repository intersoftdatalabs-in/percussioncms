/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.cecontrols;

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
 * Content editor control catalog for the Developer module (UI-01).
 *
 * <p>Admin POST/PUT/DELETE persist <strong>user</strong> controls through {@link
 * IControlAdaptor} ({@code PSCustomControlManager} file + import list). System controls are
 * read-only. This resource is REST only — it does not add Developer SPA chrome.
 */
@PSSiteManageBean(value = "restControlsResource")
@Path("/cecontrols")
@XmlRootElement
@Tag(name = "CE Controls", description = "Content editor control catalog and user-control write")
public class ControlsResource {

  private final IControlAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ControlsResource(IControlAdaptor)}; catalog methods call {@link #requireAdaptor()}.
   */
  public ControlsResource() {
    this.adaptor = null;
  }

  @Autowired
  public ControlsResource(IControlAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List content editor controls",
      description =
          "Lists system and user CE controls used by content type field editors. Admin"
              + " create/save/delete of user controls are POST/PUT/DELETE on this resource."
              + " System controls remain read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ControlDef.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ControlDef> listControls() {
    try {
      List<ControlDef> list = requireAdaptor().listControls();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get content editor control detail",
      description =
          "Loads one CE control by name. Admin write of user controls is POST/PUT/DELETE on"
              + " this resource. System controls cannot be mutated.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ControlDef.class))),
        @ApiResponse(responseCode = "404", description = "Control not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ControlDef getControl(@PathParam("name") String name) {
    try {
      ControlDef def = requireAdaptor().findControlByName(name);
      if (def == null) {
        throw new WebApplicationException("Control not found", 404);
      }
      return def;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create user CE control",
      description =
          "Admin. Creates and persists a user CE control via PSCustomControlManager (XSL file"
              + " under rx_resources/stylesheets/controls plus import list). Name is required,"
              + " unique (case-insensitive), and must not contain whitespace or wildcards."
              + " Optional displayName, description, dimension (single|array|table), choiceSet"
              + " (none|required|optional), and xslSource are applied. Duplicate name is 409."
              + " System control names cannot be created (409). This is REST only — there is no"
              + " Developer SPA create chrome.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = ControlDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "A control with that name already exists"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ControlDef createControl(ControlDef body) {
    try {
      ControlDef created = requireAdaptor().createControl(body);
      if (created == null) {
        throw new WebApplicationException("Failed to create control", 500);
      }
      return created;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update user CE control",
      description =
          "Admin. Updates a user CE control by name. Name is the catalog key and is not renamed"
              + " on PUT. System controls are 409 (packaged files are not mutated). Unknown name"
              + " is 404. Optional xslSource replaces the stylesheet; omitted xslSource"
              + " regenerates a default stylesheet from metadata.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = ControlDef.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Control not found"),
        @ApiResponse(responseCode = "409", description = "System control cannot be mutated"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ControlDef updateControl(@PathParam("name") String name, ControlDef body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      ControlDef updated = requireAdaptor().saveControl(name, body);
      if (updated == null) {
        throw new WebApplicationException("Control not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{name}")
  @Operation(
      summary = "Delete user CE control",
      description =
          "Admin. Deletes a user CE control by name (removes the user XSL file and refreshes"
              + " imports). System controls are 409 (not deleted). Unknown name is 404."
              + " Following GET is 404 after a successful delete.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Control not found"),
        @ApiResponse(responseCode = "409", description = "System control cannot be deleted"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteControl(@PathParam("name") String name) {
    try {
      boolean deleted = requireAdaptor().deleteControl(name);
      if (!deleted) {
        throw new WebApplicationException("Control not found", 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin 403, duplicate 409, and system-control 409
   * are already {@link WebApplicationException}s from the adaptor.
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

  private IControlAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Extensions/Keywords peers)
      throw new WebApplicationException(
          "Control adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
