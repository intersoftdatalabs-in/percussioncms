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
// REFACTORED: CP-JAVA11

package com.percussion.pagemanagement.service;

import static com.percussion.pagemanagement.service.impl.PSWidgetUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.percussion.pagemanagement.service.impl.PSWidgetUtils.PSWidgetPropertyCoercionException;
import com.percussion.pagemanagement.service.impl.PSWidgetUtils.PSWidgetPropertyBlankStringCoercionException;

/**
 * Test widget utils.
 */
class PSWidgetUtilsTest {

    @Test
    void testCoerceString() {
        var actual = coerceProperty("my", "true", String.class);
        assertEquals("true", actual);
    }

    @Test
    void testCoerceBadString() {
        assertThrows(PSWidgetPropertyCoercionException.class, () -> coerceProperty("my", 1, String.class));
    }

    @Test
    void testCoerceBoolean() {
        var actual = coerceProperty("my", "true", Boolean.class);
        assertTrue(actual);

        actual = coerceProperty("my", true, Boolean.class);
        assertTrue(actual);
    }

    @Test
    void testCoerceNumber() {
        var actual = coerceProperty("my", 1, Number.class);
        assertEquals(1, actual);

        actual = coerceProperty("my", "1", Number.class);
        assertEquals(1, actual);
    }

    @Test
    void testCoerceBlankSpaces() {
        assertThrows(PSWidgetPropertyCoercionException.class, () -> coerceProperty("my", "  ", Number.class));
    }

    @Test
    void testCoerceBlankString() {
        assertThrows(PSWidgetPropertyBlankStringCoercionException.class, () -> coerceProperty("my", "", Number.class));
    }
}
