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

package com.percussion.pagemanagement.data;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.parser.PSParsedRegionTree;
import com.percussion.pagemanagement.parser.PSTemplateRegionParser;
import com.percussion.share.test.PSTestUtils;

import org.junit.jupiter.api.Test;

import java.util.*;

public class PSRegionTreeTest {

    @Test
    public void testInvalidTree() {
        var root = createInvalidRegionRoot();

        var tree = new PSRegionTree();
        tree.setRootRegion(root);

        assertThrows(IllegalStateException.class, tree::getDescendentRegions);
    }

    private PSRegion createInvalidRegionRoot() {
        var children = new ArrayList<PSRegionNode>();

        var region1 = new PSRegion();
        region1.setRegionId("region-1");
        children.add(region1);

        var region2 = new PSRegion();
        region2.setRegionId("region-2");
        children.add(region2);

        var root = new PSRegion();
        root.setRegionId("percRoot");
        root.setChildren(children);

        return root;
    }

    @Test
    public void testEmptyTree() {
        var tree = new PSRegionTree();

        assertNull(tree.getRootRegion());
        assertNotNull(tree.getDescendentRegions());
        assertTrue(tree.getDescendentRegions().isEmpty());

        tree.setRootRegion(new PSRegion());

        assertNotNull(tree.getRootRegion());
        assertNotNull(tree.getDescendentRegions());
        assertTrue(tree.getDescendentRegions().isEmpty());
    }

    @Test
    public void testNonEmptyTree() {
        var tree = loadRegionTree();
        var root = tree.getRootRegion();

        var names = new ArrayList<String>();
        names.add("percRoot");
        names.addAll(Arrays.asList(nameChildren));
        var regionNames = getRegionIds(root.getAllRegions());
        assertEquals(names, regionNames);

        names = Arrays.asList(nameChildren);
        regionNames = getRegionIds(tree.getDescendentRegions());
        assertEquals(names, regionNames);
    }

    private final String[] nameChildren = new String[]{"container", "header", "middle", "leftsidebar", "content", "rightsidebar", "footer"};

    public static List<String> getRegionIds(List<PSRegion> regions) {
        var result = new ArrayList<String>();
        for (var r : regions) {
            result.add(r.getRegionId());
        }
        return result;
    }

    public static PSRegionTree loadRegionTree() {
        var tree = new PSRegionTree();
        var markup = getMarkupText("PlainTemplateMarkup.vm");
        var parser = createRegionParser();
        PSParsedRegionTree<PSRegion, PSRegionCode> pt = parser.parse(markup);
        tree.setRootRegion(pt.getRootNode());
        return tree;
    }

    private static PSTemplateRegionParser createRegionParser() {
        Map<String, PSRegion> regions = new HashMap<>();
        return new PSTemplateRegionParser(regions);
    }

    private static String getMarkupText(String name) {
        return PSTestUtils.resourceToString(PSRegionTreeTest.class, name);
    }
}
