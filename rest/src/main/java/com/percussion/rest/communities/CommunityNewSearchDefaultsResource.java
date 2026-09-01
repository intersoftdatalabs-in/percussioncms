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

package com.percussion.rest.communities;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Admin GET/PUT for community Content Explorer new-search defaults (Workbench UI-09).
 *
 * <p>Empty default set is 200 (not 404). Unknown community is 404. Unknown or duplicate search is
 * 400. Non-Admin is 403. Persistence is the Workbench {@code cxNewSearch} property via {@code
 * IPSUiDesignWs}; this resource does not create searches.
 */
@PSSiteManageBean(value = "restCommunityNewSearchDefaultsResource")
@Path("/communities/{idOrName}/new-search-defaults")
@XmlRootElement
@Tag(
    name = "Communities",
    description = "Community operations including CX new-search defaults (UI-09)")
public class CommunityNewSearchDefaultsResource {

  private final ICommunityNewSearchDefaultsAdaptor adaptor;

  @Autowired
  public CommunityNewSearchDefaultsResource(ICommunityNewSearchDefaultsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get community new-search defaults",
      description =
          "Admin. Returns the Content Explorer searches marked as new-search defaults for the"
              + " community (Workbench UI-09 / cxNewSearch). Lookup by numeric id, GUID string, or"
              + " exact name. An empty set is 200, not 404.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK (empty searches array when none are assigned)",
            content =
                @Content(schema = @Schema(implementation = CommunityNewSearchDefaults.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Community not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public CommunityNewSearchDefaults getDefaults(
      @Parameter(description = "Community id, GUID string, or exact name", required = true)
          @PathParam("idOrName")
          String idOrName) {
    try {
      CommunityNewSearchDefaults out = requireAdaptor().getDefaults(idOrName);
      if (out == null) {
        throw new WebApplicationException("Community not found: " + idOrName, 404);
      }
      return out;
    } catch (RuntimeException e) {
      throw mapFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace community new-search defaults",
      description =
          "Admin. Replaces the set of Content Explorer searches used as new-search defaults for"
              + " the community. Body searches may be identified by name, numeric id, or GUID."
              + " Empty searches clears explicit defaults for that community. A second identical"
              + " PUT is idempotent 200. Does not create searches.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced (idempotent when unchanged)",
            content =
                @Content(schema = @Schema(implementation = CommunityNewSearchDefaults.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, unknown search, or duplicate search"),
        @ApiResponse(
            responseCode = "403",
            description = "Admin role required, or request has no session/user"),
        @ApiResponse(responseCode = "404", description = "Community not found"),
        @ApiResponse(responseCode = "409", description = "Design lock held by another user"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public CommunityNewSearchDefaults replaceDefaults(
      @Parameter(description = "Community id, GUID string, or exact name", required = true)
          @PathParam("idOrName")
          String idOrName,
      CommunityNewSearchDefaults body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      CommunityNewSearchDefaults out = requireAdaptor().replaceDefaults(idOrName, body);
      if (out == null) {
        throw new WebApplicationException("Community not found: " + idOrName, 404);
      }
      return out;
    } catch (RuntimeException e) {
      throw mapFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  static WebApplicationException mapFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    if (e instanceof CommunityNewSearchDefaultsDesignLockException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      return new WebApplicationException(msg, 409);
    }
    return new WebApplicationException(e, 500);
  }

  private ICommunityNewSearchDefaultsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Community new-search defaults adaptor not configured",
          Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
