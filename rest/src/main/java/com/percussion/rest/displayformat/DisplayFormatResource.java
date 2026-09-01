/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.displayformat;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Display format catalog for the Developer module (UI-05 list/detail/write) and the modern Content
 * Explorer folder list (issue #2400 / FR-027).
 *
 * <p>Admin POST/PUT/DELETE persist through {@link IDisplayFormatAdaptor} over {@code
 * IPSUiDesignWs} (same SOAP operations Workbench uses). Developer SPA chrome is out of scope.
 */
@PSSiteManageBean(value = "restDisplayFormatResource")
@Path("/displayformats")
@XmlRootElement
@Tag(name = "Display Formats", description = "CX display format design catalog (UI-05 CRUD)")
public class DisplayFormatResource {

  private static final Logger log = LogManager.getLogger(DisplayFormatResource.class);

  private final IDisplayFormatAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public DisplayFormatResource() {
    this.adaptor = null;
  }

  @Autowired
  public DisplayFormatResource(IDisplayFormatAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List display formats",
      description =
          "Lists Content Explorer display formats with columns and usage flags. Optional filters"
              + " support the modern Explorer folder list (validForFolder) and search/view UIs"
              + " (validForViewsAndSearches). Admin POST/PUT/DELETE persist via IPSUiDesignWs.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = DisplayFormat.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<DisplayFormat> listDisplayFormats(
      @Parameter(description = "When true, only formats valid for folder list views")
          @QueryParam("validForFolder")
          Boolean validForFolder,
      @Parameter(description = "When true, only formats valid for views and searches")
          @QueryParam("validForViewsAndSearches")
          Boolean validForViewsAndSearches) {
    try {
      List<DisplayFormat> list = requireAdaptor().findAllDisplayFormats();
      if (list == null || list.isEmpty()) {
        return new DisplayFormatList();
      }
      if (validForFolder == null && validForViewsAndSearches == null) {
        return asDisplayFormatList(list);
      }
      List<DisplayFormat> filtered = new ArrayList<>(list.size());
      for (DisplayFormat df : list) {
        if (df == null) {
          continue;
        }
        if (Boolean.TRUE.equals(validForFolder) && !df.isValidForFolder()) {
          continue;
        }
        if (Boolean.FALSE.equals(validForFolder) && df.isValidForFolder()) {
          continue;
        }
        if (Boolean.TRUE.equals(validForViewsAndSearches) && !df.isValidForViewsAndSearches()) {
          continue;
        }
        if (Boolean.FALSE.equals(validForViewsAndSearches) && df.isValidForViewsAndSearches()) {
          continue;
        }
        filtered.add(df);
      }
      return asDisplayFormatList(filtered);
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
      summary = "Get display format detail",
      description =
          "Loads one display format by internal name or GUID string.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DisplayFormat.class))),
        @ApiResponse(responseCode = "404", description = "Display format not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public DisplayFormat getDisplayFormat(@PathParam("idOrName") String idOrName) {
    try {
      DisplayFormat df = requireAdaptor().findDisplayFormatByKey(idOrName);
      if (df == null) {
        // Generic body: do not echo raw idOrName (path probing).
        throw new WebApplicationException("Display format not found", 404);
      }
      return df;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create display format",
      description =
          "Admin. Creates and persists a display format via IPSUiDesignWs.createDisplayFormats"
              + " then saveDisplayFormats (Workbench Finish, held design lock released on save)."
              + " Name (or internalName) is required, unique (case-insensitive), and must not"
              + " contain whitespace or wildcards. Optional label/displayName and description are"
              + " applied before save. Duplicate name is 409. Usage flags on GET are derived from"
              + " columns (same as Workbench).",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = DisplayFormat.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(
            responseCode = "409",
            description = "A display format with that name already exists"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response createDisplayFormat(DisplayFormat body) {
    try {
      DisplayFormat createBody = body == null ? null : DisplayFormat.copyForCreate(body);
      DisplayFormat created = requireAdaptor().createDisplayFormat(createBody);
      if (created == null) {
        throw new WebApplicationException("Failed to create display format", 500);
      }
      Response.ResponseBuilder rb = Response.status(Response.Status.CREATED).entity(created);
      URI location = createdLocation(created);
      if (location != null) {
        rb.location(location);
      }
      return rb.build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to create display format ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update display format",
      description =
          "Admin. Updates label/displayName and/or description by internal name or GUID. When"
              + " columns is present, the column list is replaced (add/remove/reorder). When"
              + " allowedCommunities is present, community visibility is replaced (empty array"
              + " is all communities). Name is the catalog key and is not renamed on PUT. Loads"
              + " with a design lock (overrideLock=false) and releases on save. Usage flags on"
              + " GET remain derived from columns. Unknown id is 404. Invalid column source or"
              + " unknown community is 400. Lock/dependency conflict is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = DisplayFormat.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Display format not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public DisplayFormat updateDisplayFormat(
      @PathParam("idOrName") String idOrName, DisplayFormat body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      DisplayFormat existing = requireAdaptor().findDisplayFormatByKey(idOrName);
      if (existing == null) {
        throw new WebApplicationException("Display format not found", 404);
      }
      DisplayFormat updated = requireAdaptor().updateDisplayFormat(idOrName, body);
      if (updated == null) {
        throw new WebApplicationException("Display format not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update display format {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/{idOrName}")
  @Operation(
      summary = "Delete display format",
      description =
          "Admin. Deletes a display format by internal name or GUID via"
              + " IPSUiDesignWs.deleteDisplayFormats (ignoreDependencies=false). Unknown id is"
              + " 404. In-use or lock conflict is 409 (the lock is not stolen). Following GET is"
              + " 404 after a successful delete.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Display format not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Display format has dependents, or design lock conflict"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteDisplayFormat(@PathParam("idOrName") String idOrName) {
    try {
      boolean deleted = requireAdaptor().deleteDisplayFormat(idOrName);
      if (!deleted) {
        throw new WebApplicationException("Display format not found", 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete display format {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status without sniffing exception text. Admin/session 403
   * and duplicate/lock/dependency 409 must already be {@link WebApplicationException}s from the
   * adaptor (typed {@code PSLockErrorException} / dependency errors are wrapped there).
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

  private URI createdLocation(DisplayFormat created) {
    if (uriInfo == null || created == null) {
      return null;
    }
    String key =
        firstNonBlank(created.getName(), created.getInternalName(), created.getGuidString());
    if (key == null) {
      return null;
    }
    return uriInfo.getAbsolutePathBuilder().path(key).build();
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v.trim();
      }
    }
    return null;
  }

  /**
   * Return a {@link DisplayFormatList} so Jackson uses this package's {@code
   * JacksonContextResolver} (WRAP_ROOT_VALUE) instead of a raw {@code ArrayList} mapper.
   * Empty catalogs use the same envelope ({@code {"DisplayFormatList":[]}}), not a bare
   * {@code []}. Clients should unwrap {@code DisplayFormatList} (Developer SPA already
   * does).
   */
  private static DisplayFormatList asDisplayFormatList(List<DisplayFormat> list) {
    if (list instanceof DisplayFormatList displayFormatList) {
      return displayFormatList;
    }
    return new DisplayFormatList(list);
  }

  private IDisplayFormatAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Display format adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
