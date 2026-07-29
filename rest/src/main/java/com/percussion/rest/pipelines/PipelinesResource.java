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

package com.percussion.rest.pipelines;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only catalog of classic XML Applications (pipeline packages) for the Developer module.
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources ({@code Keywords},
 * {@code Slots}).
 */
@PSSiteManageBean(value = "restPipelinesResource")
@Path("/pipelines")
@XmlRootElement
@Tag(name = "Pipelines", description = "Data pipeline / XML application design catalog (read-only)")
public class PipelinesResource {

  private final IPipelinesAdaptor adaptor;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #PipelinesResource(IPipelinesAdaptor)}; invoking list methods without injection fails with a
   * clear diagnostic rather than a bare NPE.
   */
  public PipelinesResource() {
    this.adaptor = null;
  }

  @Autowired
  public PipelinesResource(IPipelinesAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List pipeline applications",
      description =
          "Lists non-hidden server applications (classic XML Applications) visible to the"
              + " current user. Supports optional name filter and limit/offset. Editor /"
              + " start-stop / IR import are later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ApplicationSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApplicationSummary> listApplications(
      @QueryParam("name") String name,
      @QueryParam("limit") @DefaultValue("500") int limit,
      @QueryParam("offset") @DefaultValue("0") int offset) {
    try {
      IPipelinesAdaptor bridge = requireAdaptor();
      return bridge.listApplications(uriInfo.getBaseUri(), name, limit, offset);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause; matches Keywords/Slots catalog resources
      throw new WebApplicationException(e, 500);
    }
  }

  private IPipelinesAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Pipelines adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
