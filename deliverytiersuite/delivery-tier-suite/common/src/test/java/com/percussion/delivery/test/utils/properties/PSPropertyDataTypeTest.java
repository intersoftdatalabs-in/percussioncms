/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.test.utils.properties;

import com.percussion.delivery.utils.properties.PSPropertyDataType;
import com.percussion.delivery.utils.properties.PSPropertyDefinition;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author natechadwick
 */
public class PSPropertyDataTypeTest {

  @Test
  public void testTypes() {

    Assertions.assertEquals("string", PSPropertyDataType.STRING.getName());
    Assertions.assertEquals("enum", PSPropertyDataType.ENUM.getName());
    Assertions.assertEquals("number", PSPropertyDataType.NUMBER.getName());
    Assertions.assertEquals("bool", PSPropertyDataType.BOOL.getName());
    Assertions.assertEquals("hidden", PSPropertyDataType.HIDDEN.getName());
    Assertions.assertEquals("date", PSPropertyDataType.DATE.getName());
    Assertions.assertEquals("list", PSPropertyDataType.LIST.getName());
  }

  @Test
  public void testString() {
    PSPropertyDataType t = PSPropertyDataType.parseType("string");
    Assertions.assertEquals(String.class, t.getJavaType());
    Assertions.assertEquals("string", t.getName());
  }

  @Test
  public void testEnum() {
    PSPropertyDataType t = PSPropertyDataType.parseType("enum");
    Assertions.assertEquals(String.class, t.getJavaType());
    Assertions.assertEquals("enum", t.getName());
  }

  @Test
  public void testNumber() {
    PSPropertyDataType t = PSPropertyDataType.parseType("number");
    Assertions.assertEquals(Number.class, t.getJavaType());
    Assertions.assertEquals("number", t.getName());
  }

  @Test
  public void testBool() {
    PSPropertyDataType t = PSPropertyDataType.parseType("bool");
    Assertions.assertEquals(Boolean.class, t.getJavaType());
    Assertions.assertEquals("bool", t.getName());
  }

  @Test
  public void testList() {
    PSPropertyDataType t = PSPropertyDataType.parseType("list");
    Assertions.assertEquals(List.class, t.getJavaType());
    Assertions.assertEquals("list", t.getName());
  }

  @Test
  public void testDate() {
    PSPropertyDataType t = PSPropertyDataType.parseType("date");
    Assertions.assertEquals(Date.class, t.getJavaType());
    Assertions.assertEquals("date", t.getName());
  }

  @Test
  public void testHidden() {
    PSPropertyDataType t = PSPropertyDataType.parseType("hidden");
    Assertions.assertEquals(Object.class, t.getJavaType());
    Assertions.assertEquals("hidden", t.getName());
  }

  @Test
  public void testFromProp() {
    PSPropertyDefinition p = new PSPropertyDefinition();

    p.setDatatype("hidden");

    Assertions.assertEquals(Object.class, PSPropertyDataType.fromDefinition(p).getJavaType());

    Assertions.assertEquals("hidden", PSPropertyDataType.fromDefinition(p).getName());
  }
}
