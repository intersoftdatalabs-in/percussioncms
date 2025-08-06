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

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.search.objectstore.PSWSSearchField;
import com.percussion.search.objectstore.PSWSSearchParams;

import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSSearchParamsConverter} class.
 */
@Tag("IntegrationTest")
public class PSSearchParamsConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        // create the source object
        var source = createSearchParams(3);

        var target = (PSWSSearchParams) roundTripConversion(
                PSWSSearchParams.class,
                com.percussion.webservices.content.PSSearchParams.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // create the source array
        var sourceArray = new PSWSSearchParams[]{source};

        var targetArray = (PSWSSearchParams[]) roundTripConversion(
                PSWSSearchParams[].class,
                com.percussion.webservices.content.PSSearchParams[].class,
                sourceArray);

        // verify the round-trip array is equal to the source array
        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var sourceList = new ArrayList<PSWSSearchParams>();
        sourceList.add(createSearchParams(2));
        sourceList.add(createSearchParams(4));

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSSearchParams[].class,
                sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create the search parameters for testing.
     *
     * @param count the number of properties, result and search fields to
     *    create, must be >= 0.
     * @return the new search parameters, never {@code null}.
     * @throws Exception for any error.
     */
    public static PSWSSearchParams createSearchParams(int count) throws Exception {
        if (count < 0)
            throw new IllegalArgumentException("count must be >= 0");

        // register a test content type definition
        var mgr = PSItemConverterTest.getTestItemDefManager();
        PSItemDefinition def = mgr.getItemDef(316, -1);

        var searchParams = new PSWSSearchParams();
        searchParams.setContentTypeId(def.getContentEditor().getContentType());
        searchParams.setTitle("title",
                PSWSSearchField.PSOperatorEnum.LESSTHAN.getOrdinal(),
                PSWSSearchField.PSConnectorEnum.AND.getOrdinal());
        searchParams.setSearchForFolders(true);
        searchParams.setFolderPathFilter("folderPathFilter", false);
        searchParams.setFTSQuery("fullTextQuery");
        Map<String, String> properties = new HashMap<>();
        Collection<String> resultFields = new ArrayList<>();
        List<PSWSSearchField> searchFields = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            properties.put("propertyName_" + i, "value_" + i);
            resultFields.add("resultField_" + i);

            var searchField = new PSWSSearchField("searchField_" + i,
                    PSWSSearchField.PSOperatorEnum.EQUAL.getOrdinal(), "value_" + i,
                    PSWSSearchField.PSConnectorEnum.AND.getOrdinal());
            searchFields.add(searchField);
        }
        searchParams.setProperties(properties);
        searchParams.setResultFields(resultFields);
        searchParams.setSearchFields(searchFields);

        return searchParams;
    }
}
