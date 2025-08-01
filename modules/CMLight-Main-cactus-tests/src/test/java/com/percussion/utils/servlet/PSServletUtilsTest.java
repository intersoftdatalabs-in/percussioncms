// REFACTORED: CP-JAVA11
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
package com.percussion.utils.servlet;

import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSServletUtils} class.
 */
@Tag("IntegrationTest")
public class PSServletUtilsTest {

    @BeforeEach
    void setUp() {
        // Simulate servlet context initialization if needed
        // PSServletUtils.initialize(config.getServletContext());
    }

    /**
     * Tests {@link PSServletUtils#getConfigDir()}.
     */
    @Test
    void testGetConfigDir() {
        var configDir = PSServletUtils.getConfigDir();
        assertTrue(configDir.exists());
        assertTrue(configDir.getPath().replace('\\', '/').endsWith(
                "deploy/rxapp.ear/rxapp.war/WEB-INF/config"));
    }

    /**
     * Tests {@link PSServletUtils#getUserConfigDir()}.
     */
    @Test
    void testGetUserConfigDir() {
        var configDir = PSServletUtils.getConfigDir();
        assertTrue(configDir.exists());
        assertTrue(configDir.getPath().replace('\\', '/').endsWith(
                "deploy/rxapp.ear/rxapp.war/WEB-INF/config"));
    }

    /**
     * Tests the request dispatcher and servlet code.
     * Disabled: requires servlet container and integration setup.
     */
    //@Test
    void tobefixed_testCallServlet() throws ServletException, IOException {
        assertNotNull(PSServletUtils.getDispatcher("/rxwebdav"));
        var req = new MockHttpServletRequest();
        req.setMethod("GET");
        req.setServletPath("/contentlist");
        req.setParameter("sys_deliverytype", "filesystem");
        req.setParameter("sys_assembly_context", "301");
        req.setParameter("sys_contentlist", "rffEiFullBinary");
        req.setParameter("sys_siteid", "301");
        req.setParameter("sys_authtype", "1");
        req.setParameter("sys_context", "1");
        req.setContextPath("/Rhythmyx");
        var resp = (MockHttpServletResponse) PSServletUtils.callServlet(req);
        var content = resp.getContentAsString();
        assertNotNull(content);
        assertTrue(content.length() > 0);
        assertTrue(content.contains("<contentitem"),
                "Could not find \"<contentitem\" in \"" + content + "\"");
    }
}
