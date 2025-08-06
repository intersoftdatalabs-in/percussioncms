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
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSKeywordConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSKeywordConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        var source = createKeyword("1");

        var target = (PSKeyword) roundTripConversion(
                PSKeyword.class,
                com.percussion.webservices.content.PSKeyword.class,
                source);

        assertEquals(source, target);

        var sourceArray = new PSKeyword[]{source};
        var targetArray = (PSKeyword[]) roundTripConversion(
                PSKeyword[].class,
                com.percussion.webservices.content.PSKeyword[].class,
                sourceArray);

        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server object conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var sourceList = new ArrayList<PSKeyword>();
        sourceList.add(createKeyword("1"));
        sourceList.add(createKeyword("2"));

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSKeyword[].class,
                sourceList);

        assertEquals(sourceList, targetList);
    }

    /**
     * Create a keyword for testing.
     */
    private PSKeyword createKeyword(String index) {
        var keyword = new PSKeyword("label_" + index,
                "description_" + index, index);
        keyword.setGUID(new PSGuid(PSTypeEnum.KEYWORD_DEF, 1001));
        var choices = new ArrayList<PSKeywordChoice>();
        for (int i = 0; i < 3; i++) {
            var choice = new PSKeywordChoice();
            choice.setLabel("choice_" + index + "." + i);
            choice.setDescription("description_" + index + "." + i);
            choice.setValue("1." + i);
            choice.setSequence(i);
            choices.add(choice);
        }
        keyword.setChoices(choices);
        return keyword;
    }
}
