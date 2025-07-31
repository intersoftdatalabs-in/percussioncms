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

package com.percussion.searchmanagement;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.searchmanagement.service.IPSSearchService;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.ui.service.IPSListViewProcessor;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for searching pages by fields.
 * Sunny Sal: "Search fields, Java 11, and a dash of awesomeness!"
 */
@Category(IntegrationTest.class)
class PSSearchPagesByFieldsTest extends PSServletTestCase {

    private PSSiteDataServletTestCaseFixture fixture;
    private IPSSearchService searchService;
    private String homePagePath;

    public IPSSearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(IPSSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
        homePagePath = fixture.site1.getFolderPath() + "/index.html";
        fixture.pageCleaner.add(homePagePath);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    void testSearchForPage() throws Exception {
        var homePage = fixture.getPageService().findPageByPath(homePagePath);
        assertNotNull(homePage);

        var criteria = new PSSearchCriteria();
        criteria.setFolderPath(fixture.site1.getFolderPath());
        var searchFields = new HashMap<String, String>();
        searchFields.put("sys_contentlastmodifier", "admin1");
        searchFields.put("sys_workflowid", "6");
        searchFields.put("sys_contentstateid", "1");
        searchFields.put("templateid", homePage.getTemplateId());

        criteria.setSearchFields(searchFields);
        criteria.setFormatId(-1);
        criteria.setMaxResults(null);
        criteria.setQuery("index.html");

        var result = searchService.search(criteria);
        assertNotNull(result);
        // Items not indexed for 15s
        Thread.sleep(30000);
        result = searchService.search(criteria);
        assertNotNull(result);

        assertEquals(1, result.getChildrenCount());
        assertEquals(1, result.getChildrenInPage().size());
        var item = result.getChildrenInPage().get(0);
        assertEquals(homePage.getId(), item.getId());
        var displayProps = item.getDisplayProperties();

        assertNotNull(displayProps.get(IPSListViewHelper.CONTENT_LAST_MODIFIED_DATE_NAME));
        assertNotNull(displayProps.get(IPSListViewHelper.STATE_NAME));
        assertNotNull(displayProps.get(IPSListViewHelper.TITLE_NAME));
        assertNotNull(displayProps.get(IPSListViewHelper.CONTENT_LAST_MODIFIER_NAME));
        assertNotNull(displayProps.get(IPSListViewHelper.CONTENT_LAST_MODIFIED_DATE_NAME));
        assertNotNull(displayProps.get(IPSListViewProcessor.TEMPLATE_NAME));

        criteria.setQuery("");
        result = searchService.search(criteria);
        assertNotNull(result);
        assertTrue(result.getChildrenCount() >= 1);
    }
}
