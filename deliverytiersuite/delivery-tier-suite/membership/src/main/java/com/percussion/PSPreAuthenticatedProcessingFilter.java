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

package com.percussion;

// REFACTORED: CP-JAVA11

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Pre-authenticated processing filter for Percussion Membership Services. Handles authentication
 * based on headers or servlet principal using Java 11 features.
 *
 * @author Percussion Software
 * @since 8.1.6
 */
public class PSPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

  private static final String TOMCAT_USER_HEADER = "tomcat-user";
  private static final String TOMCAT_PASSWORD_HEADER = "tomcat-password";
  private static final String TOMCAT_ROLES_HEADER = "tomcat-roles";
  private static final String ANONYMOUS_USER = "ANONYMOUS";
  private static final String DEFAULT_PASSWORD = "N/A";
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String PS_MANAGER_USER = "ps_manager";
  private static final String DELIVERY_MANAGER_ROLE = "deliverymanager";
  private static final String ANONYMOUS_ROLE = "ANONYMOUS";

  /**
   * Default constructor for the filter. Wires up a {@link PSAuthenticationDetailsSource} so the
   * filter can build authentication tokens on each request.
   */
  @SuppressWarnings("this-escape")
  public PSPreAuthenticatedProcessingFilter() {
    setAuthenticationDetailsSource(new PSAuthenticationDetailsSource());
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    return Optional.ofNullable(request.getUserPrincipal())
        .map(Principal::getName)
        .or(() -> Optional.ofNullable(request.getHeader(TOMCAT_USER_HEADER)))
        .orElse(ANONYMOUS_USER);
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(TOMCAT_PASSWORD_HEADER)).orElse(DEFAULT_PASSWORD);
  }

  /**
   * Authentication details source that builds pre-authenticated tokens with roles. Uses Java 11
   * features for cleaner, more maintainable code.
   */
  public static class PSAuthenticationDetailsSource
      implements AuthenticationDetailsSource<
          HttpServletRequest, PreAuthenticatedAuthenticationToken> {

    /** Default constructor for the static nested class. */
    public PSAuthenticationDetailsSource() {}

    @Override
    public PreAuthenticatedAuthenticationToken buildDetails(HttpServletRequest request) {
      var userName = extractUserName(request);
      var password = extractPassword(request);
      var authorities = extractAuthorities(request, userName);

      return new PreAuthenticatedAuthenticationToken(userName, password, authorities);
    }

    /** Extract username from request principal or header using Optional. */
    private String extractUserName(HttpServletRequest request) {
      return Optional.ofNullable(request.getUserPrincipal())
          .map(Principal::getName)
          .or(() -> Optional.ofNullable(request.getHeader(TOMCAT_USER_HEADER)))
          .orElse(ANONYMOUS_USER);
    }

    /** Extract password from request header using Optional. */
    private String extractPassword(HttpServletRequest request) {
      return Optional.ofNullable(request.getHeader(TOMCAT_PASSWORD_HEADER))
          .orElse(DEFAULT_PASSWORD);
    }

    /**
     * Extract granted authorities from roles header or apply default roles. Uses Stream API for
     * functional programming approach.
     */
    private List<GrantedAuthority> extractAuthorities(HttpServletRequest request, String userName) {
      return Optional.ofNullable(request.getHeader(TOMCAT_ROLES_HEADER))
          .filter(roles -> !roles.trim().isEmpty())
          .map(this::parseRolesFromHeader)
          .orElseGet(() -> getDefaultRoles(userName));
    }

    /** Parse roles from comma-separated header value using Stream API. */
    private List<GrantedAuthority> parseRolesFromHeader(String rolesHeader) {
      return Arrays.stream(rolesHeader.split(","))
          .map(String::trim)
          .filter(role -> !role.isEmpty())
          .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
          .collect(Collectors.toList());
    }

    /** Get default roles based on username using modern conditional logic. */
    private List<GrantedAuthority> getDefaultRoles(String userName) {
      var defaultRole =
          PS_MANAGER_USER.equalsIgnoreCase(userName) ? DELIVERY_MANAGER_ROLE : ANONYMOUS_ROLE;

      return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + defaultRole));
    }
  }
}
