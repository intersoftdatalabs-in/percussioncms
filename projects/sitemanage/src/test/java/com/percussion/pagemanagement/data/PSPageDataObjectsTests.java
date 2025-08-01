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
package com.percussion.pagemanagement.data;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.percussion.pagemanagement.data.PSWidgetProperties.PSWidgetProperty;
import com.percussion.share.data.PSDataObjectTestCase;
import com.percussion.share.test.PSDataObjectTestUtils;

public class PSPageDataObjectsTests {

    @Test
    public void test() {
        // Intentionally left blank for structure validation.
    }

    public static class PSRegionCodeTest extends PSDataObjectTestCase<PSRegionCode> {
        @Override
        public PSRegionCode getObject() {
            var code = new PSRegionCode();
            code.setTemplateCode("#region('' '' '' '' '')");
            return code;
        }
    }

    public static class PSRegionTest extends PSDataObjectTestCase<PSRegion> {
        @Override
        public PSRegion getObject() {
            var region = new PSRegion();
            var subRegion = new PSRegion();
            subRegion.setRegionId("sub");
            region.setRegionId("adam");
            subRegion.setChildren(asList(new PSRegionCodeTest().getObject()));
            region.setChildren(asList(subRegion));
            return region;
        }
    }

    public static class PSWidgetItemTest extends PSDataObjectTestCase<PSWidgetItem> {
        @Override
        public PSWidgetItem getObject() {
            var wi = new PSWidgetItem();
            wi.setDefinitionId("test");
            wi.setName("test");
            wi.setId("test");
            Map<String, Object> props = new HashMap<>();
            props.put("list", asList("a", "b", "c"));
            props.put("number", "42");
            wi.setProperties(props);
            return wi;
        }
    }

    public static class PSWidgetPropertyTest extends PSDataObjectTestCase<PSWidgetProperty> {
        @Override
        public PSWidgetProperty getObject() {
            var wp = new PSWidgetProperty();
            wp.setName("jose");
            wp.setValue("the best");
            return wp;
        }
    }

    public static class PSRegionWidgetsTest extends PSDataObjectTestCase<PSRegionWidgets> {
        @Override
        public PSRegionWidgets getObject() {
            var rw = new PSRegionWidgets();
            rw.setRegionId("test");
            rw.setWidgetItems(asList(new PSWidgetItemTest().getObject()));
            return rw;
        }
    }

    public static class PSPageTest extends PSDataObjectTestCase<PSPage> {
        @Override
        public PSPage getObject() {
            var page = new PSPage();
            page.setRegionBranches(new PSRegionBranchesTest().getObject());
            return page;
        }

        @Test
        public void testLog() throws Exception {
            log.debug(PSDataObjectTestUtils.doXmlSerialization(object).actualXml);
        }
    }

    public static class PSPageFullTest extends PSDataObjectTestCase<PSPage> {
        @Override
        public PSPage getObject() {
            var page = new PSPage();
            PSDataObjectTestUtils.fillObject(page);
            page.setTemplateId("blah");
            return page;
        }

        @Test
        public void testLog() throws Exception {
            log.debug(PSDataObjectTestUtils.doXmlSerialization(object).actualXml);
        }
    }

    public static class PSTemplateTest extends PSDataObjectTestCase<PSTemplate> {
        @Override
        public PSTemplate getObject() {
            var template = new PSTemplate();
            template.setRegionTree(new PSRegionTreeTest().getObject());
            return template;
        }

        @Test
        public void testLog() throws Exception {
            log.debug(PSDataObjectTestUtils.doXmlSerialization(object).actualXml);
        }
    }

    public static class PSRegionTreeTest extends PSDataObjectTestCase<PSRegionTree> {
        @Override
        public PSRegionTree getObject() {
            var tree = new PSRegionTree();
            tree.setRootRegion(new PSRegionTest().getObject());
            tree.setRegionWidgets("rid", asList(new PSWidgetItemTest().getObject()));
            return tree;
        }
    }

    public static class PSRegionBranchesTest extends PSDataObjectTestCase<PSRegionBranches> {
        @Override
        public PSRegionBranches getObject() {
            var branches = new PSRegionBranches();
            branches.setRegions(asList(new PSRegionTest().getObject()));
            branches.setRegionWidgets("rid", asList(new PSWidgetItemTest().getObject()));
            return branches;
        }

        @Test
        public void testRegionWidgetsSet() throws Exception {
            var copy = getCopy();
            var os = object.getRegionWidgetAssociations();
            var cs = copy.getRegionWidgetAssociations();
            assertEquals(os.size(), cs.size());
            var o = os.iterator().next();
            var c = cs.iterator().next();
            assertEquals(o, c);
            assertEquals(c, o);
            assertEquals("Widget associations should be equal 1", cs, os);
            assertEquals("Widget associations should be equal 2", os, cs);
        }
    }

    /**
     * The log instance to use for this class, never null.
     */
    private static final Logger log = LogManager.getLogger(PSPageDataObjectsTests.class);
}
