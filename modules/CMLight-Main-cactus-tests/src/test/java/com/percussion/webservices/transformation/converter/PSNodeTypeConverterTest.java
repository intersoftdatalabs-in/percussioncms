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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.ui.data.PSHierarchyNode;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.ui.data.NodeType;
import org.junit.experimental.categories.Category;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSNodeTypeConverter} class.
 */
@Category(IntegrationTest.class)
public class PSNodeTypeConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // Create the source object
        var source = PSHierarchyNode.NodeType.PLACEHOLDER;

        var target = (PSHierarchyNode.NodeType) roundTripConversion(
                PSHierarchyNode.NodeType.class,
                NodeType.class,
                source);

        // Verify the round-trip object is equal to the source object
        assertEquals(source, target);
    }
}
