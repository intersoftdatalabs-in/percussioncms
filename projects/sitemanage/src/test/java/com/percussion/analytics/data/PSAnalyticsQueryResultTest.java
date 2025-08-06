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
package com.percussion.analytics.data;

import com.percussion.analytics.data.IPSAnalyticsQueryResult.DataType;
import com.percussion.analytics.data.impl.PSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsQueryResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PSAnalyticsQueryResult}.
 */
@SuppressWarnings({"deprecation"})
class PSAnalyticsQueryResultTest {

    private PSAnalyticsQueryResult result;

    private static final String KEY_STRING = "stringKey";
    private static final String KEY_INT = "intKey";
    private static final String KEY_LONG = "longKey";
    private static final String KEY_FLOAT = "floatKey";
    private static final String KEY_DATE = "dateKey";

    private static final String VALUE_STRING = "FooBar";
    private static final int VALUE_INT = 14;
    private static final long VALUE_LONG = 35L;
    private static final float VALUE_FLOAT = 23.45F;
    private static final Date VALUE_DATE = new Date(89, 4, 10);

    @BeforeEach
    void setUp() {
        result = new PSAnalyticsQueryResult();
        result.put(KEY_STRING, VALUE_STRING);
        result.put(KEY_INT, VALUE_INT);
        result.put(KEY_LONG, VALUE_LONG);
        result.put(KEY_FLOAT, VALUE_FLOAT);
        result.put(KEY_DATE, VALUE_DATE);
    }

    @Test
    void testPutMethods() {
        var r = new PSAnalyticsQueryResult();
        var vals = new HashMap<String, Object>();
        vals.put(KEY_STRING, VALUE_STRING);
        vals.put(KEY_INT, VALUE_INT);
        vals.put(KEY_LONG, VALUE_LONG);
        vals.put(KEY_FLOAT, VALUE_FLOAT);
        vals.put(KEY_DATE, VALUE_DATE);
        r.putAll(vals);
        assertEquals(5, r.keySet().size());

        var ex1 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                r.put("INVALID_CLASS", new HashMap<>()));
        assertEquals("Class type is not supported.", ex1.getMessage());

        var ex2 = assertThrows(IllegalArgumentException.class, () ->
                r.put(KEY_INT, null));
        assertEquals("Value cannot be null.", ex2.getMessage());

        var ex3 = assertThrows(IllegalArgumentException.class, () ->
                r.put(null, new HashMap<>()));
        assertEquals("key cannot be null or empty.", ex3.getMessage());

        var ex4 = assertThrows(IllegalArgumentException.class, () ->
                r.putAll(null));
        assertEquals("values cannot be null or empty.", ex4.getMessage());
    }

    @Test
    void testHasValue() {
        assertTrue(result.hasValue(KEY_INT));
        assertFalse(result.hasValue("DUMMY_KEY"));

        var ex = assertThrows(IllegalArgumentException.class, () ->
                result.hasValue(null));
        assertEquals("key cannot be null or empty.", ex.getMessage());
    }

    @Test
    void testGetDataType() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
                result.getDataType(null));
        assertEquals("key cannot be null or empty.", ex.getMessage());
    }

    @Test
    void testGetDate() {
        assertThrows(IllegalArgumentException.class, () -> result.getDate(null));
        assertNull(result.getDate("null"));
        assertEquals(VALUE_DATE, result.getDate(KEY_DATE));

        var ex1 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getDate(KEY_INT));
        assertEquals("Type cannot be converted to a Date", ex1.getMessage());

        var ex2 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getDate(KEY_LONG));
        assertEquals("Type cannot be converted to a Date", ex2.getMessage());

        var ex3 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getDate(KEY_FLOAT));
        assertEquals("Type cannot be converted to a Date", ex3.getMessage());

        var ex4 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getDate(KEY_STRING));
        assertEquals("Type cannot be converted to a Date", ex4.getMessage());
    }

    @Test
    void testGetInt() {
        assertThrows(IllegalArgumentException.class, () -> result.getInt(null));
        assertEquals(-1, result.getInt("null"));
        assertEquals(VALUE_INT, result.getInt(KEY_INT));
        assertEquals(23, result.getInt(KEY_FLOAT));
        assertEquals(35, result.getInt(KEY_LONG));

        var ex1 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getInt(KEY_STRING));
        assertEquals("Type cannot be converted to a Integer", ex1.getMessage());

        var ex2 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getInt(KEY_DATE));
        assertEquals("Type cannot be converted to a Integer", ex2.getMessage());
    }

    @Test
    void testGetString() {
        assertNull(result.getString("null"));
        assertEquals(VALUE_STRING, result.getString(KEY_STRING));
        assertEquals("14", result.getString(KEY_INT));
        assertEquals("23.45", result.getString(KEY_FLOAT));
        assertEquals("35", result.getString(KEY_LONG));

        var ex = assertThrows(IllegalArgumentException.class, () ->
                result.getString(null));
        assertEquals("key cannot be null or empty.", ex.getMessage());
    }

    @Test
    void testGetFloat() {
        assertThrows(IllegalArgumentException.class, () -> result.getFloat(null));
        assertEquals(VALUE_FLOAT, result.getFloat(KEY_FLOAT), 0);
        assertEquals(14F, result.getFloat(KEY_INT), 0);
        assertEquals(35F, result.getFloat(KEY_LONG), 0);
        assertNull(result.getDate("null"));

        var ex1 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getFloat(KEY_STRING));
        assertEquals("Type cannot be converted to a Float", ex1.getMessage());

        var ex2 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getFloat(KEY_DATE));
        assertEquals("Type cannot be converted to a Float", ex2.getMessage());
    }

    @Test
    void testGetLong() {
        assertEquals(-1, result.getLong("null"));
        assertEquals(VALUE_LONG, result.getLong(KEY_LONG));
        assertEquals(14L, result.getLong(KEY_INT));
        assertEquals(23L, result.getLong(KEY_FLOAT));
        assertEquals(VALUE_DATE.getTime(), result.getLong(KEY_DATE));

        var ex = assertThrows(IllegalArgumentException.class, () ->
                result.getLong(null));
        assertEquals("key cannot be null or empty.", ex.getMessage());

        var ex2 = assertThrows(PSAnalyticsQueryResultException.class, () ->
                result.getLong(KEY_STRING));
        assertEquals("Type cannot be converted to a Long", ex2.getMessage());
    }

    @Test
    void testConstructor() {
        var testMap = new HashMap<String, Object>();
        testMap.put(KEY_STRING, VALUE_STRING);
        testMap.put(KEY_INT, VALUE_INT);
        testMap.put(KEY_LONG, VALUE_LONG);
        testMap.put(KEY_FLOAT, VALUE_FLOAT);
        testMap.put(KEY_DATE, VALUE_DATE);
        final var resultConstructorTest = new PSAnalyticsQueryResult(testMap);
        assertEquals(5, resultConstructorTest.keySet().size());
    }
}
