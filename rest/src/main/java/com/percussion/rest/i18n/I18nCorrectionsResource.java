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

package com.percussion.rest.i18n;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BFF for {@code @mkd/language}: accepts correction JSON and delegates to the server-side GCM
 * integration. Browser never holds the GCM PAT.
 *
 * <p>Full path: {@code POST /Rhythmyx/rest/i18n/corrections}
 */
@PSSiteManageBean(value = "restI18nCorrectionsResource")
@Path("/i18n/corrections")
@Tag(
    name = "i18n corrections",
    description = "Crowd-sourced translation corrections from the modern UI language client")
public class I18nCorrectionsResource {

  private static final Logger log = LogManager.getLogger(I18nCorrectionsResource.class);

  private final I18nCorrectionsAdaptor adaptor;

  @Autowired
  public I18nCorrectionsResource(I18nCorrectionsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Submit an i18n correction",
      description =
          "Accepts @mkd/language CorrectionSubmission JSON. Requires authenticated session,"
              + " CSRF, and server-side role gate (perc.mkd.language.*).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Accepted",
            content = @Content(schema = @Schema(implementation = I18nCorrectionResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body"),
        @ApiResponse(responseCode = "403", description = "Not allowed / feature off for user"),
        @ApiResponse(responseCode = "503", description = "Integration not configured"),
        @ApiResponse(responseCode = "502", description = "Backend rejected the submission")
      })
  public I18nCorrectionResult submit(I18nCorrectionSubmission body) {
    if (body == null) {
      throw new WebApplicationException("Request body is required.", 400);
    }
    try {
      return adaptor.submit(body);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, 400);
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, 403);
    } catch (IllegalStateException e) {
      log.warn("i18n correction unavailable: {}", e.getMessage());
      throw new WebApplicationException(e.getMessage(), e, 503);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("i18n correction submit failed", e);
      throw new WebApplicationException("Failed to submit correction.", e, 502);
    }
  }
}
