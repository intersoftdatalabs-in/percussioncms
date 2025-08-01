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
package com.percussion.pagemanagement.assembler;

import static com.percussion.share.test.PSMatchers.validUrl;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.data.PSResourceInstance;
import com.percussion.pagemanagement.data.PSResourceLinkAndLocation;
import com.percussion.pagemanagement.service.impl.PSLinkableAsset;
import com.percussion.share.data.IPSLinkableContentItem;
import com.percussion.sitemanage.data.PSSiteSummary;
import org.hamcrest.core.CombinableMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for resource link and location utilities.
 * Sunny Sal says: "URLs escaped, analytics appended, Bollywood style!"
 */
public class PSResourceLinkAndLocationUtilsTest {

    private PSResourceInstance resource;
    private PSSiteSummary contextSite = new PSSiteSummary();
    private PSSiteSummary itemSite = new PSSiteSummary();
    private String locationFolderPath = "/space blah";
    private final String ANALYTICS_ID = "analyticsId";

    @BeforeEach
    public void setup() {
        contextSite.setBaseUrl("http://Context.com/");
        itemSite.setBaseUrl("http://Item.com/MySite");
        itemSite.setId("1");
        contextSite.setId("2");

        resource = new PSResourceInstance();
        resource.setSite(itemSite);
        resource.setLinkContext(new PSRenderLinkContext() {
            @Override
            public Mode getMode() {
                return Mode.PUBLISH;
            }

            @Override
            public PSSiteSummary getSite() {
                return contextSite;
            }
        });
        resource.setLocationFolderPath(locationFolderPath);
        assertThat(resource.isCrossSite(), is(true));
    }

    @Test
    public void testValidateSafePhysicalPathFailure() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                PSResourceLinkAndLocationUtils.validateAsPhysicalPath("/asdf?sadf/"));
    }

    @Test
    public void testValidateSafePhysicalPath() {
        PSResourceLinkAndLocationUtils.validateAsPhysicalPath("/asdf");
    }

    @Test
    public void testConcatPath() {
        var path = "/peter paul";
        var actual = PSResourceLinkAndLocationUtils.concatPath(path, "");
        assertEquals(path, actual, "path");

        actual = PSResourceLinkAndLocationUtils.concatPath("/peter paul/", "");
        assertEquals("/peter paul", actual, "path");

        actual = PSResourceLinkAndLocationUtils.concatPath("/peter paul/", "/blah/", "", "");
        assertEquals("/peter paul/blah", actual, "path");
    }

    @Test
    public void testEscapeUrlPath() {
        var path = "/peter paul/mary joseph/j.html";
        var escapedPath = PSResourceLinkAndLocationUtils.escapePathForUrl(path);
        assertEquals("/peter%20paul/mary%20joseph/j.html", escapedPath);
    }

    @Test
    public void testEscapeBackSlashShouldNotFail() {
        var path = "/stuff \\ crap";
        var escapedPath = PSResourceLinkAndLocationUtils.escapePathForUrl(path);
        assertEquals("/stuff%20%5C%20crap", escapedPath);
    }

    @Test
    public void testEscapeColonShouldNotFail() {
        var path = "/stuff : crap";
        var escapedPath = PSResourceLinkAndLocationUtils.escapePathForUrl(path);
        assertEquals("/stuff%20:%20crap", escapedPath);
    }

    @Test
    public void testEscapeQuestionMarkShouldNotFail() {
        var path = "/stuff ? crap";
        var escapedPath = PSResourceLinkAndLocationUtils.escapePathForUrl(path);
        assertEquals("/stuff%20%3F%20crap", escapedPath);
    }

    @Test
    public void testCreateDefaultLinkAndLocationForCrossSite() {
        var link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(resource, "crap.txt");
        assertThat(link.getResourceLocation().getFilePath(), is("/space blah/crap.txt"));
        assertThat(link.getRenderLink().getUrl(),
                CombinableMatcher.<String>both(validUrl()).and(equalTo("http://Item.com/MySite/space%20blah/crap.txt")));
    }

    @Test
    public void testCreateDefaultLinkAndLocation() {
        // Not a cross site link.
        contextSite = itemSite;
        var link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(resource, "crap.txt");
        assertThat(link.getResourceLocation().getFilePath(), is("/space blah/crap.txt"));
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt"));
    }

    @Test
    public void testAppendAnalyticsId() {
        contextSite = itemSite;
        var newResourceInstance = resource;
        var asset = new PSAsset();
        var fields = new HashMap<String, Object>();
        fields.put(ANALYTICS_ID, "?my=analytics id");
        asset.setFields(fields);
        newResourceInstance.setItem(new PSLinkableAsset(asset, "crap.txt"));

        var link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt?my=analytics+id"));

        fields.put(ANALYTICS_ID, "?mynewid");
        link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt?mynewid"));

        fields.put(ANALYTICS_ID, "?utm_source=help&utm_medium=web&utm_campaign=cm153newfeature&utm_term=clicktrack");
        link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt?utm_source=help&utm_medium=web&utm_campaign=cm153newfeature&utm_term=clicktrack"));

        // test to check if the analytics has no ? at the beginning of the string
        fields.put(ANALYTICS_ID, "utm_source=help&utm_medium=web&utm_campaign=cm153newfeature&utm_term=clicktrack");
        link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt?utm_source=help&utm_medium=web&utm_campaign=cm153newfeature&utm_term=clicktrack"));
    }

    @Test
    public void testBadAppendAnalyticsId() {
        contextSite = itemSite;
        var newResourceInstance = resource;
        var asset = new PSAsset();
        var fields = new HashMap<String, Object>();
        String myanalyticsId = "?";
        fields.put(ANALYTICS_ID, myanalyticsId);
        asset.setFields(fields);
        newResourceInstance.setItem(new PSLinkableAsset(asset, "crap.txt"));

        var link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt"));

        newResourceInstance.setItem(null);
        link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt"));

        // check for null analytics id to ensure publishing doesn't break
        fields.put(ANALYTICS_ID, null);
        link = PSResourceLinkAndLocationUtils.createLinkAndLocationForFileName(newResourceInstance, "crap.txt");
        assertThat(link.getRenderLink().getUrl(), is("/MySite/space%20blah/crap.txt"));
    }
}
