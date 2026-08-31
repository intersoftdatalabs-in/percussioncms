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

package com.percussion.rest.locales;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CMS locale catalog for the Developer module (CD-18).
 *
 * <p>List/detail are unauthenticated-beyond-session catalog reads. Create, update, and delete are
 * Admin design writes via {@link ILocalesAdaptor} (held design lock, same backends SOAP uses).
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources.
 */
@PSSiteManageBean(value = "restLocalesResource")
@Path("/locales")
@XmlRootElement
@Tag(name = "Locales", description = "CMS locale design catalog")
public class LocalesResource {

  /**
   * Path template that must not swallow {@code /locales/auto-translations} (CD-18 singleton set).
   * CXF otherwise matches {@code GET/PUT /locales/{idOrLang}} first and 404s "Locale not found"
   * (#4039).
   */
  static final String ID_OR_LANG = "{idOrLang:(?!auto-translations$).+}";

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger} and assert
   * unexpected-failure diagnostics (log4j-core ListAppender is not on the rest test classpath).
   */
  static Logger log = LogManager.getLogger(LocalesResource.class);

  private final ILocalesAdaptor adaptor;

  /**
   * Sub-resource so {@code /locales/auto-translations} is reachable even when CXF prefers {@code
   * /{idOrLang}} or a stale exploded {@code sitemanage-beans.xml} omits {@code
   * restAutoTranslationsResource} (#4039).
   */
  @Autowired(required = false)
  AutoTranslationsResource autoTranslationsResource;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #LocalesResource(ILocalesAdaptor)}; list/detail methods call {@link #requireAdaptor()}.
   */
  public LocalesResource() {
    this.adaptor = null;
  }

  @Autowired
  public LocalesResource(ILocalesAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List CMS locales",
      description =
          "Lists CMS locales (language string, label, status, base flag). Auto-translation"
              + " settings are GET/PUT /locales/auto-translations.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = LocaleSummary.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<LocaleSummary> listLocales() {
    try {
      return requireAdaptor().listLocales(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list locales ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/auto-translations")
  @Produces({MediaType.APPLICATION_JSON})
  public List<AutoTranslationRow> getAutoTranslationsSet() {
    return requireAutoTranslations().getAutoTranslations();
  }

  @PUT
  @Path("/auto-translations")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public List<AutoTranslationRow> saveAutoTranslationsSet(List<AutoTranslationRow> rows) {
    return requireAutoTranslations().saveAutoTranslations(rows);
  }

  AutoTranslationsResource requireAutoTranslations() {
    if (autoTranslationsResource == null) {
      throw new WebApplicationException(
          "Auto-translations adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return autoTranslationsResource;
  }

  @GET
  @Path("/" + ID_OR_LANG)
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get CMS locale detail",
      description =
          "Loads one locale by language string (e.g. en-us) or numeric locale id. Auto-translation"
              + " settings are GET/PUT /locales/auto-translations.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = LocaleDetail.class))),
        @ApiResponse(responseCode = "404", description = "Locale not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public LocaleDetail getLocale(@PathParam("idOrLang") String idOrLang) {
    try {
      LocaleDetail detail = requireAdaptor().getLocale(uriInfo.getBaseUri(), idOrLang);
      if (detail == null) {
        throw new WebApplicationException("Locale not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      // Preserve mapped HTTP errors (e.g. 503 misconfiguration from requireAdaptor)
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to load locale {} ({}): {}", idOrLang, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create CMS locale",
      description =
          "Admin. Creates and persists a locale via IPSContentDesignWs.createLocales then"
              + " saveLocales (held design lock, released on save). languageString and label are"
              + " required. Duplicate language string is 409. Lock/dependency conflict is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = LocaleDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "409",
            description = "Language string already exists, or design lock/dependency conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public LocaleDetail createLocale(LocaleDetail body) {
    try {
      return requireAdaptor().createLocale(uriInfo.getBaseUri(), body);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error("Failed to create locale ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/" + ID_OR_LANG)
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update CMS locale",
      description =
          "Admin. Updates label, description, status, and/or baseLocale by language string or"
              + " numeric id. languageString is immutable. Loads with a design lock and releases"
              + " on save. Unknown id is 404. Lock/dependency conflict is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = LocaleDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Locale not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public LocaleDetail updateLocale(@PathParam("idOrLang") String idOrLang, LocaleDetail body) {
    try {
      LocaleDetail detail = requireAdaptor().updateLocale(uriInfo.getBaseUri(), idOrLang, body);
      if (detail == null) {
        throw new WebApplicationException("Locale not found", 404);
      }
      return detail;
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to update locale {} ({}): {}",
          idOrLang,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @DELETE
  @Path("/" + ID_OR_LANG)
  @Operation(
      summary = "Delete CMS locale",
      description =
          "Admin. Deletes a locale by language string or numeric id via"
              + " IPSContentDesignWs.deleteLocales (ignoreDependencies=false). Unknown id is 404."
              + " Lock or remaining dependents is 409.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Locale not found"),
        @ApiResponse(responseCode = "409", description = "Design lock or dependency conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteLocale(@PathParam("idOrLang") String idOrLang) {
    try {
      requireAdaptor().deleteLocale(uriInfo.getBaseUri(), idOrLang);
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to delete locale {} ({}): {}",
          idOrLang,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Lock/dependency conflicts are always 409 via {@link
   * LocaleDesignLockException} or a 409 {@link WebApplicationException}.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof LocaleDesignLockException) {
      return new WebApplicationException(e.getMessage(), 409);
    }
    if (e instanceof LocaleNotFoundException) {
      return new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Locale not found", 404);
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

  private ILocalesAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with slots/keywords)
      throw new WebApplicationException(
          "Locales adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
