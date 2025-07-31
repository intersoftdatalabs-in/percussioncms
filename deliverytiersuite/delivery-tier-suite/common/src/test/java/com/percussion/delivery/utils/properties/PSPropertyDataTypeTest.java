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
package com.percussion.delivery.utils.properties;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sunny Sal says: "Data type ka test, type safety ka best!"
 */
public class PSPropertyDataTypeTest {

    @Test
    public void testTypes() {
        assertEquals("string", PSPropertyDataType.STRING.getName());
        assertEquals("enum", PSPropertyDataType.ENUM.getName());
        assertEquals("number", PSPropertyDataType.NUMBER.getName());
        assertEquals("bool", PSPropertyDataType.BOOL.getName());
        assertEquals("hidden", PSPropertyDataType.HIDDEN.getName());
        assertEquals("date", PSPropertyDataType.DATE.getName());
        assertEquals("list", PSPropertyDataType.LIST.getName());
    }

    @Test
    public void testString() {
        var t = PSPropertyDataType.parseType("string");
        assertEquals(String.class, t.getJavaType());
        assertEquals("string", t.getName());
    }

    @Test
    public void testEnum() {
        var t = PSPropertyDataType.parseType("enum");
        assertEquals(String.class, t.getJavaType());
        assertEquals("enum", t.getName());
    }

    @Test
    public void testNumber() {
        var t = PSPropertyDataType.parseType("number");
        assertEquals(Number.class, t.getJavaType());
        assertEquals("number", t.getName());
    }

    @Test
    public void testBool() {
        var t = PSPropertyDataType.parseType("bool");
        assertEquals(Boolean.class, t.getJavaType());
        assertEquals("bool", t.getName());
    }

    @Test
    public void testList() {
        var t = PSPropertyDataType.parseType("list");
        assertEquals(List.class, t.getJavaType());
        assertEquals("list", t.getName());
    }

    @Test
    public void testDate() {
        var t = PSPropertyDataType.parseType("date");
        assertEquals(Date.class, t.getJavaType());
        assertEquals("date", t.getName());
    }

    @Test
    public void testHidden() {
        var t = PSPropertyDataType.parseType("hidden");
        assertEquals(Object.class, t.getJavaType());
        assertEquals("hidden", t.getName());
    }

    @Test
    public void testFromProp() {
        var p = new PSPropertyDefinition();
        p.setDatatype("hidden");
        assertEquals(Object.class, PSPropertyDataType.fromDefinition(p).getJavaType());
        assertEquals("hidden", PSPropertyDataType.fromDefinition(p).getName());
    }
}
