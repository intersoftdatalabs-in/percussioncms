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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
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
 * Singleton auto-translation set for the Developer module (CD-18).
 *
 * <p>Admin GET/PUT of locale × content-type rows via {@link IAutoTranslationsAdaptor}. Writes
 * acquire the design lock and release it on save (no separate lock pair). Empty list clears.
 */
@PSSiteManageBean(value = "restAutoTranslationsResource")
@Path("/locales/auto-translations")
@XmlRootElement
@Tag(name = "Locales", description = "CMS locale design catalog and auto-translation set")
public class AutoTranslationsResource {

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger} and assert
   * unexpected-failure diagnostics (log4j-core ListAppender is not on the rest test classpath).
   */
  static Logger log = LogManager.getLogger(AutoTranslationsResource.class);

  private final IAutoTranslationsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public AutoTranslationsResource() {
    this.adaptor = null;
  }

  @Autowired
  public AutoTranslationsResource(IAutoTranslationsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get auto-translation set",
      description =
          "Admin. Returns the singleton auto-translation set (locale × content type rows) via"
              + " IPSContentDesignWs.loadTranslationSettings. Empty list when none are configured.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = AutoTranslationRow.class)))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<AutoTranslationRow> getAutoTranslations() {
    try {
      return requireAdaptor().getAutoTranslations(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to load auto-translations ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace auto-translation set",
      description =
          "Admin. Replaces the singleton auto-translation set. Loads with a held design lock and"
              + " releases on save (IPSContentDesignWs.saveTranslationSettings). Empty list"
              + " clears all rows. Unknown locale or content type is 400. Duplicate"
              + " locale/content-type rows are 400. Lock conflict is 409.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = AutoTranslationRow.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, unknown locale/content type, or duplicate row"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "409", description = "Design lock conflict"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<AutoTranslationRow> saveAutoTranslations(List<AutoTranslationRow> rows) {
    try {
      return requireAdaptor().saveAutoTranslations(uriInfo.getBaseUri(), rows);
    } catch (RuntimeException e) {
      throw mapWriteFailure(e);
    } catch (Exception e) {
      log.error(
          "Failed to save auto-translations ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Map adaptor write failures to HTTP status. Lock conflicts are always 409 via {@link
   * AutoTranslationDesignLockException}.
   */
  static WebApplicationException mapWriteFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof AutoTranslationDesignLockException) {
      return new WebApplicationException(e.getMessage(), 409);
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

  private IAutoTranslationsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Auto-translations adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
