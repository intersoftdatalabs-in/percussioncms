/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.applicationfiles;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * XML application CMS/resource files for the Developer module (SY-05).
 *
 * <p>Distinct from {@code /serverconfigs} (SY-02 fixed server configuration allow-list). Paths are
 * relative under a catalog application root; traversal and absolute paths are rejected. Admin PUT
 * updates UTF-8 file bodies. SPA editor chrome is a later slice.
 */
@PSSiteManageBean(value = "restApplicationFilesResource")
@Path("/applicationfiles")
@XmlRootElement
@Tag(
    name = "Application Files",
    description = "XML application CMS/resource files (path-safe list/get/put)")
public class ApplicationFilesResource {

  private final IApplicationFileAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ApplicationFilesResource(IApplicationFileAdaptor)}; catalog methods call {@link
   * #requireAdaptor()}.
   */
  public ApplicationFilesResource() {
    this.adaptor = null;
  }

  @Autowired
  public ApplicationFilesResource(IApplicationFileAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/{app}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List application CMS/resource files",
      description =
          "Lists relative file paths under a catalog XML application root (Workbench System Design"
              + " CMS/resource tree). Application name must resolve in the object-store catalog."
              + " Distinct from /serverconfigs (SY-02). Edit of a single file body is PUT"
              + " /{app}/content?path=.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ApplicationFileSummary.class)))),
        @ApiResponse(responseCode = "404", description = "Application not found / not allow-listed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApplicationFileSummary> listFiles(@PathParam("app") String app) {
    try {
      List<ApplicationFileSummary> list = requireAdaptor().listFiles(app);
      if (list == null) {
        throw new WebApplicationException(
            "Application not found",
            Response.status(Response.Status.NOT_FOUND).entity("Application not found").build());
      }
      return list;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/{app}/content")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get application CMS/resource file",
      description =
          "Loads one file under the application root. Query path is relative (use / separators)."
              + " Absolute paths, parent traversal, and unknown apps are 404 — no arbitrary"
              + " filesystem read. Distinct from GET /serverconfigs/{name} (SY-02).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ApplicationFileSummary.class))),
        @ApiResponse(responseCode = "404", description = "Application or file not found / unsafe path"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationFileSummary getFile(
      @PathParam("app") String app, @QueryParam("path") String path) {
    try {
      ApplicationFileSummary file = requireAdaptor().getFile(app, path);
      if (file == null) {
        throw new WebApplicationException(
            "Application file not found",
            Response.status(Response.Status.NOT_FOUND)
                .entity("Application file not found")
                .build());
      }
      return file;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/{app}/content")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update application CMS/resource file",
      description =
          "Admin. Replaces UTF-8 text content for a relative path under a catalog application"
              + " root. Query path is not taken from the body path field for persistence. Absolute"
              + " paths, parent traversal, and unknown apps are 404 — no arbitrary filesystem"
              + " write. Distinct from PUT /serverconfigs/{name} (SY-02). Locking / binary"
              + " round-trip / create-folder remain design gaps.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = ApplicationFileSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input (missing body/content/path)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application or path not found / not allow-listed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationFileSummary putFile(
      @PathParam("app") String app,
      @QueryParam("path") String path,
      ApplicationFileSummary body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      if (path == null || path.isBlank()) {
        throw new IllegalArgumentException("path is required");
      }
      ApplicationFileSummary updated = requireAdaptor().putFile(app, path, body);
      if (updated == null) {
        throw new WebApplicationException(
            "Application file not found",
            Response.status(Response.Status.NOT_FOUND)
                .entity("Application file not found")
                .build());
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
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
      return new WebApplicationException(
          e.getMessage(),
          Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
    }
    return new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
  }

  private IApplicationFileAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Application file adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
