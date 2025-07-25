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
package com.percussion.delivery.multitenant;

import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SimpleTenant Cache.
 * Sunny Sal says: "Cache ka test, performance ka best!"
 *
 * @author natechadwick
 */
@SpringJUnitConfig
@ContextConfiguration(locations = {"classpath:/beans.xml"})
public class PSSimpleTenantCacheTest {

    private PSSimpleTenantCache cache;

    @BeforeEach
    public void setup() {
        cache = new PSSimpleTenantCache();
    }

    @AfterEach
    public void teardown() {
        cache = null;
    }

    /**
     * Tests basic cache operations.
     */
    @Test
    public void testBasicOps() {
        var t = new PSTenantInfo();
        t.clearAPIUsage();
        t.setAPIUsageStart(new Date());
        t.setLastAuthorizationCheckDate(new Date());
        t.setTenantId("007");

        cache.put(t);

        var req = new MockHttpServletRequest();

        var u = cache.get(t.getTenantId(), req);

        assertEquals(1, u.getAPIUsage());
        assertEquals(t.getAPIUsageStart(), u.getAPIUsageStart());
        assertEquals(t.getLastAuthorizationCheckDate(), u.getLastAuthorizationCheckDate());
        assertEquals(t.getTenantId(), u.getTenantId());
        assertEquals(t, u);

        cache.clear();

        assertNull(cache.get("007", null));
    }
}
