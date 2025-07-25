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

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.catalina.realm.GenericPrincipal;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Pre-authenticated processing filter for Spring Security.
 * // REFACTORED: CP-JAVA11
 */
public class PSPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public PSPreAuthenticatedProcessingFilter() {
        setAuthenticationDetailsSource(new PSAuthenticationDetailsSource());
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return "ANONYMOUS";
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }

    /**
     * Authentication details source for pre-authenticated tokens.
     */
    public static class PSAuthenticationDetailsSource implements
            AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedAuthenticationToken> {

        @Override
        public PreAuthenticatedAuthenticationToken buildDetails(HttpServletRequest request) {
            // Create container for pre-auth data
            var principal = (GenericPrincipal) request.getUserPrincipal();
            if (principal == null) {
                return new PreAuthenticatedAuthenticationToken("ANONYMOUS", "N/A");
            } else {
                List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
                for (var role : principal.getRoles()) {
                    grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                var password = principal.getPassword();
                if (password == null) {
                    password = "NO_PASSWORD";
                }
                return new PreAuthenticatedAuthenticationToken(
                        principal.getName(), password, grantedAuthorities
                );
            }
        }
    }
}
