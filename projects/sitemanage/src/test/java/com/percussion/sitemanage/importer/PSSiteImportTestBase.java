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

import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pagemanagement.service.impl.PSPageCatalogService;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.queue.impl.IPSPerformPageImport;
import com.percussion.queue.impl.PSPageImportQueue;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

import java.util.Properties;

/**
 * Base class for site import integration tests.
 * Handles setup/teardown of page import queue and catalog service.
 * @author Percussion CMS Team, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSSiteImportTestBase extends PSServletTestCase {

    protected IPSPageImportQueue importQueue;
    protected IPSSystemProperties systemProps;
    protected IPSPerformPageImport systemPageImporter;
    protected IPSPageCatalogService pageCatalogService;
    protected PSMockSystemProps testProps = new PSMockSystemProps();

    protected void setUp() throws Exception {
        super.setUp();
        importQueue = (IPSPageImportQueue) getBean("pageImportQueue");
        systemProps = ((PSPageImportQueue) importQueue).getSystemProps();
        pageCatalogService = (IPSPageCatalogService) getBean("pageCatalogService");

        ((PSPageImportQueue) importQueue).setSystemProps(testProps);
        ((PSPageCatalogService) pageCatalogService).setSystemProps(testProps);

        testProps.setCatalogMax("0");
        testProps.setImportMax("0");
    }

    @Override
    protected void tearDown() throws Exception {
        ((PSPageImportQueue) importQueue).setPageImporter(systemPageImporter);
        ((PSPageImportQueue) importQueue).setSystemProps(systemProps);
        ((PSPageCatalogService) pageCatalogService).setSystemProps(systemProps);
    }

    protected static class PSMockSystemProps extends Properties implements IPSSystemProperties {
        public void setCatalogMax(String value) {
            setProperty(CATALOG_PAGE_MAX, value);
        }
        public void setImportMax(String value) {
            setProperty(IMPORT_PAGE_MAX, value);
        }
    }
}
