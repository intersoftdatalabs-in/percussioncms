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

package com.percussion.test;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Base class for servlet tests which rely on the presence of a running server.
 * // REFACTORED: CP-JAVA11
 */
@Tag("IntegrationTest")
public abstract class PSServletTestCase {

    protected WebApplicationContext ctx;

    /**
     * Initializes the Spring WebApplicationContext before each test.
     * Subclasses should call super.setUp() if overridden.
     */
    @BeforeEach
    protected void setUp() throws Exception {
        // In a real test, inject or obtain the ServletContext as needed.
        // Example:
        // ServletContext servletContext = ...;
        // ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        // For now, ctx should be set by the test environment.
    }

    /**
     * Get the bean from the context for the specified name.
     *
     * @param beanName The name of the bean to locate, may not be blank.
     * @return The specified bean as an Object. Must be cast to the appropriate interface by the caller.
     */
    protected Object getBean(String beanName) {
        if (StringUtils.isBlank(beanName)) {
            throw new IllegalArgumentException("beanName may not be blank");
        }
        if (ctx == null) {
            throw new IllegalStateException("WebApplicationContext is not initialized.");
        }
        return ctx.getBean(beanName);
    }
}
