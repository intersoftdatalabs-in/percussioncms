/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.serverconfigs;

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
 * Read-only server configuration catalog for the Developer module (SY-02).
 *
 * <p>Save/edit remains a later slice.
 */
@PSSiteManageBean(value = "restServerConfigsResource")
@Path("/serverconfigs")
@XmlRootElement
@Tag(name = "Server Configs", description = "Server configuration files (read-only)")
public class ServerConfigsResource {

  private final IServerConfigAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ServerConfigsResource(IServerConfigAdaptor)}; catalog methods call {@link #requireAdaptor()}.
   */
  public ServerConfigsResource() {
    this.adaptor = null;
  }

  @Autowired
  public ServerConfigsResource(IServerConfigAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List server configuration files",
      description =
          "Lists named server configuration resources (logging, tidy, navigation, velocity macros,"
              + " etc.). Edit/save is a later slice.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ServerConfigSummary.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ServerConfigSummary> listConfigs() {
    try {
      List<ServerConfigSummary> list = requireAdaptor().listConfigs();
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
      summary = "Get server configuration detail",
      description =
          "Loads one configuration by enum name (e.g. LOG_CONFIG) including file content when"
              + " available.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ServerConfigSummary.class))),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ServerConfigSummary getConfig(@PathParam("name") String name) {
    try {
      ServerConfigSummary cfg = requireAdaptor().findConfigByName(name);
      if (cfg == null) {
        throw new WebApplicationException("Configuration not found", 404);
      }
      return cfg;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IServerConfigAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Extensions/Keywords peers)
      throw new WebApplicationException(
          "Server config adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
