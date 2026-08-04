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
package com.percussion.secure.services;

import com.percussion.secure.data.PSMembershipConfiguration;
import com.percussion.utils.string.PSStringUtils;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Provides authentication for Active Directory users and CM1 registered members using Spring
 * Security.
 *
 * @author Jay Seletz
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
public class PSMembershipAuthProvider extends AbstractUserDetailsAuthenticationProvider {
  private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();
  private static final Client msClient = ClientBuilder.newClient();
  private PSMembershipConfiguration membershipConfig;
  private PSLdapMembershipAuthProvider ldapMembershipAuthProvider;
  private String accessGroupFileName;
  private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

  /**
   * Sets the membership-service configuration this provider uses to build the membership REST URLs
   * and to read the {@code useLdap} switch.
   *
   * @param membershipConfig the membership-service configuration, assumed not {@code null}.
   */
  public void setMembershipConfig(PSMembershipConfiguration membershipConfig) {
    this.membershipConfig = membershipConfig;
  }

  /**
   * Sets the LDAP authentication provider used when {@code membershipConfig.useLdap=yes}.
   *
   * @param ldapMembershipAuthProvider the LDAP provider, assumed not {@code null}.
   */
  public void setLdapMembershipAuthProvider(
      PSLdapMembershipAuthProvider ldapMembershipAuthProvider) {
    this.ldapMembershipAuthProvider = ldapMembershipAuthProvider;
  }

  /**
   * Sets the access-group XML file path used to authorize the user after a successful login.
   *
   * @param accessGroupFileName the web-application-relative path to the XML file, never {@code
   *     null}.
   */
  public void setAccessGroupFileName(String accessGroupFileName) {
    this.accessGroupFileName = accessGroupFileName;
  }

  /**
   * Gets the current thread's session id for the authenticated subject, then clears it.
   *
   * @return The session id, or null if there is no authenticated session.
   */
  public static String getAuthenticatedSessionId() {
    var sessionId = SESSION_ID.get();
    SESSION_ID.set(null);
    return sessionId;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (membershipConfig != null && "yes".equalsIgnoreCase(membershipConfig.getUseLdap())) {
      return ldapMembershipAuthProvider.authenticate(authentication);
    }
    return super.authenticate(authentication);
  }

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
      throws AuthenticationException {
    // No additional checks required for this implementation
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication)
      throws AuthenticationException {
    var groups = login(username, Objects.toString(authentication.getCredentials(), ""));
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    if (StringUtils.isEmpty(groups)) {
      return new User(username, "", authorities);
    }
    var groupList = PSStringUtils.getAllowedGroups(groups);
    var groupsFromFile = PSMembershipAuthUtils.getAccessGroupsFromXML(accessGroupFileName);
    for (var group : groupList) {
      var trimmedGroup = StringUtils.strip(group);
      if (groupsFromFile != null
          && !groupsFromFile.isEmpty()
          && groupsFromFile.contains("'" + trimmedGroup.toUpperCase() + "'")) {
        authorities.add(new SimpleGrantedAuthority(trimmedGroup));
      }
    }
    if (!authorities.isEmpty()) {
      return new User(username, "", authorities);
    }
    logger.error(
        "User Not Authorized - PSMembershipAuthProvider.createSuccessfulAuthentication()",
        new AuthorizationServiceException("User Not Authorized"));
    throw new AuthorizationServiceException("User Not Authorized");
  }

  /**
   * Logs in the user and returns the groups the user is a member of.
   *
   * @param userId Assumed not null
   * @param password Assumed not null
   * @return A comma-delimited list of group names, never null, may be empty.
   * @throws BadCredentialsException if authentication fails for any reason.
   */
  private String login(String userId, String password) throws BadCredentialsException {
    try {
      var sessionId = authenticateMember(userId, password);
      var groups = getMemberGroups(sessionId, userId);
      SESSION_ID.set(sessionId);
      return groups;
    } catch (JSONException e) {
      logger.error("Error authenticating user " + userId, e);
      logger.debug(e.getMessage(), e);
      throw new BadCredentialsException("");
    }
  }

  /**
   * Authenticates the supplied credentials.
   *
   * @return The session id if successful
   * @throws JSONException If there is an error parsing a REST response
   */
  private String authenticateMember(String userId, String password) throws JSONException {
    var webTarget =
        msClient.target(
            membershipConfig.getBaseUrl() + "/perc-membership-services/membership/login");
    var request = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", userId, password);
    var invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);
    var response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON));
    var resultObj = getJSONResult(response);
    var sessionId = resultObj.getString("sessionId");
    var status = resultObj.getString("status");
    var message = resultObj.getString("message");
    if (!"SUCCESS".equals(status)) {
      if ("AUTH_FAILED".equals(status)) {
        throw new BadCredentialsException("");
      }
      throw new BadCredentialsException(message);
    }
    return sessionId;
  }

  /**
   * Gets the groups for the supplied session id.
   *
   * @param sessionId The session id to use
   * @param userId Used for any error messages
   * @return The list of groups as a comma-delimited string, upper-cased for case-insensitivity.
   * @throws JSONException If there is an error parsing a REST response
   */
  private String getMemberGroups(String sessionId, String userId) throws JSONException {
    var webTarget =
        msClient.target(
            membershipConfig.getBaseUrl() + "/perc-membership-services/membership/session");
    var request = String.format("{\"sessionId\": \"%s\"}", sessionId);
    var invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);
    var response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON));
    var resultObj = getJSONResult(response);
    var summaryObj = resultObj.getJSONObject("userSummary");
    var email = summaryObj.getString("email");
    if (email == null || email.isEmpty()) {
      logger.error("Unable to retrieve session info for user: " + userId);
      throw new BadCredentialsException("");
    }
    var groups = summaryObj.optString("groups", "");
    return groups.toUpperCase();
  }

  /**
   * Checks the response status and if successful returns the response as a JSON object.
   *
   * @param response The response to check, assumed not null
   * @return The JSON Object, never null.
   * @throws JSONException If the response cannot be parsed as a JSON object.
   * @throws BadCredentialsException If the response does not have a 200 status.
   */
  private JSONObject getJSONResult(Response response)
      throws JSONException, BadCredentialsException {
    if (response.getStatus() != 200) {
      throw new BadCredentialsException("Failed : HTTP error code : " + response.getStatus());
    }
    var jsonString = response.readEntity(String.class);
    return new JSONObject(jsonString);
  }

  /** Default constructor */
  public PSMembershipAuthProvider() {
    super();
  }
}
