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
package com.percussion.webservices.transformation.converter;

import com.percussion.search.objectstore.PSWSSearchField;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSSearchFieldConverter} class.
 */
@Category(IntegrationTest.class)
public class PSSearchFieldConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        // create the source object
        var source = createSearchField("field_1", "value_1", null);

        var target = (PSWSSearchField) roundTripConversion(
                PSWSSearchField.class,
                com.percussion.webservices.content.PSSearchField.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // create the source array
        var sourceArray = new PSWSSearchField[]{source};

        var targetArray = (PSWSSearchField[]) roundTripConversion(
                PSWSSearchField[].class,
                com.percussion.webservices.content.PSSearchField[].class,
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
        var sourceList = new ArrayList<PSWSSearchField>();
        sourceList.add(createSearchField("field_1", "value_1", null));
        sourceList.add(createSearchField("field_1", "value_1", "externalOp"));

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSSearchField[].class,
                sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create a search field for testing.
     *
     * @param name the field name, assumed not {@code null} or empty.
     * @param value the field value, assumed not {@code null} or empty.
     * @param externalOperator the external operator, may be {@code null}
     *    or empty to create a search field with an internal operator.
     * @return the new search field, never {@code null}.
     */
    private PSWSSearchField createSearchField(String name, String value, String externalOperator) {
        if (StringUtils.isBlank(externalOperator)) {
            return new PSWSSearchField(
                    name,
                    PSWSSearchField.PSOperatorEnum.ISNOTNULL.getOrdinal(),
                    value,
                    PSWSSearchField.PSConnectorEnum.OR.getOrdinal());
        } else {
            return new PSWSSearchField(
                    name,
                    externalOperator,
                    value,
                    PSWSSearchField.PSConnectorEnum.OR.getOrdinal());
        }
    }
}
