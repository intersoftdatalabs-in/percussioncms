/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.apikeys;

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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Developer API key scope editor (FR #6481 / parent #1690).
 *
 * <p>There is no Workbench twin for API keys: the scope catalog and least-privilege presets are
 * product-defined in {@link ApiKeyScopeCatalog}, assignments live in the sitemanage JSON
 * registry (see {@code ApiKeyScopeAdaptor}), and this module owns only HTTP + wire shapes per
 * {@code rest/AGENTS.md}.
 *
 * <p>PUT creates or replaces the scope set for a named key (Admin). Reads list the catalog,
 * presets, and stored assignments.
 */
@PSSiteManageBean(value = "restApiKeysResource")
@Path("/developer/apikeys")
@XmlRootElement
@Tag(
    name = "Developer API keys",
    description = "API key scope catalog, least-privilege presets, and per-key scope assignments")
public class ApiKeysResource {

  private final IApiKeyScopeAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ApiKeysResource(IApiKeyScopeAdaptor)}; endpoint methods call {@link #requireAdaptor()}.
   */
  public ApiKeysResource() {
    this.adaptor = null;
  }

  @Autowired
  public ApiKeysResource(IApiKeyScopeAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/scopes")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List assignable API key scopes",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ApiKeyScope.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApiKeyScope> listScopes() {
    try {
      List<ApiKeyScope> list = requireAdaptor().listScopes();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/presets")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List least-privilege scope presets",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ApiKeyScopePreset.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApiKeyScopePreset> listPresets() {
    try {
      List<ApiKeyScopePreset> list = requireAdaptor().listPresets();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/keys")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List API key scope assignments",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ApiKeyAssignment.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApiKeyAssignment> listKeys() {
    try {
      List<ApiKeyAssignment> list = requireAdaptor().listKeys();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/keys/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get one API key scope assignment",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ApiKeyAssignment.class))),
        @ApiResponse(responseCode = "404", description = "Key not registered"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApiKeyAssignment getKey(@PathParam("name") String name) {
    try {
      ApiKeyAssignment assignment = requireAdaptor().getKey(name);
      if (assignment == null) {
        throw new WebApplicationException("API key not found", 404);
      }
      return assignment;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/keys/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create or replace one API key scope assignment",
      description =
          "Admin. Creates the named key or replaces its scope set. Every scope must be a"
              + " catalog key (see GET .../scopes); unknown scopes are 400. A body name, when"
              + " present, must match the path name. The response recomputes presetId (null"
              + " for custom combinations).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Stored",
            content = @Content(schema = @Schema(implementation = ApiKeyAssignment.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name or unknown scope"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApiKeyAssignment saveKey(@PathParam("name") String name, ApiKeyAssignment body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      return requireAdaptor().saveKey(name, body);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
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

  private IApiKeyScopeAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "API key scope adaptor not configured",
          jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
