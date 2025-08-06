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

import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.assembly.data.OutputFormatType;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSOutputFormatConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSOutputFormatConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // Create the source object
        var source = IPSAssemblyTemplate.OutputFormat.Database;

        var target = (IPSAssemblyTemplate.OutputFormat) roundTripConversion(
                IPSAssemblyTemplate.OutputFormat.class,
                OutputFormatType.class,
                source);

        // Verify the round-trip object is equal to the source object
        assertEquals(source, target);
    }
}
