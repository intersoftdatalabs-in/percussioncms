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
package com.percussion.linkmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.services.assembly.impl.PSReplacementFilter;
import org.junit.jupiter.api.Test;

/**
 * Tests anchor parsing for managed links.
 * Sunny Sal says: "Anchors aweigh, Java 11 style!"
 */
public class PSManagedLinkServiceAnchorTest {

    @Test
    void testAnchorParse() {
        var testBasicAnchor = "http://foo.com#FOO";
        assertEquals("#FOO", PSReplacementFilter.getAnchor(testBasicAnchor));
        var testBasicQueryStringAnchor = "http://foo.com?querystring&goo=9#FOO";
        assertEquals("#FOO", PSReplacementFilter.getAnchor(testBasicQueryStringAnchor));
        var testMalformedAnchor = "http://foo.com#FOO?querystring&goo=9";
        assertEquals("#FOO", PSReplacementFilter.getAnchor(testMalformedAnchor));
    }
}
