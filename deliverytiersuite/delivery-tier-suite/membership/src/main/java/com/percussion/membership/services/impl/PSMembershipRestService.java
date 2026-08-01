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
package com.percussion.membership.services.impl;

import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.membership.data.PSAccountCreateResult;
import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.PSGetUserResult;
import com.percussion.membership.data.PSLoginRequest;
import com.percussion.membership.data.PSLoginResult;
import com.percussion.membership.data.PSMembershipAccount;
import com.percussion.membership.data.PSMembershipResult;
import com.percussion.membership.data.PSMembershipResult.STATUS;
import com.percussion.membership.data.PSResetRequest;
import com.percussion.membership.data.PSUserGroup;
import com.percussion.membership.data.PSUserSession;
import com.percussion.membership.data.PSUserSummary;
import com.percussion.membership.services.IPSMembershipRestService;
import com.percussion.membership.services.IPSMembershipService;
import com.percussion.membership.services.PSAuthenticationFailedException;
import com.percussion.membership.services.PSMemberExistsException;
import com.percussion.membership.services.PSResetPwdException;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * REST/Webservice layer used to access the membership service.
 *
 * @author jayseletz
 */
@Path("/membership")
@Component
@Scope("singleton")
public class PSMembershipRestService extends PSAbstractRestService
    implements IPSMembershipRestService {
  @Autowired private IPSMembershipService membershipService;

  private static final Logger log = LogManager.getLogger(PSMembershipRestService.class);

  /** Default constructor for frameworks that require it (e.g. Jersey). */
  public PSMembershipRestService() {}

  /**
   * Ctor, autowired by spring.
   *
   * @param service The service to use, may not be <code>null</code>.
   */
  public PSMembershipRestService(IPSMembershipService service) {
    Validate.notNull(service);
    membershipService = service;
  }

  /**
   * Sets up CSRF-related headers by echoing the XSRF-TOKEN cookie value as the X-XSRF-TOKEN header
   * for CSRF-aware callers.
   *
   * @param request the current request, supplied by the JAX-RS runtime.
   * @param response the current response, supplied by the JAX-RS runtime.
   */
  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (Cookie cookie : cookies) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
        response.setHeader("X-CSRF-TOKEN", cookie.getValue());
      }
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#createUser(com.percussion.membership.data.PSMembershipAccount, jakarta.ws.rs.core.HttpHeaders)
   */
  @Override
  @POST
  @Path("/user")
  @Produces("application/json")
  public PSMembershipResult createUser(
      PSMembershipAccount membership, @Context HttpHeaders header) {
    Validate.notNull(membership);

    if (!validateNotEmpty(membership.getEmail())) {
      log.error("Email may not be empty.");
      return new PSAccountCreateResult(STATUS.INVALID_PARAM, "Email may not be empty", "");
    }

    if (!validateNotEmpty(membership.getPassword())) {
      log.error("Password may not be empty.");
      return new PSAccountCreateResult(STATUS.INVALID_PARAM, "Password may not be empty", "");
    }

    if (log.isDebugEnabled()) {
      log.debug("Http Header in the service is :{}", header.getRequestHeaders());
    }

    PSAccountCreateResult result;
    try {
      MultivaluedMap<String, String> headerParams = header.getRequestHeaders();
      String[] host = headerParams.getFirst("host").split(":");
      String customerSite = host[0].toUpperCase();
      String sessionId =
          membershipService.createAccount(
              membership.getEmail(),
              membership.getPassword(),
              membership.isConfirmationRequired(),
              membership.getConfirmationPage(),
              customerSite);
      result = new PSAccountCreateResult(STATUS.SUCCESS, "", sessionId);
    } catch (PSMemberExistsException e) {
      log.error("Membership exists exception : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSAccountCreateResult(STATUS.MEMBER_EXISTS, e.getLocalizedMessage(), "");
    } catch (PSAuthenticationFailedException e) {
      log.error("Authentication failed exception : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSAccountCreateResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), "");
    } catch (Exception e) {
      log.error("Exception during create user : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSAccountCreateResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), "");
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#changeStateAccount(com.percussion.membership.data.PSAccountSummary)
   */
  @Override
  @PUT
  @Path("/admin/account")
  @RolesAllowed("deliverymanager")
  public void changeStateAccount(PSAccountSummary account) {
    if (account == null) {
      log.error("Illegal argument passed. account cannot be null.");
      return;
    }
    try {
      membershipService.changeStateAccount(account);
    } catch (Exception e) {
      log.error("Could not change state account : {}", PSExceptionUtils.getMessageForLog(e));

      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#deleteAccount(java.lang.String)
   */
  @Override
  @DELETE
  @Path("/admin/account/{email:.*}")
  @RolesAllowed("deliverymanager")
  public void deleteAccount(@PathParam("email") String email) {
    try {
      membershipService.deleteAccount(email);
    } catch (Exception e) {
      log.error("Exception while deleting account : {}", PSExceptionUtils.getMessageForLog(e));

      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#getUser(java.lang.String)
   */
  @Override
  @POST
  @Path("/session")
  @Produces(MediaType.APPLICATION_JSON)
  public PSGetUserResult getUser(PSUserSession psUserSession) {
    String sessionId = psUserSession.getSessionId();
    if (!validateNotEmpty(sessionId)) {
      log.error("Illegal argument passed. session id cannot be empty.");
      return new PSGetUserResult(STATUS.INVALID_PARAM, "No session Id supplied", null);
    }

    PSGetUserResult result;
    try {
      PSUserSummary userSum = membershipService.getUser(sessionId);

      if (log.isDebugEnabled()) {
        log.debug(
            "The user email is {} and the user status is : {}",
            userSum.getEmail(),
            userSum.getStatus());
      }
      result = new PSGetUserResult(STATUS.SUCCESS, "", userSum);
    } catch (Exception e) {
      log.error("Exception while getting user : {}", PSExceptionUtils.getMessageForLog(e));

      result = new PSGetUserResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), null);
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#login(com.percussion.membership.data.PSLoginRequest)
   */
  @Override
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  public PSLoginResult login(PSLoginRequest loginRequest) {
    Validate.notNull(loginRequest);
    String email = loginRequest.getEmail();
    String password = loginRequest.getPassword();

    if (!validateNotEmpty(email)) {
      log.error("Email may not be empty.");
      return new PSLoginResult(STATUS.INVALID_PARAM, "Email may not be empty", "");
    }

    if (!validateNotEmpty(password)) {
      log.error("Password may not be empty.");
      return new PSLoginResult(STATUS.INVALID_PARAM, "Password may not be empty", "");
    }

    if (log.isDebugEnabled()) {
      log.debug("Email in the login request is : {}", loginRequest.getEmail());
    }

    PSLoginResult result;
    try {
      String sessionId = membershipService.login(email, password);
      result = new PSLoginResult(STATUS.SUCCESS, "", sessionId);
    } catch (PSAuthenticationFailedException e) {
      log.error(
          "Could not log in! Authentication failed : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), "");
    } catch (Exception e) {
      log.error("Exception occurred while login : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), "");
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#logout(java.lang.String)
   */
  @Override
  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  public PSMembershipResult logout(PSUserSession psUserSession) {
    String sessionId = psUserSession.getSessionId();
    if (!validateNotEmpty(sessionId)) {
      log.error("Illegal argument passed. session id cannot be empty.");
      return new PSMembershipResult(STATUS.INVALID_PARAM, "No session Id supplied");
    }

    PSMembershipResult result;

    try {
      membershipService.logout(sessionId);
      result = new PSMembershipResult(STATUS.SUCCESS, "");
    } catch (Exception e) {
      log.error("Exception occurred while logout : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSMembershipResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#requestPwdReset(com.percussion.membership.data.PSResetRequest, jakarta.ws.rs.core.HttpHeaders)
   */
  @Override
  @POST
  @Path("/pwd/requestReset")
  @Produces(MediaType.APPLICATION_JSON)
  public PSMembershipResult requestPwdReset(
      PSResetRequest resetRequest, @Context HttpHeaders header) {
    Validate.notNull(resetRequest);
    String email = resetRequest.getEmail();
    String resetLinkUrl = resetRequest.getRedirectPage();

    if (!validateNotEmpty(email)) {
      log.error("Email may not be empty.");
      return new PSLoginResult(STATUS.INVALID_PARAM, "Email may not be empty", "");
    }

    if (log.isDebugEnabled()) {
      log.debug("Http Header in the service is : {}", header.getRequestHeaders());
    }

    PSMembershipResult result;
    try {
      String resetKey = membershipService.setResetKey(email, resetLinkUrl);
      result = new PSAccountCreateResult(STATUS.SUCCESS, "", resetKey);
    } catch (PSAuthenticationFailedException e) {
      log.error("Authentication Failed! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSAccountCreateResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), "");
    } catch (Exception e) {
      log.error(
          "Exception occurred while requesting password reset : {}",
          PSExceptionUtils.getMessageForLog(e));
      result = new PSAccountCreateResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), "");
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#validatePwdResetKey(java.lang.String)
   */
  @Override
  @POST
  @Path("/pwd/validate/{resetKey:.*}")
  @Produces("application/json")
  public PSGetUserResult validatePwdResetKey(@PathParam("resetKey") String resetKey) {
    Validate.notNull(resetKey);

    PSGetUserResult result;

    if (!validateNotEmpty(resetKey)) {
      log.error("Parameters may not be empty.");
      result = new PSGetUserResult(STATUS.INVALID_PARAM, "Parameters may not be empty", null);
    }

    try {
      PSUserSummary sum = membershipService.validatePwdResetKey(resetKey);
      result = new PSGetUserResult(STATUS.SUCCESS, "", sum);
    } catch (PSResetPwdException e) {
      log.error("Reset Password Exception! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSGetUserResult(STATUS.INVALID_RESET_KEY, e.getLocalizedMessage(), null);
    } catch (PSAuthenticationFailedException e) {
      log.error("Authentication Failed! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSGetUserResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), null);
    } catch (Exception e) {
      log.error(
          "Exception occurred while validating password reset : {}",
          PSExceptionUtils.getMessageForLog(e));
      result = new PSGetUserResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), null);
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#resetPwd(java.lang.String, com.percussion.membership.data.PSMembershipAccount)
   */
  @Override
  @POST
  @Path("/pwd/reset/{resetKey:.*}")
  @Produces("application/json")
  public PSLoginResult resetPwd(
      @PathParam("resetKey") String resetKey, PSMembershipAccount resetRequest) {
    Validate.notNull(resetRequest);
    String email = resetRequest.getEmail();
    String password = resetRequest.getPassword();

    if (!validateNotEmpty(resetKey) || !validateNotEmpty(email) || !validateNotEmpty(password)) {
      log.error("Parameters may not be empty.");
      return new PSLoginResult(STATUS.INVALID_PARAM, "Parameters may not be empty", "");
    }

    PSLoginResult result;
    try {
      String sessionId = membershipService.resetPwd(resetKey, email, password);
      result = new PSLoginResult(STATUS.SUCCESS, "", sessionId);
    } catch (PSResetPwdException e) {
      log.error("Reset Password Exception! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.INVALID_RESET_KEY, e.getLocalizedMessage(), "");
    } catch (PSAuthenticationFailedException e) {
      log.error("Authentication Failed! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), "");
    } catch (Exception e) {
      log.error(
          "Exception occurred while resetting password : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), "");
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#confirmAccount(java.lang.String)
   */
  @Override
  @POST
  @Path("/registration/confirm/{rvkey:.*}")
  @Produces("application/json")
  public PSLoginResult confirmAccount(@PathParam("rvkey") String confirmKey) {
    if (!validateNotEmpty(confirmKey)) {
      log.error("Parameters may not be empty.");
      return new PSLoginResult(STATUS.INVALID_PARAM, "Parameters may not be empty", "");
    }

    PSLoginResult result;
    try {
      String memberId = membershipService.confirmAccount(confirmKey);
      result = new PSLoginResult(STATUS.SUCCESS, "", memberId);
    } catch (PSResetPwdException e) {
      log.error("Reset Password Exception! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.INVALID_RESET_KEY, e.getLocalizedMessage(), "");
    } catch (PSAuthenticationFailedException e) {
      log.error("Authentication Failed! : {}", PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.AUTH_FAILED, e.getLocalizedMessage(), "");
    } catch (Exception e) {
      log.error(
          "Exception occurred while registration confirmation : {}",
          PSExceptionUtils.getMessageForLog(e));
      result = new PSLoginResult(STATUS.UNEXPECTED_ERROR, e.getLocalizedMessage(), "");
    }

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#findUserGroups()
   */
  @Override
  @GET
  @Path("/admin/users")
  @RolesAllowed("deliverymanager")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PSUserSummary> findUserGroups() {
    try {
      return membershipService.findUsers();
    } catch (Exception e) {
      log.error(
          "Exception occurred while finding user groups : {}",
          PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.membership.services.impl.IPSMembershipRestService#updateUserGroups(com.percussion.membership.data.PSUserGroup)
   */
  @Override
  @PUT
  @Path("/admin/user/group/{siteName}")
  @RolesAllowed("deliverymanager")
  @Produces(MediaType.APPLICATION_JSON)
  public void updateUserGroups(PSUserGroup userSummary) {
    Validate.notNull(userSummary);
    String email = userSummary.getEmail();
    String groups = userSummary.getGroups();

    if (log.isDebugEnabled()) {
      log.debug("The user email is {} and the groups are {}", email, groups);
    }

    try {
      membershipService.setUserGroups(email, groups);
    } catch (Exception e) {
      log.error(
          "Exception occurred while updating user groups : {}",
          PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /**
   * Validate the supplied string to be not <code>null</code> and not empty.
   *
   * @param string The string to validate.
   * @return <code>true</code> if it is valid, <code>false</code> if not.
   */
  private boolean validateNotEmpty(String string) {
    return string != null && string.trim().length() > 0;
  }

  @Override
  public String getVersion() {

    String version = super.getVersion();

    log.info("getVersion() from PSMembershipRestService ...{}", version);

    return version;
  }

  @DELETE
  @Path("/updateOldSiteEntries/{prevSiteName}/{newSiteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  @Override
  public Response updateOldSiteEntries(
      @PathParam("prevSiteName") String prevSiteName,
      @PathParam("newSiteName") String newSiteName) {
    log.debug("Nothing to do for membership service. Prev name is: {}", prevSiteName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
