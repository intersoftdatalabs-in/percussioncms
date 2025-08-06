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

import com.percussion.services.content.data.PSFieldDescription;

import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link PSFieldDescriptionConverter} class.
 */
@Tag("IntegrationTest")
public class PSFieldDescriptionConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // Create the source object
        var src = new PSFieldDescription("fld1", PSFieldDescription.PSFieldTypeEnum.TEXT.name());

        var target = (PSFieldDescription) roundTripConversion(
                PSFieldDescription.class,
                com.percussion.webservices.content.PSFieldDescription.class,
                src);

        // Verify the round-trip object is equal to the source object
        assertEquals(src, target);
    }

    /**
     * Test a list of server object convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSFieldDescription>();
        srcList.add(new PSFieldDescription("fld1", PSFieldDescription.PSFieldTypeEnum.TEXT.name()));
        srcList.add(new PSFieldDescription("fld2", PSFieldDescription.PSFieldTypeEnum.TEXT.name()));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.content.PSFieldDescription[].class,
                srcList);

        assertEquals(srcList, srcList2);
    }
}
