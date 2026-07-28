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
package com.percussion.membership.services;

import com.percussion.delivery.services.IPSRestService;
import com.percussion.membership.data.*;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * REST service for the membership service. Exposes user account lifecycle and authentication
 * operations under {@code /membership}.
 *
 * @author natechadwick
 */
@Path("/membership")
public interface IPSMembershipRestService extends IPSRestService {

  /**
   * Rest service method to create a membership account.
   *
   * @param membership Object containing the account info to create, may not be <code>null</code>.
   * @param header the current request's HTTP headers, injected by the JAX-RS runtime.
   * @return A result object, never <code>null</code>.
   */
  @POST
  @Path("/user")
  @Produces("application/json")
  public abstract PSMembershipResult createUser(
      PSMembershipAccount membership, @Context HttpHeaders header);

  /**
   * Rest service method to change the state of an user account.
   *
   * @param account a {@link PSAccountSummary} object with the data to process.
   */
  @PUT
  @Path("/admin/account")
  public abstract void changeStateAccount(PSAccountSummary account);

  /**
   * Rest service method to delete an user account.
   *
   * @param email the email relative to the account to delete, never empty or <code>null</code>.
   */
  @DELETE
  @Path("/admin/account/{email:.*}")
  public abstract void deleteAccount(@PathParam("email") String email);

  /**
   * Rest service method to look up the user for the supplied session.
   *
   * @param psUserSession session the user session to look up, may not be {@code null}.
   * @return the look-up result, never {@code null}.
   */
  @POST
  @Path("/session")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSGetUserResult getUser(PSUserSession psUserSession);

  /**
   * Rest service method to log in the supplied credentials.
   *
   * @param loginRequest the login request carrying email and password, may not be {@code null}.
   * @return the login result carrying the new session, never {@code null}.
   */
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSLoginResult login(PSLoginRequest loginRequest);

  /**
   * Rest service method to destroy the session for the supplied user session.
   *
   * @param psUserSession the session to destroy, may not be {@code null}.
   * @return the membership result, never {@code null}.
   */
  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMembershipResult logout(PSUserSession psUserSession);

  /**
   * Rest service method to request a password reset for the supplied email.
   *
   * @param resetRequest the reset request, may not be {@code null}.
   * @param header the current request's HTTP headers, injected by the JAX-RS runtime.
   * @return the membership result, never {@code null}.
   */
  @POST
  @Path("/pwd/requestReset")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMembershipResult requestPwdReset(
      PSResetRequest resetRequest, @Context HttpHeaders header);

  /**
   * Rest service method to validate the reset key.
   *
   * @param resetKey String containing the token key, may not be <code>null</code>.
   * @return An {@link PSGetUserResult} object containing the email, may be empty but never <code>
   *     null</code>.
   */
  @POST
  @Path("/pwd/validate/{resetKey:.*}")
  @Produces("application/json")
  public abstract PSGetUserResult validatePwdResetKey(@PathParam("resetKey") String resetKey);

  /**
   * Rest service method to reset the user password.
   *
   * @param resetKey String containing the token key, may not be <code>null</code>.
   * @param resetRequest An {@link PSMembershipAccount} object with the parameters to associate the
   *     new password to the user.
   * @return An {@link PSLoginResult} object containing the session, may be empty but never <code>
   *     null</code>.
   */
  @POST
  @Path("/pwd/reset/{resetKey:.*}")
  @Produces("application/json")
  public abstract PSLoginResult resetPwd(
      @PathParam("resetKey") String resetKey, PSMembershipAccount resetRequest);

  /**
   * Rest service method to confirm a user account via the supplied confirm key.
   *
   * @param confirmKey String containing the token key, may not be <code>null</code>.
   * @return An {@link PSLoginResult} object containing the session, may be empty but never <code>
   *     null</code>.
   */
  @POST
  @Path("/registration/confirm/{rvkey:.*}")
  @Produces("application/json")
  public abstract PSLoginResult confirmAccount(@PathParam("rvkey") String confirmKey);

  /**
   * Rest service method to enumerate all registered users.
   *
   * @return a list of user summaries, never {@code null}, may be empty.
   */
  @GET
  @Path("/admin/users")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract List<PSUserSummary> findUserGroups();

  /**
   * Rest service method to update the groups for a given user.
   *
   * @param userSummary the user groups payload, may not be {@code null}.
   */
  @PUT
  @Path("/admin/user/group/{siteName}")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract void updateUserGroups(PSUserGroup userSummary);
}
