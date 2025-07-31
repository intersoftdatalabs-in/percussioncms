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

import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Sunny Sal here! This is a custom authentication provider for pre-authenticated tokens.
 * Uses Java 11 features and Google Java Style. Ensures robust, maintainable, and secure authentication.
 * // REFACTORED: CP-JAVA11
 */
@Component
public class CustomAuthenticationProvider implements
        AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    /**
     * Loads user details from a pre-authenticated token.
     * Validates input and extracts authorities for Spring Security.
     *
     * @param token the pre-authenticated token, must not be null
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException if token or details are missing
     */
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token)
            throws UsernameNotFoundException {
        Objects.requireNonNull(token, "PreAuthenticatedAuthenticationToken must not be null");
        var details = token.getDetails();
        if (!(details instanceof PreAuthenticatedAuthenticationToken)) {
            throw new UsernameNotFoundException("Token details are not of expected type");
        }
        var sessionUserDetails = (PreAuthenticatedAuthenticationToken) details;
        @SuppressWarnings("unchecked")
        var authorities = (List<GrantedAuthority>) sessionUserDetails.getAuthorities();
        // Defensive: credentials may be null, fallback to empty string
        var credentials = sessionUserDetails.getCredentials();
        var password = credentials instanceof String ? (String) credentials : "";
        return new User(
                sessionUserDetails.getName(),
                password,
                true, true, true, true,
                authorities
        );
    }
}
