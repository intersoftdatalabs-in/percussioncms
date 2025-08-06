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

import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSItemSummaryConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSItemSummaryConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        var source = createItemSummary();

        var target = (PSItemSummary) roundTripConversion(
                PSItemSummary.class,
                com.percussion.webservices.content.PSItemSummary.class,
                source);

        assertEquals(source, target);

        var sourceArray = new PSItemSummary[]{source};
        var targetArray = (PSItemSummary[]) roundTripConversion(
                PSItemSummary[].class,
                com.percussion.webservices.content.PSItemSummary[].class,
                sourceArray);

        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server object conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var sourceList = new ArrayList<PSItemSummary>();
        sourceList.add(createItemSummary());
        sourceList.add(createItemSummary());

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSItemSummary[].class,
                sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create an item summary for testing.
     */
    public static PSItemSummary createItemSummary() {
        var summary = new PSItemSummary();
        summary.setGUID(new PSLegacyGuid(1001, 2));
        summary.setName("title");
        summary.setContentTypeId(101);
        summary.setContentTypeName("contentType");
        summary.setObjectType(PSItemSummary.ObjectTypeEnum.ITEM);

        Collection<PSItemSummary.OperationEnum> operations =
                new ArrayList<>();
        operations.add(PSItemSummary.OperationEnum.READ);
        operations.add(PSItemSummary.OperationEnum.WRITE);
        operations.add(PSItemSummary.OperationEnum.TRANSITION);
        summary.setOperations(operations);

        return summary;
    }
}
