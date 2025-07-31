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
package com.percussion.sitemanage.importer;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.utils.testing.IntegrationTest;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration tests for {@link PSSiteImporter}.
 * @author Lucas Piccoli, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
@Disabled("Tech debt: placeholder and legacy tests")
class PSSiteImporterTest {

    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0";

    /**
     * Placeholder test to keep JUnit happy, all other tests ignored as tech debt.
     */
    @Test
    void testNothing() {
        // No-op
    }

    @Disabled
    void ignore_testConnectToUrl() {
        var importContext = new PSSiteImportCtx();
        importContext.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
        importContext.setUserAgent(USER_AGENT);

        try {
            var invalidUrls = List.of("", null, "#$%", "www.badurl-", "www.urlwithouthttp.com");
            for (var url : invalidUrls) {
                try {
                    importContext.setSiteUrl(url);
                    PSSiteImporter.getPageContentFromSite(importContext);
                    fail("Invalid URL passed.");
                } catch (Exception e) {
                    assertEquals(IllegalArgumentException.class, e.getClass());
                }
            }

            var unreachableUrl = "http://www.unreachable123321123321123321.com";
            try {
                importContext.setSiteUrl(unreachableUrl);
                PSSiteImporter.getPageContentFromSite(importContext);
                fail("Unreachable URL didn't throw exception.");
            } catch (IOException e) {
                // Expected
            }

            var validUrl = "http://www.percussion.com";
            try {
                importContext.setSiteUrl(validUrl);
                PSSiteImporter.getPageContentFromSite(importContext);
            } catch (IOException e) {
                fail("Couldn't connect to existing URL.");
            }
        } catch (RuntimeException e) {
            fail("The following error occurred: " + e.getMessage());
        }
    }

    @Disabled
    void ignore_testParsing() {
        var validUrl = "http://www.percussion.com";
        try {
            var importContext = new PSSiteImportCtx();
            importContext.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
            importContext.setSiteUrl(validUrl);
            importContext.setUserAgent(USER_AGENT);

            var pageContent = PSSiteImporter.getPageContentFromSite(importContext);
            assertNotNull(pageContent.getTitle());
            assertNotNull(pageContent.getHeadContent());
            assertNotEquals("", pageContent.getHeadContent());
            assertNotNull(pageContent.getBodyContent());
            assertNotEquals("", pageContent.getBodyContent());
        } catch (RuntimeException e) {
            fail("The document couldn't be parsed: " + e.getMessage());
        } catch (IOException e) {
            fail("Couldn't connect to existing URL.");
        }
    }

    @Disabled
    void ignore_testGetRedirectedUrl_302Response() {
        try {
            var url = "http://www.firefox.com";
            if (!isHostReachable(url)) {
                return;
            }
            var logger = new PSSiteImportLogger(PSLogObjectType.TEMPLATE);
            var redirectedUrl = PSSiteImporter.getRedirectedUrl(url, logger, USER_AGENT);
            assertRedirection(url, redirectedUrl, logger);
        } catch (IOException e) {
            fail("No exception should have been thrown.");
        }
    }

    @Disabled
    void ignore_testGetRedirectedUrl_301Response() {
        try {
            var url = "http://firefox.com";
            if (!isHostReachable(url)) {
                return;
            }
            var logger = new PSSiteImportLogger(PSLogObjectType.TEMPLATE);
            var redirectedUrl = PSSiteImporter.getRedirectedUrl(url, logger, USER_AGENT);
            assertRedirection(url, redirectedUrl, logger);
        } catch (IOException e) {
            fail("No exception should have been thrown.");
        }
    }

    @Disabled
    void ignore_testGetRedirectedUrl_notRedirected() {
        try {
            var url = "http://www.percussion.com";
            if (!isHostReachable(url)) {
                return;
            }
            var logger = new PSSiteImportLogger(PSLogObjectType.TEMPLATE);
            var redirectedUrl = PSSiteImporter.getRedirectedUrl(url, logger, USER_AGENT);
            assertTrue(equalsIgnoreCase(url, redirectedUrl),
                    "The original site should not have been redirected, but was redirected from '" + url + "' to '" + redirectedUrl + "'.");
