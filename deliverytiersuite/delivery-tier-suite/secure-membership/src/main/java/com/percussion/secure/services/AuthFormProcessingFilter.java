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
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
// REFACTORED: CP-JAVA11
public class AuthFormProcessingFilter extends AbstractAuthenticationProcessingFilter {
  public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "j_username";
  public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "j_password";
  public static final String SPRING_SECURITY_LAST_USERNAME_KEY = "SPRING_SECURITY_LAST_USERNAME";
  private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;
  private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;
  private boolean postOnly = true;

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

  protected String obtainPassword(HttpServletRequest request) {
    return request.getParameter(passwordParameter);
  }

  protected String obtainUsername(HttpServletRequest request) {
    return request.getParameter(usernameParameter);
  }

  protected void setDetails(
      HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
    authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
  }

  public void setUsernameParameter(String usernameParameter) {
    this.usernameParameter = usernameParameter;
  }

  public void setPasswordParameter(String passwordParameter) {
    this.passwordParameter = passwordParameter;
  }

  public void setPostOnly(boolean postOnly) {
    this.postOnly = postOnly;
  }

  public final String getUsernameParameter() {
    return usernameParameter;
  }

  public final String getPasswordParameter() {
    return passwordParameter;
  }
}
