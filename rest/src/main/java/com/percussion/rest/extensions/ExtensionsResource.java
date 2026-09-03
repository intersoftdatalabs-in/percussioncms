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

// REFACTORED: CP-JAVA11

package com.percussion.rest.extensions;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST resource for Extension catalog and Admin user-extension write (SY-01).
 *
 * <p>Admin POST/PUT/DELETE persist <strong>user</strong> extensions through {@link
 * IExtensionAdaptor} ({@code IPSExtensionService}). System and handler-owned extensions are
 * read-only. This resource is REST only — it does not add Developer SPA chrome.
 */
@PSSiteManageBean(value = "restExtensionsResource")
@Path("/extensions")
@XmlRootElement
@Tag(name = "Extensions", description = "Extension catalog and user-extension write")
public class ExtensionsResource {

  private final IExtensionAdaptor adaptor;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ExtensionsResource(IExtensionAdaptor)}; catalog methods call {@link #requireAdaptor()}.
   */
  public ExtensionsResource() {
    this.adaptor = null;
  }

  @Autowired
  public ExtensionsResource(IExtensionAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  /**
   * Developer P0 catalog: list all extensions (UI/exit design inventory).
   *
   * <p>Uses an empty filter. Failures surface as 500 rather than empty lists when the adaptor
   * throws.
   */
  @GET
  @Path("/catalog")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List extensions (catalog)",
      description =
          "Lists registered server extensions for the Developer module. Admin"
              + " register/update/delete of user extensions are POST/PUT/DELETE on this"
              + " resource. System and handler-owned extensions remain read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Extension.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<Extension> listExtensionsCatalog() {
    try {
      List<Extension> list = requireAdaptor().listExtensions(uriInfo.getBaseUri());
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /** Detail by FQN or extension short name. Query param avoids path issues with FQN slashes. */
  @GET
  @Path("/catalog/item")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get extension detail",
      description =
          "Loads one extension by FQN or extension name (query param key). Admin write of user"
              + " extensions is POST/PUT/DELETE on this resource. System and handler-owned"
              + " extensions cannot be mutated.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Extension.class))),
        @ApiResponse(responseCode = "404", description = "Extension not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Extension getExtensionCatalogItem(
      @Parameter(name = "key", required = true, description = "FQN or extension name")
          @QueryParam("key")
          String key) {
    try {
      Extension ext = requireAdaptor().findExtensionByKey(uriInfo.getBaseUri(), key);
      if (ext == null) {
        throw new WebApplicationException("Extension not found", 404);
      }
      return ext;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Register user extension",
      description =
          "Admin. Registers (installs) a user extension via IPSExtensionService under context"
              + " user/. extensionName and at least one supportedInterfaces entry are required."
              + " handlerName defaults to Java. Duplicate FQN is 409. System/handler contexts"
              + " cannot be registered (409). Developer → Extensions SPA also uses this"
              + " endpoint for user-extension create.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = Extension.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "Extension already exists or immutable context"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Extension registerExtension(Extension body) {
    try {
      Extension created = requireAdaptor().registerExtension(uriInfo.getBaseUri(), body);
      if (created == null) {
        throw new WebApplicationException("Failed to register extension", 500);
      }
      return created;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/catalog/item")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update user extension",
      description =
          "Admin. Updates mutable fields of a user extension by FQN or extension name (query"
              + " param key). Identity (handler/context/name) is not renamed on PUT. System and"
              + " handler-owned extensions are 409. Unknown key is 404.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = Extension.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Extension not found"),
        @ApiResponse(
            responseCode = "409",
            description = "System or handler-owned extension cannot be mutated"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Extension updateExtension(
      @Parameter(name = "key", required = true, description = "FQN or extension name")
          @QueryParam("key")
          String key,
      Extension body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      Extension updated = requireAdaptor().updateExtension(uriInfo.getBaseUri(), key, body);
      if (updated == null) {
        throw new WebApplicationException("Extension not found", 404);
      }
      return updated;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/catalog/item")
  @Operation(
      summary = "Delete user extension",
      description =
          "Admin. Deletes a user extension by FQN or extension name (query param key). System"
              + " and handler-owned extensions are 409 (not deleted). Unknown key is 404."
              + " Following GET is 404 after a successful delete.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Extension not found"),
        @ApiResponse(
            responseCode = "409",
            description = "System or handler-owned extension cannot be deleted"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteExtension(
      @Parameter(name = "key", required = true, description = "FQN or extension name")
          @QueryParam("key")
          String key) {
    try {
      boolean deleted = requireAdaptor().deleteExtension(uriInfo.getBaseUri(), key);
      if (!deleted) {
        throw new WebApplicationException("Extension not found", 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Lists Extensions available on the system.
   *
   * @param filter An extension filter options object
   * @return List of Extensions matching the filter
   */
  @POST
  @Path("/list")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List Extensions available on the system",
      description = "Returns a list of Extensions that match the supplied ExtensionFilterOptions",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Extension.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "404", description = "No Extensions found")
      })
  public List<Extension> getExtensions(
      @Parameter(
              name = "filter",
              description = "An extension filter options object",
              required = true)
          ExtensionFilterOptions filter) {
    try {
      var extensions = requireAdaptor().getExtensions(uriInfo.getBaseUri(), filter);
      return new ExtensionList(extensions);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Admin 403, duplicate 409, and immutable 409 are
   * already {@link WebApplicationException}s from the adaptor.
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

  private IExtensionAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with View/Slots/Locales peers)
      throw new WebApplicationException(
          "Extension adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
