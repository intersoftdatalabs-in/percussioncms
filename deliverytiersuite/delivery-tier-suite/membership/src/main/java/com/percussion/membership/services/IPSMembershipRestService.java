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
// REFACTORED: CP-JAVA11
package com.percussion.membership.services;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import com.percussion.delivery.services.IPSRestService;
import com.percussion.membership.data.*;

/**
 * REST service for membership accounts.
 *
 * @author natechadwick
 */
@Path("/membership")
public interface IPSMembershipRestService extends IPSRestService {

    /**
     * Creates a membership account.
     *
     * @param membership Account info to create, not null.
     * @return Result object, never null.
     */
    @POST
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON)
    PSMembershipResult createUser(PSMembershipAccount membership, @Context HttpHeaders header);

    /**
     * Changes the state of a user account.
     *
     * @param account {@link PSAccountSummary} with data to process.
     */
    @PUT
    @Path("/admin/account")
    void changeStateAccount(PSAccountSummary account);

    /**
     * Deletes a user account.
     *
     * @param email Email of account to delete.
     */
    @DELETE
    @Path("/admin/account/{email:.*}")
    void deleteAccount(@PathParam("email") String email);

    @POST
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    PSGetUserResult getUser(PSUserSession psUserSession);

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    PSLoginResult login(PSLoginRequest loginRequest);

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    PSMembershipResult logout(PSUserSession psUserSession);

    @POST
    @Path("/pwd/requestReset")
    @Produces(MediaType.APPLICATION_JSON)
    PSMembershipResult requestPwdReset(PSResetRequest resetRequest, @Context HttpHeaders header);

    /**
     * Validates the reset key.
     *
     * @param resetKey Token key, not null.
     * @return {@link PSGetUserResult} containing the email, never null.
     */
    @POST
    @Path("/pwd/validate/{resetKey:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    PSGetUserResult validatePwdResetKey(@PathParam("resetKey") String resetKey);

    /**
     * Resets the user password.
     *
     * @param resetKey Token key, not null.
     * @param resetRequest Parameters to associate new password.
     * @return {@link PSLoginResult} containing the session, never null.
     */
    @POST
    @Path("/pwd/reset/{resetKey:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    PSLoginResult resetPwd(@PathParam("resetKey") String resetKey, PSMembershipAccount resetRequest);

    /**
     * Confirms an account.
     *
     * @param confirmKey Token key, not null.
     * @return {@link PSLoginResult} containing the session, never null.
     */
    @POST
    @Path("/registration/confirm/{rvkey:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    PSLoginResult confirmAccount(@PathParam("rvkey") String confirmKey);

    @GET
    @Path("/admin/users")
    @Produces(MediaType.APPLICATION_JSON)
    List<PSUserSummary> findUserGroups();

    @PUT
    @Path("/admin/user/group/{siteName}")
    @Produces(MediaType.APPLICATION_JSON)
    void updateUserGroups(PSUserGroup userSummary);
}
