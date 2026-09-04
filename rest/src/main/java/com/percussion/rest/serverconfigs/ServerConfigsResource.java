/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.serverconfigs;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
 * Server configuration catalog for the Developer module (SY-02).
 *
 * <p>Admin PUT updates an allow-listed configuration file body ({@code PSConfigurationTypes}
 * names only). Arbitrary filesystem paths are rejected. SPA save chrome is a later slice.
 */
@PSSiteManageBean(value = "restServerConfigsResource")
@Path("/serverconfigs")
@XmlRootElement
@Tag(name = "Server Configs", description = "Server configuration files (allow-listed read/write)")
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
              + " etc.). Admin update of an allow-listed file body is PUT /{name}.",
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
              + " available. Non-allow-listed names are 404.",
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

  @PUT
  @Path("/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update allow-listed server configuration file",
      description =
          "Admin. Updates the file body for an allow-listed configuration key"
              + " (PSConfigurationTypes enum name such as LOG_CONFIG). Path name is the catalog"
              + " key and is not renamed. Body must include content (file text). Names outside the"
              + " allow-list (path traversal, unknown enum) are 404 — no arbitrary filesystem"
              + " write. Locking / concurrent edit remain design gaps.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = ServerConfigSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input (missing body/content)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Configuration not found / not allow-listed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ServerConfigSummary updateConfig(
      @PathParam("name") String name, ServerConfigSummary body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      ServerConfigSummary updated = requireAdaptor().updateConfig(name, body);
      if (updated == null) {
        throw new WebApplicationException("Configuration not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin 403 and not-found 404 are already {@link
   * WebApplicationException}s from the adaptor when applicable.
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

  private IServerConfigAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with Extensions/Keywords peers)
      throw new WebApplicationException(
          "Server config adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
