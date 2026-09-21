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

package com.percussion.rest.editor;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST checkout / check-in for the React Content Editor (#4644 / parent #4532).
 *
 * <pre>
 *   POST /Rhythmyx/rest/editor/items/{id}/checkout
 *   POST /Rhythmyx/rest/editor/items/{id}/checkin
 * </pre>
 *
 * <p>HTTP <strong>403</strong> (not allowed) and <strong>409</strong> (held by another user / not
 * held by the session) are errors — not success.
 */
@PSSiteManageBean(value = "restEditorItemLockResource")
@Path("/editor/items")
@XmlRootElement
@Tag(name = "Editor item lock", description = "Check out and check in the open editor item")
public class EditorItemLockResource {

  private static final Logger log = LogManager.getLogger(EditorItemLockResource.class);

  private final IEditorItemLockAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public EditorItemLockResource() {
    this.adaptor = null;
  }

  @Autowired
  public EditorItemLockResource(IEditorItemLockAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @POST
  @Path("/{id}/checkout")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Check out an item",
      description = "Takes the CMS lock for the current user. 403/409 are failures.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Checked out to the session user",
            content = @Content(schema = @Schema(implementation = EditorItemLockInfo.class))),
        @ApiResponse(responseCode = "400", description = "Missing item id"),
        @ApiResponse(responseCode = "403", description = "Not allowed to check out"),
        @ApiResponse(responseCode = "409", description = "Checked out to another user")
      })
  public EditorItemLockInfo checkout(@PathParam("id") String id) {
    try {
      return requireAdaptor().checkout(uriInfo.getBaseUri(), id);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("checkout failed: {}", e.getMessage(), e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{id}/checkin")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Check in an item",
      description = "Releases the CMS lock. 403/409 are failures.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Checked in",
            content = @Content(schema = @Schema(implementation = EditorItemLockInfo.class))),
        @ApiResponse(responseCode = "400", description = "Missing item id"),
        @ApiResponse(responseCode = "403", description = "Not allowed to check in"),
        @ApiResponse(responseCode = "409", description = "Not checked out to the session user")
      })
  public EditorItemLockInfo checkin(@PathParam("id") String id) {
    try {
      return requireAdaptor().checkin(uriInfo.getBaseUri(), id);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("checkin failed: {}", e.getMessage(), e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private IEditorItemLockAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException("Editor lock adaptor is not configured", 503);
    }
    return adaptor;
  }
}
