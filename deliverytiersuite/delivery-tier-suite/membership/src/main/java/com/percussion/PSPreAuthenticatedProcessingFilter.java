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

package com.percussion;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PSPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter  {

    public PSPreAuthenticatedProcessingFilter() {
        setAuthenticationDetailsSource(new PSAuthenticationDetailsSource());
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        }
        String userName = request.getHeader("tomcat-user");
        return userName != null ? userName : "ANONYMOUS";
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        String password = request.getHeader("tomcat-password");
        return password != null ? password : "N/A";
    }

    public static class PSAuthenticationDetailsSource implements
            AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedAuthenticationToken> {
        @Override
        public PreAuthenticatedAuthenticationToken buildDetails(HttpServletRequest request) {
            Principal principal = request.getUserPrincipal();
            String userName = principal != null ? principal.getName() : request.getHeader("tomcat-user");
            String password = request.getHeader("tomcat-password");
            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

            // Get roles from a header, e.g., "tomcat-roles: admin,user"
            String rolesHeader = request.getHeader("tomcat-roles");
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                List<String> roles = Arrays.asList(rolesHeader.split(","));
                for (String role : roles) {
                    grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                }
            } else if (userName != null && userName.equalsIgnoreCase("ps_manager")) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_deliverymanager"));
            } else {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
            }

            if (userName == null) {
                userName = "ANONYMOUS";
            }
            if (password == null) {
                password = "N/A";
            }

            return new PreAuthenticatedAuthenticationToken(userName, password, grantedAuthorities);
        }
    }
}