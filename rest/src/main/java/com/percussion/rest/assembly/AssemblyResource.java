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

package com.percussion.rest.assembly;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/** Assembly preview location for modern Content Explorer (replaces previewslotvariant HTML). */
@PSSiteManageBean(value = "restAssemblyResource")
@Path("/assembly")
@XmlRootElement
@Tag(name = "Assembly", description = "Assembly preview location")
public class AssemblyResource {

  private final IAssemblyAdaptor adaptor;

  /** No-arg constructor for bean-discovery edge cases. */
  public AssemblyResource() {
    this.adaptor = null;
  }

  /**
   * Production constructor.
   *
   * @param adaptor assembly adaptor, never {@code null} in the live webapp
   */
  @Autowired
  public AssemblyResource(IAssemblyAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/preview-location")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Assembly preview location",
      description =
          "Returns a context-root-relative assembler preview URL for a content item and"
              + " template. Replaces sys_cxSupport previewslotvariant HTML redirects.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PreviewLocation.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid ids"),
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PreviewLocation previewLocation(
      @Parameter(required = true, description = "Content item id") @QueryParam("contentId")
          Integer contentId,
      @Parameter(required = true, description = "Assembly template id") @QueryParam("templateId")
          Integer templateId,
      @Parameter(description = "Revision; omitted uses the item current revision")
          @QueryParam("revision")
          Integer revision) {
    try {
      if (contentId == null || contentId <= 0 || templateId == null || templateId <= 0) {
        throw new WebApplicationException("contentId and templateId are required", 400);
      }
      PreviewLocation loc = requireAdaptor().previewLocation(contentId, templateId, revision);
      if (loc == null) {
        throw new WebApplicationException("Item not found", 404);
      }
      return loc;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IAssemblyAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Assembly adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
