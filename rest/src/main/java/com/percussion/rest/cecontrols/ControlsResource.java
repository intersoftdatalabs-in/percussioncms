/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.cecontrols;

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
 * Read-only content editor control catalog for the Developer module (UI-01).
 *
 * <p>User control create/edit remains a later slice.
 */
@PSSiteManageBean(value = "restControlsResource")
@Path("/cecontrols")
@XmlRootElement
@Tag(name = "CE Controls", description = "Content editor control catalog (read-only)")
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
          "Lists system and user CE controls used by content type field editors. User control"
              + " registration remains a later slice.",
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
      description = "Loads one CE control by name. Write remains unsupported.",
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

  private IControlAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Extensions/Keywords peers)
      throw new WebApplicationException(
          "Control adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
