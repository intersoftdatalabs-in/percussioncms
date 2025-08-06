/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.sitemanage.task.impl;

import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.data.PSSite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for canonical URL generation in site map generator.
 * // REFACTORED: CP-JAVA11
 */
public class PSSiteMapGeneratorTaskTests {

    @Test
    public void testCanonicalUrl() {
        var testSite = new PSSite();
        testSite.setDefaultDocument("index.html");
        testSite.setCanonical(true);
        testSite.setCanonicalDist("sections");

        var result = PSSiteMapGeneratorTask.getCanonicalLocation(testSite, "/section1/index.html");
        assertEquals("/section1/", result);

        testSite.setCanonical(false);
        result = PSSiteMapGeneratorTask.getCanonicalLocation(testSite, "/section1/index.html");
        assertEquals("/section1/index.html", result);

        testSite.setCanonical(true);
        testSite.setCanonicalDist("pages");
        result = PSSiteMapGeneratorTask.getCanonicalLocation(testSite, "/section1/index.html");
        assertEquals("/section1/index.html", result);

        testSite.setCanonical(true);
        testSite.setCanonicalDist("sections");
        testSite.setDefaultDocument("index");
        result = PSSiteMapGeneratorTask.getCanonicalLocation(testSite, "/section1/index.html");
        assertEquals("/section1/index.html", result);

        result = PSSiteMapGeneratorTask.getCanonicalLocation(testSite, "/section1/index");
        assertEquals("/section1/", result);
    }
}
