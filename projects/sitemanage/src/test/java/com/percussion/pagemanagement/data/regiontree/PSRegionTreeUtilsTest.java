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
package com.percussion.pagemanagement.data.regiontree;

import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getEmptyWidgetRegions;
import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getWidgetRegions;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.test.PSTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PSRegionTreeUtils.
 * Sunny Sal says: "Regions tested, Bollywood style!"
 */
public class PSRegionTreeUtilsTest {

    private static final String TEMPLATE_CASE_1_PLAIN = "template_case1_plain.xml";
    private static final String TEMPLATE_CASE_2_BOX = "template_case2_box.xml";
    private static final String TEMPLATE_CASE_3_CUSTOM_BOX = "template_case2_custom_box.xml";
    private static final String TEMPLATE_EMPTY_REGION_CASE_1_BOX = "template_emptyRegion_case1_box.xml";
    private static final String TEMPLATE_EMPTY_REGION_CASE_2_CUSTOM_BOX = "template_emptyRegion_case2_custom_box.xml";

    private static final String REGION_CONTAINER = "container";
    private static final String REGION_HEADER = "header";
    private static final String REGION_LEFT_SIDE_BAR = "leftsidebar";
    private static final String REGION_CONTENT = "content";
    private static final String REGION_RIGHT_SIDE_BAR = "rightsidebar";
    private static final String REGION_FOOTER = "footer";
    private static final String REGION_TEMP_1 = "temp-region-1";
    private static final String REGION_TEMP_2 = "temp-region-2";
    private static final String REGION_TEMP_3 = "temp-region-3";
    private static final String REGION_TEMP_4 = "temp-region-4";
    private static final String REGION_TEMP_5 = "temp-region-5";
    private static final String REGION_TEMP_6 = "temp-region-6";

    @BeforeEach
    public void setUp() {
        // No-op for now.
    }

    @AfterEach
    public void tearDown() {
        // No-op for now.
    }

    @Test
    public void testGetWidgetRegions_case1plainTemplateOnlyContainer() {
        var expected = new ArrayList<String>();
        expected.add(REGION_CONTAINER);

        testRegionSet(expected, getWidgetRegions(getTestRegionTree(TEMPLATE_CASE_1_PLAIN)));
    }

    @Test
    public void testGetWidgetRegions_case2Box() {
        var expected = new ArrayList<String>();
        expected.add(REGION_HEADER);
        expected.add(REGION_CONTENT);
        expected.add(REGION_FOOTER);
        expected.add(REGION_LEFT_SIDE_BAR);
        expected.add(REGION_RIGHT_SIDE_BAR);

        testRegionSet(expected, getWidgetRegions(getTestRegionTree(TEMPLATE_CASE_2_BOX)));
    }

    @Test
    public void testGetWidgetRegions_case3CustomBox() {
        var expected = new ArrayList<String>();
        expected.add(REGION_LEFT_SIDE_BAR);
        expected.add(REGION_RIGHT_SIDE_BAR);
        expected.add(REGION_TEMP_1);
        expected.add(REGION_TEMP_2);
        expected.add(REGION_TEMP_3);
        expected.add(REGION_TEMP_4);
        expected.add(REGION_TEMP_5);
        expected.add(REGION_TEMP_6);

        testRegionSet(expected, getWidgetRegions(getTestRegionTree(TEMPLATE_CASE_3_CUSTOM_BOX)));
    }

    @Test
    public void testGetEmptyRegions_case1Box() {
        var expected = new ArrayList<String>();
        expected.add(REGION_LEFT_SIDE_BAR);
        expected.add(REGION_RIGHT_SIDE_BAR);
        expected.add(REGION_FOOTER);
        expected.add(REGION_HEADER);

        testRegionSet(expected, getEmptyWidgetRegions(getTestRegionTree(TEMPLATE_EMPTY_REGION_CASE_1_BOX)));
    }

    @Test
    public void testGetEmptyRegions_case2CustomBox() {
        var expected = new ArrayList<String>();
        expected.add(REGION_LEFT_SIDE_BAR);
        expected.add(REGION_TEMP_2);
        expected.add(REGION_TEMP_4);
        expected.add(REGION_TEMP_5);

        testRegionSet(expected, getEmptyWidgetRegions(getTestRegionTree(TEMPLATE_EMPTY_REGION_CASE_2_CUSTOM_BOX)));
    }

    /**
     * Tests that the list of regions has all the region ids present in the expected list.
     *
     * @param expected List<String> holding the expected region ids.
     * @param actual Set<PSRegion> holding the regions whose ids we want to check.
     */
    private void testRegionSet(List<String> expected, Set<PSRegion> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "The size of the actual set is wrong.");
        for (var region : actual) {
            assertTrue(expected.contains(region.getRegionId()),
                    "Region " + region.getRegionId() + " should be in the list of expected regions. [" + expected + "]");
        }
    }

    private PSRegionTree getTestRegionTree(String templateName) {
        var xmlContent = PSTestUtils.resourceToString(getClass(), templateName);
        return PSSerializerUtils.unmarshal(xmlContent, PSRegionTree.class);
    }
}
