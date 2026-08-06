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
import jakarta.ws.rs.GET;
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
 * Read-only CMS locale catalog for the Developer module (CD-18).
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources.
 */
@PSSiteManageBean(value = "restLocalesResource")
@Path("/locales")
@XmlRootElement
@Tag(name = "Locales", description = "CMS locale design catalog (read-only)")
public class LocalesResource {

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger} and assert
   * unexpected-failure diagnostics (log4j-core ListAppender is not on the rest test classpath).
   */
  static Logger log = LogManager.getLogger(LocalesResource.class);

  private final ILocalesAdaptor adaptor;

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
          "Lists CMS locales (language string, label, status, base flag). Create/edit/delete and"
              + " auto-translation settings are later slices.",
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
  @Path("/{idOrLang}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get CMS locale detail",
      description =
          "Loads one locale by language string (e.g. en-us) or numeric locale id. Write and"
              + " auto-translation remain unsupported (see designGaps).",
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

  private ILocalesAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure (align with slots/keywords)
      throw new WebApplicationException(
          "Locales adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
