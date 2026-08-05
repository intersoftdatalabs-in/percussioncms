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

package com.percussion.auditlog;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Audit event emitted whenever a user authenticates against the CMS — covering login, renewal,
 * session revocation, and logout actions. Carries the session id, role claims, and community name
 * so the downstream audit sink can attribute the action.
 */
public class PSAuthenticationEvent extends AbstractEvent {

  /** Tag identifying the HTTP session id associated with the authentication action. */
  public static final String SESSIONID_TAG = "sessionid";

  /** Tag identifying the role(s) granted as part of the authentication action. */
  public static final String ROLES_TAG = "roles";

  /** Tag identifying the community the authenticated user belongs to. */
  public static final String COMMUNITYNAME_TAG = "communityName";

  /** Resource URI for a single user account. */
  public static final String USER_URI = "data/security/account/user";

  /** Resource URI for the system security service that records authentication events. */
  public static final String SYSTEM_SECURITY_URI = "service/bss/cms/security";

  /** Constructs an empty event with the security observer pre-assigned. */
  @SuppressWarnings("this-escape")
  public PSAuthenticationEvent() {
    super();

    this.setObserverName(SYSTEM_SECURITY_URI);
  }

  /**
   * Constructs an authentication event fully populated from the supplied servlet request and
   * metadata.
   *
   * @param outcome the action outcome (typically {@code SUCCESS} or {@code FAILURE}), never {@code
   *     null}.
   * @param action the authentication action enum value, never {@code null}.
   * @param request the HTTP request that triggered the event, never {@code null}.
   * @param username the user name captured by the authentication attempt, never {@code null}.
   */
  @SuppressWarnings("this-escape")
  public PSAuthenticationEvent(
      String outcome,
      AuthenticationEventActions action,
      HttpServletRequest request,
      String username) {
    super();
    this.setObserverName(SYSTEM_SECURITY_URI);
    this.setOutcome(outcome);
    this.setAction(action);
    this.setInitiatorIP(request.getRemoteAddr());
    this.setTargetUsername(username);
    this.setAgentName(request.getHeader("User-Agent"));
  }

  /** Enumerates the authentication lifecycle actions recorded by {@link PSAuthenticationEvent}. */
  public enum AuthenticationEventActions {
    /** A user signed in successfully. */
    login,
    /** An existing session was renewed or refreshed. */
    renew,
    /** A previously issued session was explicitly revoked. */
    revoke,
    /** The user signed out, terminating the session. */
    logout
  }

  private AuthenticationEventActions action;

  private String sessionId;
  private String roles;
  private String communityName;

  /**
   * Returns the action recorded for this event.
   *
   * @return the action, may be {@code null} when not yet set.
   */
  public AuthenticationEventActions getAction() {
    return action;
  }

  /**
   * Sets the action recorded for this event.
   *
   * @param action the action to record, never {@code null}.
   */
  public void setAction(AuthenticationEventActions action) {
    this.action = action;
  }

  /**
   * Returns the HTTP session id associated with the event.
   *
   * @return the session id, may be {@code null} when not set.
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Sets the HTTP session id associated with the event.
   *
   * @param sessionId the session id, never {@code null} or empty.
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Returns the role(s) granted by the authentication action, typically a comma-delimited list.
   *
   * @return the roles string, may be {@code null} when not set.
   */
  public String getRoles() {
    return roles;
  }

  /**
   * Sets the role(s) granted by the authentication action.
   *
   * @param roles the roles string, may be {@code null}.
   */
  public void setRoles(String roles) {
    this.roles = roles;
  }

  /**
   * Returns the community name associated with the authentication.
   *
   * @return the community name, may be {@code null} when not set.
   */
  public String getCommunityName() {
    return communityName;
  }

  /**
   * Sets the community name associated with the authentication.
   *
   * @param communityName the community name, may be {@code null}.
   */
  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }
}
