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

package com.percussion.delivery.spring;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.realm.GenericPrincipal;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Pre-authenticated processing filter used by the delivery tier Spring Security integration.
 * Always populates the principal as {@code ANONYMOUS}; real principal data is loaded by
 * {@link PSAuthenticationDetailsSource} from the Tomcat session user.
 */
public class PSPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

  /** Default constructor; installs the {@link PSAuthenticationDetailsSource} so that real
   * principal data is loaded from the Tomcat session user. */
  @SuppressWarnings("this-escape")
  public PSPreAuthenticatedProcessingFilter() {
    setAuthenticationDetailsSource(new PSAuthenticationDetailsSource());
  }

  /**
   * Always returns {@code ANONYMOUS} as the pre-authenticated principal; real principal data is
   * populated later by the authentication details source.
   *
   * @param request the current servlet request, never <code>null</code>.
   * @return the literal string {@code "ANONYMOUS"}.
   */
  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    return "ANONYMOUS";
  }

  /**
   * Returns {@code N/A} as the pre-authenticated credentials placeholder.
   *
   * @param request the current servlet request, never <code>null</code>.
   * @return the literal string {@code "N/A"}.
   */
  @Override
  protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
    return "N/A";
  }

  /**
   * Builds a {@link PreAuthenticatedAuthenticationToken} from the Tomcat
   * {@code GenericPrincipal} attached to the current request, copying any granted roles into
   * Spring Security {@link SimpleGrantedAuthority} instances.
   */
  public static class PSAuthenticationDetailsSource
      implements AuthenticationDetailsSource<
          HttpServletRequest, PreAuthenticatedAuthenticationToken> {
    /** Default constructor. */
    public PSAuthenticationDetailsSource() {}

    /**
     * Builds the pre-authenticated token for the supplied servlet request.
     *
     * @param request the current servlet request, never <code>null</code>.
     * @return the pre-authenticated token, never <code>null</code>.
     */
    @Override
    public PreAuthenticatedAuthenticationToken buildDetails(HttpServletRequest request) {
      // create container for pre-auth data
      GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
      if (principal == null) {
        return new PreAuthenticatedAuthenticationToken("ANONYMOUS", "N/A");
      } else {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        String[] roles = principal.getRoles();
        for (String role : roles) {
          grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        // GenericPrincipal does not expose password, use default value
        String password = "NO_PASSWORD";
        return new PreAuthenticatedAuthenticationToken(
            principal.getName(), password, grantedAuthorities);
      }
    }
  }
}
