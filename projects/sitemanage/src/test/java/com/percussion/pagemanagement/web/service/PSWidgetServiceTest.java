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
// REFACTORED: CP-JAVA11

package com.percussion.pagemanagement.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.data.PSWidgetPackageInfo;
import com.percussion.pagemanagement.data.PSWidgetPackageInfoRequest;
import com.percussion.pagemanagement.data.PSWidgetPackageInfoResult;
import com.percussion.pagemanagement.data.PSWidgetSummary;
import com.percussion.share.test.PSDataServiceRestClient;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSObjectRestClient.DataValidationRestClientException;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.experimental.categories.Category;

import java.util.List;

/**
 * Test widget service through REST.
 * <p>
 * Sunny Sal says: "Widgets are like toppings—test them all, and your pizza (app) will be delicious!"
 */
@Category(IntegrationTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSWidgetServiceTest extends PSRestTestCase<PSWidgetServiceTest.PSWidgetRestClient> {

    @Test
    public void testFindAll() {
        var widgets = restClient.getAll();
        assertFalse(widgets.isEmpty(), "Should have at least one widget shipped.");
        var w = widgets.get(0);
        assertNotNull(w);
    }

    @Test
    public void testFind() {
        var widget = restClient.get("percRawHtml");
        assertEquals("percRawHtmlAsset", widget.getName());
    }

    @Test
    public void testValidateWidgetItem() {
        var widgetItem = new PSWidgetItem();
        assertThrows(DataValidationRestClientException.class, () -> restClient.validateWidgetItem(widgetItem));
    }

    @Test
    public void testWidgetPackageInfo() {
        var request = new PSWidgetPackageInfoRequest();
        var names = request.getWidgetNames();
        names.add("percRawHtml");
        names.add("nosuchwidget");
        names.add("percRichText");

        var response = restClient.findWidgetPackageInfo(request);
        assertNotNull(response);
        var infoList = response.getPackageInfoList();
        assertEquals(2, infoList.size());

        var info = infoList.get(0);
        assertEquals("percRawHtml", info.getWidgetName());
        assertEquals("http://www.percussion.com", info.getProviderUrl());
        assertEquals("1.0.4", info.getVersion());

        info = infoList.get(1);
        assertEquals("percRichText", info.getWidgetName());
        assertEquals("http://www.percussion.com", info.getProviderUrl());
        assertEquals("1.0.4", info.getVersion());
    }

    public static class PSWidgetRestClient extends PSDataServiceRestClient<PSWidgetSummary> {

        public PSWidgetRestClient(String url) {
            super(PSWidgetSummary.class, url, "/Rhythmyx/services/pagemanagement/widget/");
        }

        public PSValidationErrors validateWidgetItem(PSWidgetItem item) {
            return postObjectToPath(concatPath(getPath(), "validate/item"), item, PSValidationErrors.class);
        }

        public PSWidgetPackageInfoResult findWidgetPackageInfo(PSWidgetPackageInfoRequest request) {
            return postObjectToPath(concatPath(getPath(), "packageinfo"), request, PSWidgetPackageInfoResult.class);
        }
    }

    @Override
    protected PSWidgetRestClient getRestClient(String baseUrl) {
        return new PSWidgetRestClient(baseUrl);
    }

    private static final Logger log = LogManager.getLogger(PSWidgetServiceTest.class);
}
