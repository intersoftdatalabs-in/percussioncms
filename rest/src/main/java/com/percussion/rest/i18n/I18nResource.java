/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.i18n;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/i18n")
@Tag(name = "i18n", description = "Internationalization and translations")
public class I18nResource {

    private static final Logger log = LogManager.getLogger(I18nResource.class);

    @Autowired
    private II18nAdaptor adaptor;

    @GET
    @Path("/translations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get translations",
            description = "Returns translation strings for the specified locale and optional prefix filter"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Translations returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Translations.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Server error")
    public Response getTranslations(
            @Parameter(description = "Locale code (e.g., en-us, es, hi)", required = true)
            @QueryParam("locale") @DefaultValue("en-us") String locale,

            @Parameter(description = "Optional prefix filter (e.g., perc.ui., javascript.)")
            @QueryParam("prefix") String prefix,

            @Parameter(description = "Optional comma-separated list of key prefixes to include")
            @QueryParam("prefixes") String prefixes) {

        try {
            List<String> prefixList = null;
            if (prefix != null && !prefix.isEmpty()) {
                prefixList = List.of(prefix);
            } else if (prefixes != null && !prefixes.isEmpty()) {
                prefixList = List.of(prefixes.split(","));
            }

            Translations translations = adaptor.getTranslations(locale, prefixList);
            return Response.ok(translations).build();
        } catch (Exception e) {
            log.error("Error fetching translations: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch translations", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/locales")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get available locales",
            description = "Returns list of all available locale codes"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Locales returned successfully"
    )
    public Response getLocales() {
        try {
            List<String> locales = adaptor.getAvailableLocales();
            return Response.ok(locales).build();
        } catch (Exception e) {
            log.error("Error fetching locales: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch locales", e.getMessage()))
                    .build();
        }
    }

    public II18nAdaptor getAdaptor() {
        return adaptor;
    }

    public void setAdaptor(II18nAdaptor adaptor) {
        this.adaptor = adaptor;
    }
}
