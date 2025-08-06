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

import com.percussion.search.objectstore.PSWSSearchParams;
import com.percussion.search.objectstore.PSWSSearchRequest;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSSearchConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSSearchConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        // create the source object
        var source = createSearch(3);

        var target = (PSWSSearchRequest) roundTripConversion(
                PSWSSearchRequest.class,
                com.percussion.webservices.content.PSSearch.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // create the source array
        var sourceArray = new PSWSSearchRequest[]{source};

        var targetArray = (PSWSSearchRequest[]) roundTripConversion(
                PSWSSearchRequest[].class,
                com.percussion.webservices.content.PSSearch[].class,
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
        var sourceList = new ArrayList<PSWSSearchRequest>();
        sourceList.add(createSearch(2));
        sourceList.add(createSearch(4));

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSSearch[].class, sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create the search request for testing.
     *
     * @param count the number of search parameters to create, assumed > 0.
     * @return the new search request, never {@code null}.
     * @throws Exception for any error.
     */
    private PSWSSearchRequest createSearch(int count) throws Exception {
        var searchParams = PSSearchParamsConverterTest.createSearchParams(count);

        var search = new PSWSSearchRequest(searchParams);
        search.setCaseInsensitiveSearch(false);
        search.setUseExternalSearchEngine(false);

        return search;
    }
}
