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
          "Will delete the specified roleName from the system. If the Role is a Directory based"
              + " Group, the link to the directory is removed but the Group will not be removed"
              + " from the remote Directory.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class))),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public Status deleteRole(
      @Parameter(description = "The roleName of the Role to delete.", name = "roleName")
          @PathParam("roleName")
          String roleName) {
    int retCode = 404;
    String message = "Role not found";
    try {
      roleName = java.net.URLDecoder.decode(roleName, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      retCode = 500;
      message = e.getMessage();
    }
    try {
      requireAdaptor().deleteRole(uriInfo.getBaseUri(), roleName);
      retCode = 200;
      message = "OK";
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      retCode = 500;
      message = e.getMessage();
    }
    return new Status(retCode, message);
  }

  /**
   * Create a role, or update an existing role's description.
   *
   * <p>{@code create=true} always uses {@link IRoleAdaptor#createRole} (role service create). A
   * name that is not already defined also uses create. {@code update=true} always uses {@link
   * IRoleAdaptor#updateRole} and does not create a missing role. An existing name is updated only
   * when {@code create} is not true — a create payload must not clear members. Description update
   * does not change membership or rename the role.
   */
  @PUT
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create a role or update its description",
      description =
          "Creates a role via adaptor createRole / role service create when create=true, or when"
              + " the name is not already defined and update is not true. update=true changes the"
              + " description of an existing role only (members and name stay as stored) and is"
              + " 404 when the role is missing. Blank name is 400. Create and description update"
              + " require the Admin role (403). A duplicate create is 400 and does not update. A"
              + " description longer than 255 characters is 400. Returns the resulting Role.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Blank name, invalid role, or description longer than 255 characters"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Role not found (update=true)"),
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
                      + " and are not created. Cannot be combined with create=true.",
              name = "update")
          @QueryParam("update")
          Boolean update,
      @Parameter(description = "The body containing a JSON payload", name = "body") Role role) {
    if (role == null || isBlank(role.getName())) {
      throw new WebApplicationException("Role name is required", 400);
    }
    if (Boolean.TRUE.equals(create) && Boolean.TRUE.equals(update)) {
      throw new WebApplicationException("Specify create or update, not both", 400);
    }
    role.setName(role.getName().trim());
    var base = uriInfo != null ? uriInfo.getBaseUri() : null;
    try {
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

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
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
