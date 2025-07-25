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

// REFACTORED: CP-JAVA11
package com.percussion.delivery.integrations;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.Arrays;

/**
 * REST service for integrations endpoints (e.g., CSRF token support).
 * <p>
 * Modernized for Java 11 and Google Java Style.
 * </p>
 */
@Path("/integrations")
@Component
public class IntegrationsRestService {
    public IntegrationsRestService() {
        // NOOP
    }

    /**
     * Sets CSRF headers if XSRF-TOKEN cookie is present.
     * @param request HTTP request
     * @param response HTTP response
     */
    @HEAD
    @Path("/csrf")
    public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        Arrays.stream(cookies)
                .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
                .findFirst()
                .ifPresent(cookie -> {
                    response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
                    response.setHeader("X-CSRF-TOKEN", cookie.getValue());
                });
    }
}
