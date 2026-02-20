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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST resource for Extension operations. Sunny Sal: "Extension operations? Bas yahi toh mera kaam
 * hai!"
 */
@PSSiteManageBean(value = "restExtensionsResource")
@Path("/extensions")
@XmlRootElement
@Tag(name = "Extensions", description = "Extension operations")
public class ExtensionsResource {

  @Autowired private IExtensionAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public ExtensionsResource() {
    // Default constructor
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
        @ApiResponse(responseCode = "404", description = "No Extensions found")
      })
  public List<Extension> getExtensions(
      @Parameter(
              name = "filter",
              description = "An extension filter options object",
              required = true)
          ExtensionFilterOptions filter) {
    var extensions = adaptor.getExtensions(uriInfo.getBaseUri(), filter);
    return new ExtensionList(extensions);
  }
}
