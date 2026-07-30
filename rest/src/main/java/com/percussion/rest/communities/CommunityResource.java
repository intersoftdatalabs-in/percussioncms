// REFACTORED: CP-JAVA11
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

package com.percussion.rest.communities;

import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.rest.Status;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST resource for Community operations. */
@PSSiteManageBean(value = "restCommunityResource")
@Path("/communities")
@XmlRootElement
@Tag(name = "Communities", description = "Community operations")
@Lazy
public class CommunityResource implements ICommunityResource {

  private static final Logger log = LogManager.getLogger(CommunityResource.class);

  @Autowired private ICommunityAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public CommunityResource() {
    // NOOP
  }

  @Override
  @POST
  @Path("/bulk")
  @Operation(summary = "Creates a set of communities from an array list of community names.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  public CommunityList createCommunities(
      @Parameter(example = "{\"List\":\n" + "[\"Comm1\",\"Comm2\",\"Comm3\"]\n" + "}")
          List<String> names) {
    try {
      return adaptor.createCommunities(names);
    } catch (Exception e) {
      log.error("An error occurred calling createCommunities", e);
      throw new WebApplicationException(e);
    }
  }

  @Override
  @GET
  @Path("/find")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get a community by name or pattern. * is the wildcard.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    schema = @Schema(implementation = CommunityList.class),
                    examples =
                        @ExampleObject(
                            value =
                                "{\n"
                                    + "  \"CommunityList\": [\n"
                                    + "    {\n"
                                    + "      \"description\": \"Default Community\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 10,\n"
                                    + "        \"stringValue\": \"0-13-10\",\n"
                                    + "        \"type\": 13,\n"
                                    + "        \"untypedString\": \"0-10\",\n"
                                    + "        \"uuid\": 10\n"
                                    + "      },\n"
                                    + "      \"id\": 10,\n"
                                    + "      \"label\": \"Default\",\n"
                                    + "      \"name\": \"Default\"\n"
                                    + "    }\n"
                                    + "  ]\n"
                                    + "}")))
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  public CommunityList findCommunities(
      @Parameter(name = "name", required = true) @QueryParam("name") String name) {
    try {
      return adaptor.findCommunities(name);
    } catch (Exception e) {
      log.error("An error occurred calling findCommunities.", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  @POST
  @Path("/bulk/load")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary =
          "Loads one or more communities by guid. Takes a GuidList of communities to retrieve."
              + " Setting the lock header to true will lock the communities on the server. Setting"
              + " overridelock header to true will return the list even if the communities are"
              + " locked in another session.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Community.class)))),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  public CommunityList loadCommunities(
      @Parameter(name = "ids", required = true) GuidList ids,
      @Parameter(name = "lock") @HeaderParam("lock") boolean lock,
      @HeaderParam("overridelock") @Parameter(name = "overridelock") boolean overridelock) {
    try {
      return adaptor.loadCommunities(ids, lock, overridelock);
    } catch (Exception e) {
      log.error("An error occurred calling loadCommunities", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  @PUT
  @Path("/bulk")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary =
          "Saves the communities in the submitted list. If the release header is set, any locks"
              + " will be released. To avoid extended locks, please set the release header when"
              + " calling this method unless you really want to keep them locked.")
  public void saveCommunities(
      @Parameter(description = "communities", required = true) CommunityList communities,
      @Parameter(name = "release", allowEmptyValue = false, required = true)
          @HeaderParam(value = "release")
          boolean release) {
    try {
      adaptor.saveCommunities(communities, release);
    } catch (Exception e) {
      log.error("An error occurred calling saveCommunities", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  @DELETE
  @Path("/bulk")
  @Consumes({MediaType.APPLICATION_JSON})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  @Operation(
      summary =
          "Accepts a GuidList containing the ids of the Communities to delete. If the"
              + " ignoredependencies header is set on the request, then the system won't fail the"
              + " delete if a dependency is relying on this community.")
  public void deleteCommunities(
      @Parameter(name = "ids") GuidList ids,
      @Parameter(name = "ignoredependencies", required = true) @HeaderParam("ignoredependencies")
          boolean ignoredependencies) {
    try {
      adaptor.deleteCommunities(ids, ignoredependencies);
    } catch (Exception e) {
      log.error("An error occurred calling deleteCommunities", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Programmatic / interface entry — object-type filter already resolved.
   *
   * <p>HTTP entry point is the three-argument overload that accepts {@code X-Object-Type} and
   * legacy {@code type} headers.
   */
  @Override
  public CommunityVisibilityList getVisibilityByCommunity(GuidList ids, ObjectTypeEnum type) {
    try {
      return adaptor.getVisibilityByCommunity(ids, type);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("An error occurred calling getVisibilityByCommunity", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Path("/visibility")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid object type header"),
        @ApiResponse(responseCode = "500", description = "ERROR")
      })
  @Operation(
      summary =
          "Returns the Community visibility list for the specified communities. Optional filter via"
              + " X-Object-Type (preferred) or legacy type header; omit for all object types.")
  public CommunityVisibilityList getVisibilityByCommunity(
      @Parameter(
              description = "List of GUID",
              example =
                  "{\"GuidList\": [\n"
                      + "   {\n"
                      + "        \"stringValue\": \"0-13-1003\",\n"
                      + "        \"untypedString\": \"0-1003\",\n"
                      + "        \"hostId\": 0,\n"
                      + "        \"type\": 13,\n"
                      + "        \"uuid\": 1003,\n"
                      + "        \"longValue\": 1003\n"
                      + "      },\n"
                      + "      {\n"
                      + "        \"stringValue\": \"0-13-10\",\n"
                      + "        \"untypedString\": \"0-10\",\n"
                      + "        \"hostId\": 0,\n"
                      + "        \"type\": 13,\n"
                      + "        \"uuid\": 10,\n"
                      + "        \"longValue\": 10\n"
                      + "      }\n"
                      + "]}")
          GuidList ids,
      @Parameter(
              name = "X-Object-Type",
              description =
                  "Preferred ObjectTypeEnum name filter (e.g. NODEDEF, TEMPLATE). Takes precedence"
                      + " over legacy type header.")
          @HeaderParam("X-Object-Type")
          ObjectTypeEnum objectType,
      @Parameter(
              name = "type",
              description = "Legacy ObjectTypeEnum filter; used when X-Object-Type is absent.")
          @HeaderParam("type")
          ObjectTypeEnum legacyType) {
    return getVisibilityByCommunity(ids, objectType != null ? objectType : legacyType);
  }

  @POST
  @Path("/switch/{name}")
  @Operation(
      summary =
          "Switch the user's session to the specified community. May error out if user does not"
              + " have access to a Role in specified community",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Status switchCommunity(
      @Parameter(required = true, description = "Name of the community to switch to")
          @PathParam("name")
          String name) {
    var ret = new Status(200, "OK");
    try {
      adaptor.switchCommunity(name);
    } catch (Exception e) {
      log.error("An error occurred calling switchCommunity", e);
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
    return ret;
  }

  @GET
  @Path("/roles")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List available roles",
      description =
          "Lists security roles that can be associated with a community (for membership editing).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = CommunityRole.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public CommunityRoleList listAvailableRoles() {
    try {
      return adaptor.listAvailableRoles();
    } catch (Exception e) {
      log.error("An error occurred calling listAvailableRoles", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get community detail",
      description =
          "Community detail including associated roles (role id/name when resolvable)."
              + " Lookup by numeric id, GUID string, or exact name."
              + " Object ACL dialogs are not supported yet.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Community.class))),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Community getCommunity(
      @Parameter(description = "Community id, GUID string, or exact name", required = true)
          @PathParam("idOrName")
          String idOrName) {
    Community community;
    try {
      community = adaptor.getCommunity(idOrName);
    } catch (Exception e) {
      // Do not leak internal exception text to API clients
      log.error("An error occurred calling getCommunity for {}", idOrName, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (community == null) {
      throw new WebApplicationException("Community not found: " + idOrName, 404);
    }
    return community;
  }

  @PUT
  @Path("/{idOrName}/roles")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update community role membership",
      description =
          "Replaces the set of roles associated with the community. Body is a"
              + " CommunityRoleList (or array of CommunityRole). Role guid or roleId required"
              + " per entry. Object ACL editing is not supported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated community with roles",
            content = @Content(schema = @Schema(implementation = Community.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Community not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Community updateCommunityRoles(
      @Parameter(description = "Community id, GUID string, or exact name", required = true)
          @PathParam("idOrName")
          String idOrName,
      CommunityRoleList roles) {
    Community community;
    try {
      community = adaptor.updateCommunityRoles(idOrName, roles);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      // Do not leak internal exception text to API clients
      log.error("An error occurred calling updateCommunityRoles for {}", idOrName, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (community == null) {
      throw new WebApplicationException("Community not found: " + idOrName, 404);
    }
    return community;
  }
}
