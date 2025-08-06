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

package com.percussion.category;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for PSCategory.
 */
public class PSCategoryTests {

    @TempDir
    java.nio.file.Path tempFolder;

    public PSCategoryTests() {}

    @Test
    @Disabled("TODO: Fix me. This test will always fail as written.")
    public void testToJson() {
        var cat = new PSCategory();
        cat.setTitle("TEST");
        cat.setAllowedSites("TEST");

        var topNodes = new ArrayList<PSCategoryNode>();
        var childNodes = new ArrayList<PSCategoryNode>();

        var childNode = new PSCategoryNode();
        childNode.setId("2");
        childNode.setCreatedBy("L2TEST_CREATED_BY");
        childNode.setSelectable(true);
        childNode.setChildNodes(null);
        childNode.setCreationDate(LocalDateTime.now());
        childNode.setTitle("L2TEST_TITLE");
        childNode.setDeleted(false);
        childNode.setOldId(null);
        childNode.setAllowedSites(null);
        childNode.setInitialViewCollapsed(true);
        childNode.setLastModifiedBy("L2TEST_MODIFIED_BY");
        childNode.setLastModifiedDate(LocalDateTime.now());
        childNodes.add(childNode);

        var node = new PSCategoryNode();
        node.setId("1");
        node.setCreatedBy("L1TEST_CREATED_BY");
        node.setSelectable(true);
        node.setChildNodes(childNodes);
        node.setCreationDate(LocalDateTime.now());
        node.setTitle("L1TEST_TITLE");
        node.setDeleted(false);
        node.setOldId(null);
        node.setAllowedSites(null);
        node.setInitialViewCollapsed(true);
        node.setLastModifiedBy("L1TEST_MODIFIED_BY");
        node.setLastModifiedDate(LocalDateTime.now());
        topNodes.add(node);

        cat.setTopLevelNodes(topNodes);

        assertEquals("{}", cat.toJSON());
    }
}
