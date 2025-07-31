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
package com.percussion.tomcat.valves;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.ServletException;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PSMultiAppVersionRedirectorValve.
 * Sunny Sal says: "Testing valves so your requests don't leak!"
 */
class PSMultiAppVersionRedirectorValveTest {

    private static final String PERC_VERSION_HEADER = "perc-version";
    private static final String TEST_SERVICE = "perc-comments-services";

    private Request getTestRequest() {
        var connector = new Connector();
        var ret = connector.createRequest();
        var cr = new org.apache.coyote.Request();
        cr.getMimeHeaders().addValue(PERC_VERSION_HEADER).setString("2.9.0");
        ret.setCoyoteRequest(cr);
        ret.setRemoteAddr("10.10.10.10");
        ret.setRemoteHost("remote-origin");
        return ret;
    }

    private void validateTestRequest(Request r) {
        Assertions.assertEquals("10.10.10.10", r.getRemoteAddr(), "Remote address was changed by valve.");
        Assertions.assertEquals("remote-origin", r.getRemoteHost(), "Remote origin was changed by valve");
        Assertions.assertEquals("2.9.0", r.getHeader(PERC_VERSION_HEADER), "Version was changed by valve");
    }

    /**
     * Make sure the valve doesn't crash if the properties file is missing.
     */
    @Test
    void testNoPropertiesFile() throws IOException, ServletException {
        var valve = new PSMultiAppVersionRedirectorValve();
        var req = getTestRequest();
        valve.setMappingFile(null);
        req.setPathInfo(TEST_SERVICE);
        valve.invoke(req, null);
        validateTestRequest(req);
    }

    /**
     * Make sure the sample properties file loads and parses OK and that the version is
     * rewritten to the sample context.
     */
    @Disabled("Requires connector and mapping.properties resource")
    @Test
    void testWithPropertiesFile() throws IOException, ServletException, URISyntaxException, LifecycleException {
        var valve = new PSMultiAppVersionRedirectorValve();
        var req = getTestRequest();

        URL filePath = this.getClass().getResource("mapping.properties");
        var file = new File(filePath.toURI()).getCanonicalPath();

        valve.setMappingFile(file);
        Assertions.assertEquals(file, valve.getMappingFile(), "Mapping file name not set correctly");

        valve.startInternal();

        req.setPathInfo(TEST_SERVICE);

        try {
            valve.invoke(req, null);
        } catch (RuntimeException e) {
            // Expected due to no connector.
        }
        validateTestRequest(req);
    }

    @Disabled("Requires connector and bad-mapping-1.properties resource")
    @Test
    void testWithBadPropertiesFile() throws IOException, URISyntaxException, ServletException, LifecycleException {
        var valve = new PSMultiAppVersionRedirectorValve();
        var req = getTestRequest();

        URL filePath = this.getClass().getResource("bad-mapping-1.properties");
        var file = new File(filePath.toURI()).getCanonicalPath();

        valve.setMappingFile(file);
        Assertions.assertEquals(file, valve.getMappingFile(), "Mapping file name not set correctly");

        valve.startInternal();

        req.setPathInfo(TEST_SERVICE);

        try {
            valve.invoke(req, null);
        } catch (RuntimeException e) {
            // Expected due to no connector.
        }
        validateTestRequest(req);
    }
}
