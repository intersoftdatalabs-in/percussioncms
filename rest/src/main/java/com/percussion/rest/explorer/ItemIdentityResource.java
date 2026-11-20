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

package com.percussion.rest.explorer;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST batch visibility identity for the React Content Explorer (#6783 / parent #4530).
 *
 * <pre>
 *   POST /Rhythmyx/rest/explorer/identity/batch
 * </pre>
 *
 * <p>Resolves the checkout-status token plus numeric content / object / workflow / community ids
 * the shell evaluator ({@code menuVisibility.ts}) needs for Workbench hide-on-match rules — facts
 * that are not on {@code PSPathItem} rows (#6618). Unknown or unauthorized ids are omitted from the
 * response; the client stays permissive for anything missing. HTTP <strong>400</strong> is a bad
 * batch (missing body, no usable ids, or over the id cap); HTTP 500 is a wiring failure.
 */
@PSSiteManageBean(value = "restExplorerItemIdentityResource")
@Path("/explorer/identity")
@Tag(name = "Explorer identity", description = "Batch visibility identity for Explorer rows")
public class ItemIdentityResource {

  private static final Logger log = LogManager.getLogger(ItemIdentityResource.class);

  /** Maximum ids per batch; keeps one selection resolve from becoming a list scan. */
  static final int MAX_IDS = 50;

  private final IItemIdentityAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public ItemIdentityResource() {
    this.adaptor = null;
  }

  @Autowired
  public ItemIdentityResource(IItemIdentityAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @POST
  @Path("/batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Resolve visibility identity for Explorer rows",
      description =
          "Returns the checkout-status token and numeric ids the shell"
              + " visibility evaluator needs. Unknown ids are omitted; missing"
              + " facts stay permissive client-side.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Identity per resolvable id, in request order",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ItemIdentity.class)))),
        @ApiResponse(responseCode = "400", description = "Missing body, no usable ids, or over cap"),
        @ApiResponse(responseCode = "500", description = "Adaptor wiring failure")
      })
  public List<ItemIdentity> lookupBatch(ItemIdentityBatchRequest request) {
    IItemIdentityAdaptor impl = requireAdaptor();
    List<String> ids = normalizeIds(request);
    try {
      List<ItemIdentity> found = impl.lookupBatch(uriInfo.getBaseUri(), ids);
      return found != null ? found : new ArrayList<>();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("identity batch lookup failed: {}", e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  private IItemIdentityAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException("Item identity adaptor is not wired", 500);
    }
    return adaptor;
  }

  static List<String> normalizeIds(ItemIdentityBatchRequest request) {
    if (request == null || request.getIds() == null) {
      throw new WebApplicationException("ids are required", 400);
    }
    List<String> ids = new ArrayList<>();
    for (String raw : request.getIds()) {
      String id = raw == null ? "" : raw.trim();
      if (!id.isEmpty() && !ids.contains(id)) {
        ids.add(id);
      }
    }
    if (ids.isEmpty()) {
      throw new WebApplicationException("ids are required", 400);
    }
    if (ids.size() > MAX_IDS) {
      throw new WebApplicationException("too many ids (max " + MAX_IDS + ")", 400);
    }
    return ids;
  }
}
