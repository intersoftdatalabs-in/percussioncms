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

package com.percussion.secure.services;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

/**
 * Processes a Spring Security username / password form authentication request for the
 * secure-membership flow. Carries forward the standard Spring Security username, password, and
 * last-username form field keys plus the {@code postOnly} check that rejects non-POST attempts with
 * an {@link AuthenticationServiceException}.
 *
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
// REFACTORED: CP-JAVA11
public class AuthFormProcessingFilter extends AbstractAuthenticationProcessingFilter {
  /** Form field key carrying the submitted username on the authentication request. */
  public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "j_username";

  /** Form field key carrying the submitted password on the authentication request. */
  public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "j_password";

  /** Session key used to remember the last successfully authenticated username. */
  public static final String SPRING_SECURITY_LAST_USERNAME_KEY = "SPRING_SECURITY_LAST_USERNAME";

  private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;
  private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;
  private boolean postOnly = true;

  /**
   * Constructs a filter that processes authentication submissions against the supplied URL.
   *
   * @param defaultFilterProcessesUrl the URL this filter handles, never {@code null}.
   */
  protected AuthFormProcessingFilter(String defaultFilterProcessesUrl) {
    super(defaultFilterProcessesUrl);
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse httpServletResponse)
      throws AuthenticationException, IOException, ServletException {
    if (postOnly && !"POST".equals(request.getMethod())) {
      throw new AuthenticationServiceException(
          "Authentication method not supported: " + request.getMethod());
    }

    var username = obtainUsername(request);
    var password = obtainPassword(request);
    if (username == null) {
      username = "";
    }
    if (password == null) {
      password = "";
    }
    username = username.trim();
    var authRequest = new UsernamePasswordAuthenticationToken(username, password);

    // Allow subclasses to set the "details" property
    setDetails(request, authRequest);
    if (this.getAuthenticationManager() == null) {
      logger.info("Authentication manager is null.");
    } else {
      logger.info("Authentication manager was {}");
    }
    return this.getAuthenticationManager().authenticate(authRequest);
  }

  /**
   * Reads the password parameter from the supplied request using the configured {@link
   * #passwordParameter} key.
   *
   * @param request the current HTTP request, assumed not {@code null}.
   * @return the submitted password, or {@code null} if the parameter is not present.
   */
  protected String obtainPassword(HttpServletRequest request) {
    return request.getParameter(passwordParameter);
  }

  /**
   * Reads the username parameter from the supplied request using the configured {@link
   * #usernameParameter} key.
   *
   * @param request the current HTTP request, assumed not {@code null}.
   * @return the submitted username, or {@code null} if the parameter is not present.
   */
  protected String obtainUsername(HttpServletRequest request) {
    return request.getParameter(usernameParameter);
  }

  /**
   * Populates the {@code details} property of the supplied authentication token from the current
   * request so subclasses can record request-specific information.
   *
   * @param request the current HTTP request, assumed not {@code null}.
   * @param authRequest the in-progress authentication token, assumed not {@code null}.
   */
  protected void setDetails(
      HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
    authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
  }

  /**
   * Sets the form field key used to read the submitted username.
   *
   * @param usernameParameter the form field key to use; defaults to {@link
   *     #SPRING_SECURITY_FORM_USERNAME_KEY}.
   */
  public void setUsernameParameter(String usernameParameter) {
    this.usernameParameter = usernameParameter;
  }

  /**
   * Sets the form field key used to read the submitted password.
   *
   * @param passwordParameter the form field key to use; defaults to {@link
   *     #SPRING_SECURITY_FORM_PASSWORD_KEY}.
   */
  public void setPasswordParameter(String passwordParameter) {
    this.passwordParameter = passwordParameter;
  }

  /**
   * Sets whether the filter should reject non-POST authentication attempts.
   *
   * @param postOnly {@code true} to restrict authentication to HTTP POST; defaults to {@code true}.
   */
  public void setPostOnly(boolean postOnly) {
    this.postOnly = postOnly;
  }

  /**
   * Gets the form field key used to read the submitted username.
   *
   * @return the configured username parameter key, never {@code null}.
   */
  public final String getUsernameParameter() {
    return usernameParameter;
  }

  /**
   * Gets the form field key used to read the submitted password.
   *
   * @return the configured password parameter key, never {@code null}.
   */
  public final String getPasswordParameter() {
    return passwordParameter;
  }
}
