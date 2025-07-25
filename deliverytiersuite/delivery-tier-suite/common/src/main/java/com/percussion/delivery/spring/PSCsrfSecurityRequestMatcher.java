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

import java.util.regex.Pattern;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Sunny Sal here! This request matcher provides CSRF protection, allowing configuration of allowed methods and ignored paths.
 * Uses Java 11 features and Google Java Style. Ensures robust, maintainable, and secure request matching.
 * // REFACTORED: CP-JAVA11
 */
@Component
public class PSCsrfSecurityRequestMatcher implements RequestMatcher {

    private static final Logger log = LogManager.getLogger(PSCsrfSecurityRequestMatcher.class);

    private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
    private String[] ignoredPaths;
    private boolean caseInsensitive = false;

    /**
     * Constructs a CSRF request matcher.
     *
     * @param allowedMethodsPattern Regular expression listing excluded methods.
     * @param unprotectedPaths      Comma-separated list of paths to ignore.
     * @param caseInsensitive       Use case-insensitive comparison.
     */
    public PSCsrfSecurityRequestMatcher(
            String allowedMethodsPattern,
            String unprotectedPaths,
            boolean caseInsensitive) {
        Objects.requireNonNull(allowedMethodsPattern, "allowedMethodsPattern must not be null");
        Objects.requireNonNull(unprotectedPaths, "unprotectedPaths must not be null");
        this.allowedMethods = Pattern.compile(allowedMethodsPattern);
        this.caseInsensitive = caseInsensitive;
        var paths = caseInsensitive ? unprotectedPaths.toLowerCase() : unprotectedPaths;
        this.ignoredPaths = paths.split(",");
        log.debug(
                "Initializing CSRF request matcher, Allowed Methods: {}, Ignored Paths: {}",
                allowedMethodsPattern, unprotectedPaths
        );
    }

    /**
     * Determines if the request should be protected by CSRF.
     * Skips protection for allowed HTTP methods and ignored paths.
     *
     * @param request the HTTP servlet request
     * @return true if CSRF protection is required, false otherwise
     */
    @Override
    public boolean matches(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest must not be null");
        if (allowedMethods.matcher(request.getMethod()).matches()) {
            log.debug("Skipping CSRF for request method: {}", request.getMethod());
            return false;
        }

        var uri = request.getRequestURI();
        if (caseInsensitive) {
            uri = uri.toLowerCase();
        }

        for (var path : this.ignoredPaths) {
            var comparePath = caseInsensitive ? path.toLowerCase() : path;
            if (!comparePath.isEmpty() && uri.contains(comparePath)) {
                log.debug("Skipping CSRF for request URI: {}", request.getRequestURI());
                return false;
            }
        }

        log.debug("Request not filtered, requiring CSRF for request: {}", request);
        return true;
    }
}
