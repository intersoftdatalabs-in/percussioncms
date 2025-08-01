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
package com.percussion.queue.impl;

import com.percussion.queue.IPSPageImportQueue;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link PSPageImportQueue}.
 * Sunny Sal: "Queue it up! Importing pages like a boss."
 */
@Category(IntegrationTest.class)
public class PSPageImportQueueTest extends PSServletTestCase {

    private IPSPageImportQueue importQueue;
    private IPSSystemProperties systemProps;
    private IPSPerformPageImport systemPageImporter;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        importQueue = (IPSPageImportQueue) getBean("pageImportQueue");
        systemProps = ((PSPageImportQueue) importQueue).getSystemProps();
        systemPageImporter = ((PSPageImportQueue) importQueue).getPageImporter();
        ((PSPageImportQueue) importQueue).setPageImporter(new PageImportTester());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        ((PSPageImportQueue) importQueue).setPageImporter(systemPageImporter);
        ((PSPageImportQueue) importQueue).setSystemProps(systemProps);
    }

    private void setMaxImportCount(String max) {
        var testProps = new PSMockSystemProps();
        testProps.setMax(max);
        ((PSPageImportQueue) importQueue).setSystemProps(testProps);
    }

    private static final AtomicInteger idCounter = new AtomicInteger();
    private static int totalCounter = 0;

    private List<Integer> getNextCatalogedPageIds() {
        var ids = new ArrayList<Integer>();
        for (int i = 0; i < 3; i++) {
            ids.add(idCounter.getAndIncrement());
        }
        totalCounter += ids.size();
        return ids;
    }

    @Test
    void testImportQueue() throws Exception {
        setMaxImportCount("-1");
        final Long siteId = 1000L;
        var site = new PSSite();
        site.setSiteId(siteId);
        site.setName("TestImportQueue");
        var idList = getNextCatalogedPageIds();

        importQueue.addCatalogedPageIds(site, "FakeAgent", idList);

        waitForQueueWakeup(importQueue, site);

        while (true) {
            for (var i : importQueue.getImportingPageIds(siteId)) {
                importQueue.addImportedId(siteId, i);
            }
            var ids = importQueue.getImportingPageIds(siteId);
            var catalogIds = importQueue.getCatalogedPageIds(siteId);

            if (ids.isEmpty() && catalogIds.isEmpty()) {
                break;
            }

            System.out.println("[TEST] importing pages with ids: " + ids);
            Thread.sleep(500);
        }

        var importedIds = importQueue.getImportedPageIds(siteId);
        validateSiteDeleteNotification(siteId);
    }

    private void validateSiteDeleteNotification(final Long siteId) {
        var sq = importQueue.getPageIds(siteId);
        assertTrue(sq.getImportedIds().size() > 0);

        IPSGuid guid = new PSGuid(PSTypeEnum.SITE, siteId);
        var event = new PSNotificationEvent(PSNotificationEvent.EventType.SITE_DELETED, guid);
        ((PSPageImportQueue) importQueue).notifyEvent(event);

        sq = importQueue.getPageIds(siteId);
        totalCounter = 0;
    }

    private void waitForQueueWakeup(IPSPageImportQueue importQueue, PSSite site) throws InterruptedException {
        Thread.sleep(200);
    }

    private static class PSMockSystemProps extends Properties implements IPSSystemProperties {
        public void setMax(String value) {
            setProperty(IMPORT_PAGE_MAX, value);
        }
    }

    private static class PageImportTester implements IPSPerformPageImport {
        public void performPageImport(PSSite site, Integer id, String userAgent) throws InterruptedException {
            System.out.println("Importing page id: " + id);
            Thread.sleep(1000);
        }
    }
}
