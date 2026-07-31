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

package com.percussion.rest.about;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only "About" endpoint exposing the server version and license/copyright disclaimer.
 *
 * <p>Backed by the same string-resource keys printed to the console at server startup (see issue
 * #1529), so the UI About dialog and the startup log share a single source of truth.
 */
@PSSiteManageBean(value = "restAboutResource")
@Path("/about")
@XmlRootElement
@Tag(name = "About", description = "Server version and third-party license disclaimer")
public class AboutResource {

  private final IAboutAdaptor adaptor;

  public AboutResource() {
    this.adaptor = null;
  }

  @Autowired
  public AboutResource(IAboutAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server version and license disclaimer",
      description =
          "Returns the same version string and third-party license disclaimer text that is"
              + " printed to the console at server startup.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AboutDetail.class))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public AboutDetail getAbout() {
    try {
      return requireAdaptor().getAbout();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IAboutAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "About adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
