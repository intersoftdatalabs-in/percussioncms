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

package com.percussion.rest.actions;

import com.percussion.rest.Guid;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** REST resource exposing Action Menu operations. */
@PSSiteManageBean(value = "restActionMenuResource")
@Path("/actions")
@Tag(name = "Action Menu", description = "Action Menu operations")
public class ActionMenuResource {

  /** Logger used by this resource. */
  private static final Logger log = LogManager.getLogger(ActionMenuResource.class);

  /** Adaptor that implements the action menu operations. */
  @Autowired private IActionMenuAdaptor adaptor;

  /** Injected URI info, may be {@code null}. */
  @Context private UriInfo uriInfo;

  /** No-op constructor. */
  public ActionMenuResource() {}

  /**
   * Developer P0 catalog: all design-time action menus (UI-02 read).
   *
   * <p>Prefer this over {@code /find} when no filter criteria are needed; failures surface as 500
   * rather than empty lists.
   */
  @GET
  @Path("/catalog")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List action menus (catalog)",
      description =
          "Lists CX action menus for the Developer module. Create/edit/delete and entry"
              + " composition remain later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ActionMenu.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ActionMenu> listActionMenus() {
    try {
      List<ActionMenu> list = requireAdaptor().findMenus(null, null, null, null, null);
      return list != null ? list : Collections.emptyList();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error listing action menus", e);
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/catalog/{idOrName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get action menu detail",
      description =
          "Loads one action menu by name or numeric id. Write and cascading entry edit remain"
              + " unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ActionMenu.class))),
        @ApiResponse(responseCode = "404", description = "Menu not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ActionMenu getActionMenu(@PathParam("idOrName") String idOrName) {
    try {
      ActionMenu menu = requireAdaptor().findMenuByKey(idOrName);
      if (menu == null) {
        // Generic body: do not echo raw idOrName (path probing).
        throw new WebApplicationException("Action menu not found", 404);
      }
      return menu;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error loading action menu", e);
      throw new WebApplicationException(e, 500);
    }
  }

  private IActionMenuAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Action menu adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }

  @GET
  @Path("/find")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Finds action menus",
      description = "Returns a list of Action Menus that matches the criteria.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ActionMenu.class)))),
        @ApiResponse(responseCode = "404", description = "No Action Menu's found"),
        @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
      })
  public List<ActionMenu> findActions(
      @Parameter(name = "name", required = false) @QueryParam("name") String name,
      @Parameter(name = "label", required = false) @QueryParam("label") String label,
      @Parameter(name = "dynamic", required = false) @QueryParam("dynamic") Boolean dynamic,
      @Parameter(name = "item", required = false) @QueryParam("item") Boolean item,
      @Parameter(name = "cascading", required = false) @QueryParam("cascading") Boolean cascading) {
    var safeName = StringUtils.isBlank(name) ? null : name;
    var safeLabel = StringUtils.isBlank(label) ? null : label;
    try {
      return Optional.ofNullable(adaptor.findMenus(safeName, safeLabel, item, dynamic, cascading))
          .orElse(Collections.emptyList());
    } catch (Exception e) {
      log.error("Error finding action menus: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  /**
   * Loads action menus by GUIDs. Not implemented.
   *
   * @param guids the action menu GUIDs
   * @return an empty array (not implemented)
   */
  public ActionMenu[] loadActions(List<Guid> guids) {
    // Not implemented yet
    return new ActionMenu[0];
  }

  /**
   * Accepts an object with an array of content ids and assignment types and returns the allowed
   * menus. Not implemented.
   *
   * @param request the request payload
   * @return an empty list (not implemented)
   */
  @POST
  @Operation(
      description =
          "Accepts an object with an array of contentid's and assignment types and returns the"
              + " allowed menus")
  public ActionMenuList getAllowedTransitions(AllowedWorkflowTransitionsRequest request) {
    // Not implemented yet
    return new ActionMenuList();
  }

  @POST
  @Path("/find/types")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Finds action menus by content type",
      description =
          "Returns a list of Action Menus that matches the criteria. ActionId should be ignored for"
              + " these menus.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ActionMenu.class)))),
        @ApiResponse(responseCode = "404", description = "No Action Menu's found"),
        @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
      })
  public ActionMenuList getAllowedContentTypeMenus(AllowedContentTypeMenusRequest request) {
    int[] raw = request.getContentIds() != null ? request.getContentIds() : new int[0];
    var contentIds = Arrays.stream(raw).boxed().toArray(Integer[]::new);
    return new ActionMenuList(adaptor.findAllowedContentTypes(contentIds));
  }

  @GET
  @Path("/find/templates/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Returns a list of allowed Templates for the given content id",
      description = "ActionId should be ignored for these menus.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Success",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ActionMenu.class)))),
        @ApiResponse(responseCode = "404", description = "No Action Menu's found"),
        @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
      })
  public ActionMenuList getAllowedTemplateMenus(
      @Parameter(required = true, description = "The content id to retrieve template URLS for.")
          @PathParam(value = "id")
          int contentId,
      @Parameter(description = "Set to true to include AA menus.") @QueryParam(value = "isAA")
          boolean isAA) {
    return new ActionMenuList(adaptor.findAllowedTemplates(contentId, isAA));
  }
}
