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

package com.percussion.rest.roles;

import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.security.error.PSExceptionUtils;
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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** REST resource for Role operations. Sunny Sal: "Role resource, permissions ka force!" */
@PSSiteManageBean(value = "restRolesResource")
@Path("/roles")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Roles", description = "Role operations")
public class RolesResource {

  private static final Logger log = LogManager.getLogger(RolesResource.class);

  @Autowired private IRoleAdaptor roleAdaptor;

  @Context private UriInfo uriInfo;

  public RolesResource() {
    // Default constructor
  }

  /** Get a Role by name. */
  @GET
  @Path("/{roleName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get a Role",
      description = "Will get the Role specified by the roleName.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public Role getRoleByName(@PathParam("roleName") String roleName) {
    try {
      roleName = java.net.URLDecoder.decode(roleName, "UTF-8");
      return requireAdaptor().getRole(uriInfo.getBaseUri(), roleName);
    } catch (BackendException | UnsupportedEncodingException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Delete a Role by name. */
  @DELETE
  @Path("/{roleName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a Role",
      description =
          "Deletes the CMS role. System roles (System, Default) are 400. Non-Admin is 403."
              + " A missing role is 404. A role that would strand users or that a workflow still"
              + " assigns is 409 and is not deleted. If the role is a directory group, only the"
              + " CMS link is removed; the remote directory group is not deleted.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class))),
        @ApiResponse(responseCode = "400", description = "Blank name or system role"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Role is in use or deleting it would strand users"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public Status deleteRole(
      @Parameter(description = "The roleName of the Role to delete.", name = "roleName")
          @PathParam("roleName")
          String roleName) {
    if (isBlank(roleName)) {
      throw new WebApplicationException("Role name is required", 400);
    }
    try {
      roleName = java.net.URLDecoder.decode(roleName, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new WebApplicationException(e.getMessage(), 500);
    }
    if (isBlank(roleName)) {
      throw new WebApplicationException("Role name is required", 400);
    }
    var base = uriInfo != null ? uriInfo.getBaseUri() : null;
    try {
      requireAdaptor().deleteRole(base, roleName);
      return new Status(200, "OK");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      var message = e.getMessage() != null ? e.getMessage() : "Could not delete role";
      throw new WebApplicationException(message, 500);
    }
  }

  /**
   * Create a role, update an existing role's description, set its home page, or add one user.
   *
   * <p>{@code create=true} always uses {@link IRoleAdaptor#createRole} (role service create). A
   * name that is not already defined also uses create. {@code update=true} always uses {@link
   * IRoleAdaptor#updateRole} and does not create a missing role. {@code homePage=true} always uses
   * {@link IRoleAdaptor#updateRoleHomePage} and does not create a missing role. {@code
   * addUser=true} always uses {@link IRoleAdaptor#addRoleUser} and does not create a missing role
   * or user. An existing name is updated only when {@code create} is not true — a create payload
   * must not clear members. Description update does not change membership, home page, or name.
   * Home-page update does not change description, membership, or name. A blank home page clears
   * the stored value. Add-user appends one existing user and does not change description, home
   * page, or name.
   */
  @PUT
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create a role, update its description, set its home page, or add one user",
      description =
          "Creates a role via adaptor createRole / role service create when create=true, or when"
              + " the name is not already defined and update, homePage, and addUser are not true."
              + " update=true changes the description of an existing role only (members, home"
              + " page, and name stay as stored) and is 404 when the role is missing. A client"
              + " user list on update=true is ignored."
              + " homePage=true changes the home page of an existing role only (description,"
              + " members, and name stay as stored). A blank home page clears it. An unknown home"
              + " page is 400. addUser=true adds one existing user to an existing role. A blank"
              + " user name, more than one user, or an unknown user is 400 and does not change"
              + " membership. A user who is already a member is 409 and does not change membership."
              + " Blank role name is 400. Create, description update, home-page update, and"
              + " add-user require the Admin role (403). A duplicate create is 400 and does not"
              + " update. A description longer than 255 characters is 400. Do not combine create,"
              + " update, homePage, and addUser. Returns the resulting Role.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Blank name, invalid role, description longer than 255 characters, unknown home"
                    + " page, blank or unknown user, more than one user, or more than one of"
                    + " create, update, homePage, and addUser"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found (update=true, homePage=true, or addUser=true)"),
        @ApiResponse(
            responseCode = "409",
            description = "User is already a member of the role (addUser=true)"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Role updateRole(
      @Parameter(
              description =
                  "When true, always create. Duplicate names are 400 and are not updated.",
              name = "create")
          @QueryParam("create")
          Boolean create,
      @Parameter(
              description =
                  "When true, update the description of an existing role. Missing names are 404"
                      + " and are not created. A client user list is ignored. Cannot be combined"
                      + " with create=true, homePage=true, or addUser=true.",
              name = "update")
          @QueryParam("update")
          Boolean update,
      @Parameter(
              description =
                  "When true, update the home page of an existing role. A blank home page clears"
                      + " it. Missing names are 404 and are not created. Cannot be combined with"
                      + " create=true, update=true, or addUser=true.",
              name = "homePage")
          @QueryParam("homePage")
          Boolean homePage,
      @Parameter(
              description =
                  "When true, add exactly one existing user from the role users list. Missing"
                      + " roles are 404 and are not created. An unknown user is 400. A user who"
                      + " is already a member is 409. Cannot be combined with create=true,"
                      + " update=true, or homePage=true.",
              name = "addUser")
          @QueryParam("addUser")
          Boolean addUser,
      @Parameter(description = "The body containing a JSON payload", name = "body") Role role) {
    if (role == null || isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (moreThanOneSaveFlag(create, update, homePage, addUser)) {
      throw new WebApplicationException(
          "Specify create, update, homePage, or addUser, not more than one", 400);
    }
    role.setName(role.getName().trim());
    var base = uriInfo != null ? uriInfo.getBaseUri() : null;
    try {
      if (Boolean.TRUE.equals(addUser)) {
        return requireAdaptor().addRoleUser(base, role);
      }
      if (Boolean.TRUE.equals(homePage)) {
        return requireAdaptor().updateRoleHomePage(base, role);
      }
      // Explicit description update must not fall through to create when the role is gone.
      if (Boolean.TRUE.equals(update)) {
        return requireAdaptor().updateRole(base, role);
      }
      // New names and explicit creates must not fall through to updateRole (empty users would
      // clear membership).
      if (Boolean.TRUE.equals(create) || !requireAdaptor().roleExists(base, role.getName())) {
        return requireAdaptor().createRole(base, role);
      }
      return requireAdaptor().updateRole(base, role);
    } catch (WebApplicationException e) {
      throw e;
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      var message = e.getMessage() != null ? e.getMessage() : "Could not save role";
      throw new WebApplicationException(message, 500);
    }
  }

  /**
   * Same as {@link #updateRole(Boolean, Boolean, Boolean, Boolean, Role)} with {@code addUser}
   * unset. Kept so callers that only create, describe, or set a home page do not have to pass the
   * membership flag.
   */
  public Role updateRole(Boolean create, Boolean update, Boolean homePage, Role role) {
    return updateRole(create, update, homePage, null, role);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /** True when more than one of create, update, homePage, or addUser is explicitly true. */
  private static boolean moreThanOneSaveFlag(
      Boolean create, Boolean update, Boolean homePage, Boolean addUser) {
    int selected = 0;
    if (Boolean.TRUE.equals(create)) {
      selected++;
    }
    if (Boolean.TRUE.equals(update)) {
      selected++;
    }
    if (Boolean.TRUE.equals(homePage)) {
      selected++;
    }
    if (Boolean.TRUE.equals(addUser)) {
      selected++;
    }
    return selected > 1;
  }

  /** Find available roles on the system by % wild card pattern. */
  @GET
  @Path("/list/{pattern}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Find available roles on the system by % wild card pattern",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Role.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RoleList findRoles() {
    try {
      var ret = requireAdaptor().findRoles(uriInfo.getBaseUri(), "%");
      return new RoleList(ret);
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Admin SE-03 roles browse catalog with community / workflow / unassigned grouping metadata.
   *
   * <p>Literal path {@code /catalog} must not be captured by {@code /{roleName}}.
   */
  @GET
  @Path("/catalog")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Browse roles by community / workflow / unassigned",
      description =
          "Admin. Returns system roles with Workbench Security Design grouping metadata"
              + " (community, workflow, unassigned). Optional group query filter limits the"
              + " result. Non-Admin is 403 via adaptor requireAdmin (same Admin gate pattern as"
              + " other Developer Admin REST; this module does not use JAX-RS @RolesAllowed)."
              + " Missing adaptor is 503 on all role endpoints.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RoleBrowseCatalog.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid group filter (expected community, workflow, or unassigned)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RoleBrowseCatalog browseRoles(
      @Parameter(
              description =
                  "Optional filter: community, workflow, or unassigned. Omit for the full catalog.")
          @QueryParam("group")
          String group) {
    try {
      return requireAdaptor().browseRoles(uriInfo != null ? uriInfo.getBaseUri() : null, group);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (RuntimeException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e, 500);
    }
  }

  private IRoleAdaptor requireAdaptor() {
    if (roleAdaptor == null) {
      throw new WebApplicationException("Role adaptor not configured", 503);
    }
    return roleAdaptor;
  }

  public IRoleAdaptor getRoleAdaptor() {
    return roleAdaptor;
  }

  public void setRoleAdaptor(IRoleAdaptor roleAdaptor) {
    this.roleAdaptor = roleAdaptor;
  }
}
