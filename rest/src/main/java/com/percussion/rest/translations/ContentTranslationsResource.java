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

package com.percussion.rest.translations;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST façade for content-item translation (P-Trans / #2429).
 *
 * <pre>
 *   POST /Rhythmyx/rest/content-explorer/translations
 *   GET  /Rhythmyx/rest/content-explorer/translations/{itemId}
 * </pre>
 *
 * <p>Thin HTTP layer over the same domain path SOAP {@code content.NewTranslations} uses. Does
 * <strong>not</strong> call SOAP from the SPA path — apibridge invokes {@code
 * IPSContentWs#newTranslations} in-process.
 *
 * <p><strong>Not exposed (product disposition pending on #2411/#2428):</strong> in-flight
 * translation queue filter, session content-locale context switch.
 */
@PSSiteManageBean(value = "restContentTranslationsResource")
@Path("/content-explorer/translations")
@Tag(
    name = "Content Explorer Translations",
    description =
        "Create and list content-item locale variants (NewTranslations public REST façade)")
public class ContentTranslationsResource {

  static Logger log = LogManager.getLogger(ContentTranslationsResource.class);

  private final IContentTranslationsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public ContentTranslationsResource() {
    this.adaptor = null;
  }

  @Autowired
  public ContentTranslationsResource(IContentTranslationsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create content-item locale variants",
      description =
          "Creates new locale variants for the supplied source content ids. Same domain path as"
              + " SOAP content.NewTranslations / IPSContentWs.newTranslations. When locales is"
              + " omitted, all system auto-translations are used.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK — created variants returned",
            content =
                @Content(schema = @Schema(implementation = CreateTranslationsResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body / contract"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error creating translations")
      })
  public CreateTranslationsResult createTranslations(CreateTranslationsRequest body) {
    if (body == null) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Request body is required.")
              .build());
    }
    try {
      return requireAdaptor().createTranslations(baseUri(), body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
    } catch (SecurityException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build());
    } catch (Exception e) {
      log.error(
          "Failed to create translations ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(
          e,
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Failed to create translations.")
              .build());
    }
  }

  @GET
  @Path("/{itemId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List locale variants for a content item",
      description =
          "Returns the item's current locale plus translation-category dependents. itemId may be a"
              + " hyphenated host-type-uuid GUID (for example 16777215-101-551) or a bare numeric"
              + " content id. Does not include in-flight queue status or session content-locale"
              + " context (product disposition).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(schema = @Schema(implementation = ItemTranslationVariants.class))),
        @ApiResponse(responseCode = "400", description = "Invalid item id"),
        @ApiResponse(responseCode = "403", description = "Caller cannot read the item"),
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public ItemTranslationVariants listItemVariants(
      @Parameter(name = "itemId", required = true, description = "Legacy content id or guid string")
          @PathParam("itemId")
          String itemId) {
    try {
      ItemTranslationVariants out = requireAdaptor().listItemVariants(baseUri(), itemId);
      if (out == null) {
        throw new WebApplicationException(
            Response.status(Response.Status.NOT_FOUND).entity("Item not found").build());
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
    } catch (SecurityException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build());
    } catch (Exception e) {
      log.error(
          "Failed to list translation variants for {} ({}): {}",
          itemId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          e,
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Failed to list translation variants.")
              .build());
    }
  }

  private URI baseUri() {
    return uriInfo == null ? null : uriInfo.getBaseUri();
  }

  private IContentTranslationsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Content translations adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
