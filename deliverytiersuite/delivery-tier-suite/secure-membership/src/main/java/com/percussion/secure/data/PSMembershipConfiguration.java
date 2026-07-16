// REFACTORED: CP-JAVA11
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
package com.percussion.secure.data;

import java.util.Objects;

/** @deprecated This class is part of the deprecated secure-membership module. */
@Deprecated
public class PSMembershipConfiguration {
  // Set by Spring beans config
  private String membershipServiceHost;
  private String membershipServiceProtocol;
  private String membershipServicePort;
  private String membershipSessionCookieName;
  private String useLdap;

  /** Set on first access by {@link #getBaseUrl()}, not modified after that. */
  private String baseUrl = null;

  /**
   * Gets the name of the cookie used to store the membership session ID.
   *
   * @return The name, never {@code null} or empty.
   */
  public String getMembershipSessionCookieName() {
    return membershipSessionCookieName;
  }

  /**
   * Sets the host to use to access the membership service.
   *
   * @param membershipServiceHost The host name, must not be {@code null} or empty.
   */
  public void setMembershipServiceHost(String membershipServiceHost) {
    Objects.requireNonNull(membershipServiceHost, "membershipServiceHost must not be null");
    if (membershipServiceHost.isEmpty()) {
      throw new IllegalArgumentException("membershipServiceHost must not be empty");
    }
    this.membershipServiceHost = membershipServiceHost;
  }

  /**
   * Sets the protocol to use to access the membership service.
   *
   * @param membershipServiceProtocol The protocol (http or https), must not be {@code null} or
   *     empty.
   */
  public void setMembershipServiceProtocol(String membershipServiceProtocol) {
    Objects.requireNonNull(membershipServiceProtocol, "membershipServiceProtocol must not be null");
    if (membershipServiceProtocol.isEmpty()) {
      throw new IllegalArgumentException("membershipServiceProtocol must not be empty");
    }
    this.membershipServiceProtocol = membershipServiceProtocol;
  }

  /**
   * Sets the port to use to access the membership service.
   *
   * @param membershipServicePort The port, must not be {@code null} or empty, should be valid for
   *     the specified protocol.
   */
  public void setMembershipServicePort(String membershipServicePort) {
    Objects.requireNonNull(membershipServicePort, "membershipServicePort must not be null");
    if (membershipServicePort.isEmpty()) {
      throw new IllegalArgumentException("membershipServicePort must not be empty");
    }
    this.membershipServicePort = membershipServicePort;
  }

  /**
   * Sets the cookie name to use when setting the session ID cookie for membership.
   *
   * @param membershipSessionCookieName The cookie name, must not be {@code null} or empty.
   */
  public void setMembershipSessionCookieName(String membershipSessionCookieName) {
    Objects.requireNonNull(
        membershipSessionCookieName, "membershipSessionCookieName must not be null");
    if (membershipSessionCookieName.isEmpty()) {
      throw new IllegalArgumentException("membershipSessionCookieName must not be empty");
    }
    this.membershipSessionCookieName = membershipSessionCookieName;
  }

  /**
   * Gets the base URL to use for the membership service host.
   *
   * @return The URL, never {@code null} or empty.
   */
  public String getBaseUrl() {
    if (baseUrl == null) {
      baseUrl =
          membershipServiceProtocol + "://" + membershipServiceHost + ":" + membershipServicePort;
    }
    return baseUrl;
  }

  /**
   * Gets the property which defines whether to use secure LDAP membership.
   *
   * @return The value provided by the user in the perc-secured-sections.properties file for
   *     perc.use.ldap.
   */
  public String getUseLdap() {
    return useLdap;
  }

  /**
   * Sets the value from the property file for secure LDAP membership usage.
   *
   * @param useLdap The value to set.
   */
  public void setUseLdap(String useLdap) {
    this.useLdap = useLdap;
  }
}
