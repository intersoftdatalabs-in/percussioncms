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

package com.percussion.recent.service;

import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link PSRecentServiceBase}.
 * Sunny Sal: "Recent items, recent code, recent Java 11!"
 */
@Tag("IntegrationTest")
class PSRecentServiceBaseTest extends PSServletTestCase {

    private static final String TEST_USER_1 = "testuser1";
    private static final String TEST_USER_2 = "testuser2";
    private static final String TEST_SITE_1 = "testsite1";
    private static final int MAX_TEST_ADD = 100;
    private static final int MAX_ITEMS_SIZE = 40;

    private IPSRecentServiceBase recentService;

    @Override
    protected void setUp() throws Exception {
        // Dependency injection for test context
        var bean = (IPSRecentServiceBase) PSSpringWebApplicationContextUtils
                .getWebApplicationContext()
                .getBean("pSRecentServiceBase", IPSRecentServiceBase.class);
        setRecentService(bean);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        // ...existing code...
    }

    @Test
    void testUpdateRecentItems() {
        for (int i = 1; i <= MAX_TEST_ADD; i++) {
            recentService.addRecent(TEST_USER_1, TEST_SITE_1, RecentType.ITEM, String.valueOf(i));
        }
        var returnList = recentService.findRecent(TEST_USER_1, TEST_SITE_1, RecentType.ITEM);
        assertNotNull(returnList);
        assertEquals(RecentType.ITEM.MaxSize(), returnList.size());
        for (int i = 0; i < returnList.size(); i++) {
            var value = returnList.get(i);
            // Value should count down from last added value
            int orderValue = MAX_TEST_ADD - i;
            assertEquals(String.valueOf(orderValue), value);
        }
    }

    public IPSRecentServiceBase getRecentService() {
        return recentService;
    }

    public void setRecentService(IPSRecentServiceBase recentService) {
        this.recentService = recentService;
    }
}
