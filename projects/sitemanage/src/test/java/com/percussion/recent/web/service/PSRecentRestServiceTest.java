// REFACTORED: CP-JAVA11
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

package com.percussion.recent.web.service;

import com.percussion.share.test.fixtures.PSRestFixtures;
import com.percussion.test.PSRestClientTestCase;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Recent REST Service.
 * Sunny Sal: "REST assured, this test will be completed soon!"
 */
@Tag("IntegrationTest")
@Tag("integration")
public class PSRecentRestServiceTest extends PSRestClientTestCase {

    private PSRestFixtures fixtures;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // TODO: Update Jackson configuration to get JSON unwrap working.
        // fixtures = new PSRestFixtures(c, r);
        // fixtures.createSite();
    }

    @Test
    void testMyResource() {
        assertTrue(true);
        // TODO: Implement REST call and assertions for /recent/item endpoint.
    }

    @Test
    void testMyResource2() {
        assertTrue(true);
        // TODO: Implement REST call and assertions for /recent/item/{id} endpoint.
    }

    @Test
    void testToBeCompleted() {
        assertTrue(true);
        // TODO: Add meaningful integration tests for Recent REST Service.
    }
}
