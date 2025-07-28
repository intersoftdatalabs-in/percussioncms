/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.rest.users;

import com.percussion.error.PSExceptionUtils;
import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST resource for User operations.
 * Sunny Sal: "User resource, authentication ka force!"
 */
@PSSiteManageBean(value = "restUsersResource")
@Path("/users")
@XmlRootElement
@Tag(name = "Users", description = "User operations")
public class UsersResource {

    private static final Logger log = LogManager.getLogger(UsersResource.class);

    @Autowired
    private IUserAdaptor userAdaptor;

    @Context
    private UriInfo uriInfo;

    /**
     * Get a User by userName.
     */
    @GET
    @Path("/{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get a User",
        description = "Will get the User specified by the userName.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error message")
        }
    )
    public User getUserByName(@PathParam("userName") String userName) {
        try {
            userName = java.net.URLDecoder.decode(userName, "UTF-8");
            return userAdaptor.getUser(uriInfo.getBaseUri(), userName);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), e, 500);
        }
    }

    /**
     * Delete a user by userName.
     */
    @DELETE
    @Path("/{userName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete a user",
        description = "Will delete the specified userName from the system. If the user is a Directory based user, the link to the directory is removed but the user will not be removed from the remote Directory."
                + "<br/> Send a DELETE request using the userName"
                + "<br/> Example URL: http://localhost:9992/Rhythmyx/rest/users/userNameToDelete .",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Status.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error message")
        }
    )
    public Status deleteUser(
            @Parameter(description = "The userName of the user to delete.", name = "userName") @PathParam("userName") String userName) {
        int retCode = 404;
        String message = "User not found";
        try {
            userName = java.net.URLDecoder.decode(userName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            retCode = 500;
            message = e.getMessage();
        }
        try {
            userAdaptor.deleteUser(uriInfo.getBaseUri(), userName);
            retCode = 200;
            message = "OK";
        } catch (Exception e) {
            retCode = 500;
            message = e.getMessage();
        }
        return new Status(retCode, message);
    }

    /**
     * Create or update a User.
     */
    @PUT
    @Path("/{userName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create or update a User",
        description = "Creates or Updates the specified User. Returns the resulting User.",
        responses = {
            @ApiResponse(responseCode = "500", description = "Error"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = User.class)))
        }
    )
    public User updateUser(
            @Parameter(description = "The body containing a JSON payload", name = "body") User user,
            @Parameter(description = "The userName to be updated or created", name = "userName") @PathParam("userName") String userName) {
        try {
            userName = java.net.URLDecoder.decode(userName, "UTF-8");
            return userAdaptor.updateOrCreateUser(uriInfo.getBaseUri(), user);
        } catch (BackendException | UnsupportedEncodingException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Find a list of users by name.
     */
    @GET
    @Path("/list/{pattern}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Find a list of users by name",
        description = "Returns a list of user names. The % Wildcard character may be used in searches. The wildcard should be URL encoded as %25 on the URL.",
        responses = {
            @ApiResponse(responseCode = "500", description = "Error"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
        }
    )
    public List<String> findUsers(@PathParam("pattern") String pattern) {
        try {
            pattern = java.net.URLDecoder.decode(pattern, "UTF-8");
            return userAdaptor.findUsers(uriInfo.getBaseUri(), pattern);
        } catch (BackendException | UnsupportedEncodingException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Check the status of the Active Directory or LDAP connection for external directories.
     */
    @GET
    @Path("/directory/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Check the status of the Active Directory or LDAP connection for external directories.",
        description = "Directory configuration is optional, use this method to verify that the Directory connection is configured and working.",
        responses = {
            @ApiResponse(responseCode = "500", description = "Error"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Status.class)))
        }
    )
    public Status checkDirectoryStatus() {
        var status = userAdaptor.checkDirectoryStatus();
        if (status == null) {
            status = new Status(404, "Not Found");
        }
        return status;
    }

    /**
     * Search for users in External Directories.
     */
    @GET
    @Path("/directory/list/{pattern}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Search for users in External Directories",
        description = "Search results are limited to 200 users. Prior to calling this method, check the directory status. Search pattern may include a Wildcard with the % character. This should be URL encoded on the URL with a %25 character.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "500", description = "Error")
        }
    )
    public List<String> searchDirectoryService(@PathParam("pattern") String pattern) {
        try {
            pattern = java.net.URLDecoder.decode(pattern, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 always supported
        }
        var ret = userAdaptor.searchDirectory(pattern);
        if (ret == null) {
            ret = new ArrayList<>();
        }
        return ret;
    }

    public IUserAdaptor getUserAdaptor() {
        return userAdaptor;
    }

    public void setUserAdaptor(IUserAdaptor userAdaptor) {
        this.userAdaptor = userAdaptor;
    }
}
