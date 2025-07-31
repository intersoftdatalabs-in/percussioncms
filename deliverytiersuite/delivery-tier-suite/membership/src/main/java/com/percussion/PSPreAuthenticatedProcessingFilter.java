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

import org.apache.catalina.users.MemoryRole;
import org.apache.catalina.users.MemoryUser;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Pre-authenticated filter for Percussion CMS.
 * Sunny Sal: "Security so tight, even your mom can't log in without a token!"
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

    public static class PSAuthenticationDetailsSource implements
            AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedAuthenticationToken> {
        @Override
        public PreAuthenticatedAuthenticationToken buildDetails(HttpServletRequest request) {
            Principal principal = request.getUserPrincipal();
            if (principal == null || !MemoryUser.class.isAssignableFrom(principal.getClass())) {
                var userName = request.getHeader("tomcat-user");
                var password = request.getHeader("tomcat-password");
                if (userName != null && userName.equalsIgnoreCase("ps_manager")) {
                    var grantedAuthorities = new ArrayList<GrantedAuthority>();
                    grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_deliverymanager"));
                    return new PreAuthenticatedAuthenticationToken(userName, password, grantedAuthorities);
                } else {
                    return new PreAuthenticatedAuthenticationToken("ANONYMOUS", "N/A");
                }
            } else {
                var memoryUser = (MemoryUser) principal;
                var grantedAuthorities = new ArrayList<GrantedAuthority>();
                var roles = memoryUser.getRoles();
                while (roles.hasNext()) {
                    var role = (MemoryRole) roles.next();
                    var roleName = "ROLE_" + role.getName();
                    grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
                }
                return new PreAuthenticatedAuthenticationToken(memoryUser.getName(), memoryUser.getPassword(), grantedAuthorities);
            }
        }
    }
}
