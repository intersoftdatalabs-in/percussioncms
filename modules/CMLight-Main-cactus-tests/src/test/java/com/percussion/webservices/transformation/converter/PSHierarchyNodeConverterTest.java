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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.ui.data.PSHierarchyNode;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSHierarchyNodeConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSHierarchyNodeConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // Create source object
        var source = new PSHierarchyNode("name", new PSGuid(
                PSTypeEnum.HIERARCHY_NODE, 1001), PSHierarchyNode.NodeType.FOLDER);

        // Convert
        var target = (PSHierarchyNode) roundTripConversion(
                PSHierarchyNode.class,
                com.percussion.webservices.ui.data.PSHierarchyNode.class, source);

        assertEquals(source, target);

        // Create source object with properties
        source = new PSHierarchyNode("name", new PSGuid(
                PSTypeEnum.HIERARCHY_NODE, 1001), PSHierarchyNode.NodeType.FOLDER);
        source.addProperty("property_1", "value_1");
        source.addProperty("property_2", " ");
        source.addProperty("property_3", null);

        // Convert
        target = (PSHierarchyNode) roundTripConversion(
                PSHierarchyNode.class,
                com.percussion.webservices.ui.data.PSHierarchyNode.class, source);

        assertEquals(source, target);

        // Create the source array
        var sourceArray = new PSHierarchyNode[]{source};

        var targetArray = (PSHierarchyNode[]) roundTripConversion(
                PSHierarchyNode[].class,
                com.percussion.webservices.ui.data.PSHierarchyNode[].class,
                sourceArray);

        // Verify the round-trip array is equal to the source array
        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }
}
