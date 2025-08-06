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

package com.percussion.category.web.service;

import com.percussion.category.data.PSCategory;
import com.percussion.category.marshaller.PSCategoryMarshaller;
import com.percussion.category.marshaller.PSCategoryUnMarshaller;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PSJerseyRestClient and PSCategoryServiceRestClient.
 */
@Tag("IntegrationTest")
public class PSJerseyRestClientTest {

    private static PSCategoryServiceRestClient client;

    @BeforeAll
    public static void setUp() {
        client = new PSCategoryServiceRestClient();
    }

    @Test
    public void testGetCategories() throws PSDataServiceException {
        var result = client.getCategoryList("xyz");
        assertNotNull(result);
        validateData(PSCategoryMarshaller.marshalToJson(result));
    }

    @Test
    public void testUpdateCategories() throws PSDataServiceException {
        var resultCat = client.getCategoryList("xyz");
        var result = PSCategoryMarshaller.marshalToJson(resultCat);
        assertNotNull(result);
        result = result.replace("Children", "topLevelNodes");
        result = result.replace("Child", "childNodes");

        if (result.contains("\"topLevelNodes\":{")) {
            result = result.replace("\"topLevelNodes\":{", "\"topLevelNodes\":[{");
        }

        result = result.replace("\"childNodes\":{", "\"childNodes\":[{");
        Pattern pattern = Pattern.compile("\\}\\}");
        Matcher m = pattern.matcher(result);
        while (m.find()) {
            result = result.replace("}}", "}]}");
            m = pattern.matcher(result);
        }
        var category = PSCategoryUnMarshaller.unMarshalFromString(result);
        var updatedResult = client.updateCategories(category, "xyz");
        validateData(PSCategoryMarshaller.marshalToJson(updatedResult));
    }

    private void validateData(String result) {
        result = result.replace("Children", "topLevelNodes");
        result = result.replace("Child", "childNodes");

        if (result.contains("\"topLevelNodes\":{")) {
            result = result.replace("\"topLevelNodes\":{", "\"topLevelNodes\":[{");
        }

        result = result.replace("\"childNodes\":{", "\"childNodes\":[{");
        Pattern pattern = Pattern.compile("\\}\\}");
        Matcher m = pattern.matcher(result);
        while (m.find()) {
            result = result.replace("}}", "}]}");
            m = pattern.matcher(result);
        }

        var category = PSCategoryUnMarshaller.unMarshalFromString(result);
        assertNotNull(category, "categories cannot be null");
        // Additional assertions can be added here for deeper validation.
    }
}
