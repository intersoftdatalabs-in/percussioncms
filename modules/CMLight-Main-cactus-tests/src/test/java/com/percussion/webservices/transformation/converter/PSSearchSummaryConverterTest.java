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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSSearchSummary;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSSearchSummaryConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSSearchSummaryConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        // create the source object
        var source = createSearchSummary();

        var target = (PSSearchSummary) roundTripConversion(
                PSSearchSummary.class,
                com.percussion.webservices.content.PSSearchResults.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // create the source array
        var sourceArray = new PSSearchSummary[]{source};

        var targetArray = (PSSearchSummary[]) roundTripConversion(
                PSSearchSummary[].class,
                com.percussion.webservices.content.PSSearchResults[].class,
                sourceArray);

        // verify the round-trip array is equal to the source array
        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server object conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var sourceList = new ArrayList<PSSearchSummary>();
        sourceList.add(createSearchSummary());
        sourceList.add(createSearchSummary());

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSSearchResults[].class,
                sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create a search summary for testing.
     *
     * @return the new search summary, never {@code null}.
     */
    public static PSSearchSummary createSearchSummary() {
        var summary = new PSSearchSummary();
        summary.setGUID(new PSLegacyGuid(1001, 2));
        summary.setName("title");
        summary.setContentTypeId(101);
        summary.setContentTypeName("contentType");
        summary.setObjectType(PSItemSummary.ObjectTypeEnum.ITEM);

        Collection<PSItemSummary.OperationEnum> operations = new ArrayList<>();
        operations.add(PSItemSummary.OperationEnum.READ);
        operations.add(PSItemSummary.OperationEnum.WRITE);
        operations.add(PSItemSummary.OperationEnum.TRANSITION);
        summary.setOperations(operations);

        Map<String, String> fields = new HashMap<>();
        fields.put("name_1", "value_1");
        fields.put("name_2", "value_2");
        fields.put("name_3", "value_3");
        summary.setFields(fields);

        return summary;
    }
}
