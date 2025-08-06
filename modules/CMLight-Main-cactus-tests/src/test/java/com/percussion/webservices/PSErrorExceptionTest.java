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
package com.percussion.webservices;

import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.faults.PSError;
import com.percussion.webservices.transformation.converter.PSConverterTestBase;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSErrorException} class.
 */
@Category(IntegrationTest.class)
class PSErrorExceptionTest extends PSConverterTestBase {

    /**
     * Test all contracts for PSErrorException.
     */
    @Test
    void testContracts() {
        // Null message
        var exception = assertThrows(IllegalArgumentException.class,
            () -> new PSErrorException(1, null, "stack"));
        assertNotNull(exception);

        // Blank message
        exception = assertThrows(IllegalArgumentException.class,
            () -> new PSErrorException(1, " ", "stack"));
        assertNotNull(exception);

        // Null stack
        exception = assertThrows(IllegalArgumentException.class,
            () -> new PSErrorException(1, "message", null));
        assertNotNull(exception);

        // Blank stack
        exception = assertThrows(IllegalArgumentException.class,
            () -> new PSErrorException(1, "message", " "));
        assertNotNull(exception);

        // Valid case
        assertDoesNotThrow(() -> new PSErrorException(1, "message", "stack"));
    }

    /**
     * Test conversion between server and client error exception.
     */
    @Test
    void testConversion() throws Exception {
        var source = new PSErrorException(1, "message", "stack");
        var target = (PSErrorException) roundTripConversion(
                PSErrorException.class, PSError.class, source);
        assertEquals(source, target);
    }
}
