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
package com.percussion.sitemanage.importer.utils;

import com.percussion.sitemanage.importer.IPSConnectivity;
import com.percussion.sitemanage.importer.helpers.PSHelperTestUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that {@link PSHtmlRetriever} relies on specific text in the IOException thrown by JSoup.
 * This verifies that for the known case where we expect that text, we interpret the exception correctly.
 * This will fail if a newer version of JSoup is introduced and the exception text changes or is handled differently.
 */
class PSHtmlRetrieverTest {

    @Test
    @Disabled("Integration test - requires network access")
    void testHtml() throws Exception {
        var url = "http://samples.percussion.com/";
        var ret = new PSHtmlRetriever(new PSTestConn(url));
        var doc = ret.getHtmlDocument();
        assertNotNull(doc);
    }

    @Test
    @Disabled("Integration test - requires network access")
    void test404() throws Exception {
        var didThrow = false;
        var url = "http://samples.percussion.com/foo";
        var ret = new PSHtmlRetriever(new PSTestConn(url));
        try {
            ret.getHtmlDocument();
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            didThrow = true;
        }
        assertTrue(didThrow);
    }

    @Test
    @Disabled("Integration test - requires network access")
    void testNonHtmlContent() throws Exception {
        var url = "http://samples.percussion.com/assets/snow.jpg";
        var ret = new PSHtmlRetriever(new PSTestConn(url));
        var doc = ret.getHtmlDocument();
        assertNull(doc);

        // make sure it's really there
        var testConnectivity = new PSTestConn(url);
        var connection = testConnectivity.getConnection();
        connection.ignoreContentType(true);
        doc = connection.get();
        assertNotNull(doc);
    }

    private static class PSTestConn implements IPSConnectivity {
        private final Connection miConn;

        private PSTestConn(String url) {
            miConn = Jsoup.connect(url);
            miConn.ignoreContentType(false);
            miConn.followRedirects(false);
            miConn.userAgent(PSHelperTestUtils.USER_AGENT);
        }

        @Override
        public Document get() throws IOException {
            return miConn.get();
        }

        @Override
        public int getResponseStatusCode() {
            return miConn.response().statusCode();
        }

        @Override
        public String getResponseUrl() {
            return miConn.response().url().toString();
        }

        public Connection getConnection() {
            return miConn;
        }
    }
}
