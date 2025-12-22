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

package com.percussion.rest.jexl;

import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.ExtensionList;
import com.percussion.rest.extensions.IExtensionAdaptor;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides JEXL-related RESTful resources. Sunny Sal: "JEXL extensions? Bas yahi toh mera kaam
 * hai!"
 */
@PSSiteManageBean(value = "restJexlResource")
@Path("/jexl")
@XmlRootElement
@Tag(name = "JEXL Language Extensions", description = "Jexl related operations")
public class JexlResource {

  @Autowired private IExtensionAdaptor extensionAdaptor;

  @Context private UriInfo uriInfo;

  public JexlResource() {
    // Default constructor
  }

  /**
   * Returns a list of all registered JEXL extensions on the system.
   *
   * @return ExtensionList of JEXL extensions.
   */
  @GET
  @Path("/extensions")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Returns a list of all registered Jexl extensions on the System",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Extension.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ExtensionList listLocationSchemeGenerators() {
    var filter = new ExtensionFilterOptions();
    filter.setInterfacePattern("com.percussion.extension.IPSJexlExpression");
    return new ExtensionList(extensionAdaptor.getExtensions(uriInfo.getBaseUri(), filter));
  }
}
