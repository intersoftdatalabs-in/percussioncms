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

package com.percussion.rest.sharedfields;

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
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only catalog of content-editor shared field groups for the Developer module (CD-15).
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources.
 */
@PSSiteManageBean(value = "restSharedFieldsResource")
@Path("/sharedfields")
@XmlRootElement
@Tag(name = "SharedFields", description = "Shared field group design catalog (read-only)")
public class SharedFieldsResource {

  private final ISharedFieldsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public SharedFieldsResource() {
    this.adaptor = null;
  }

  @Autowired
  public SharedFieldsResource(ISharedFieldsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List shared field groups",
      description =
          "Lists shared field groups from the content-editor shared definition. Create/edit/delete"
              + " are later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = SharedFieldGroupSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<SharedFieldGroupSummary> listGroups() {
    try {
      return requireAdaptor().listGroups(uriInfo.getBaseUri());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get shared field group detail",
      description =
          "Loads one shared field group by name (read-only). Includes field catalog; write and"
              + " system-def editor remain unsupported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SharedFieldGroupDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SharedFieldGroupDetail getGroup(@PathParam("name") String name) {
    try {
      SharedFieldGroupDetail detail = requireAdaptor().getGroup(uriInfo.getBaseUri(), name);
      if (detail == null) {
        // Generic body: do not echo raw name (path probing).
        throw new WebApplicationException("Shared field group not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private ISharedFieldsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Shared fields adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
