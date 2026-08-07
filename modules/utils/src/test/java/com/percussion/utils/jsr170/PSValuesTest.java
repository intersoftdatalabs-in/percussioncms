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
package com.percussion.utils.jsr170;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSValuesTest {
  static final FastDateFormat ms_date = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

  public PSValuesTest() {}

  @Test
  public void testBoolean() throws Exception {
    PSValueFactory fact = new PSValueFactory();

    Value bool = fact.createValue(true);
    assertEquals(PropertyType.BOOLEAN, bool.getType());
    assertEquals("true", bool.getString());
    assertEquals(true, bool.getBoolean());
    try {
      bool.getDate();
      assertTrue(false == true, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
    try {
      bool.getLong();
      assertTrue(false, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
    try {
      bool.getDouble();
      assertTrue(false, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
    assertNotNull(bool.getStream());
  }

  @Test
  public void testDate() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    Calendar cal = new GregorianCalendar();
    cal.setTime(new Date());
    Value date = fact.createValue(cal);
    String dateStr = PSValueConverter.convertToString(cal);
    assertEquals(PropertyType.DATE, date.getType());
    assertEquals(dateStr, date.getString());
    assertEquals(cal, date.getDate());
    assertEquals(cal.getTimeInMillis(), date.getLong());
    assertEquals(cal.getTimeInMillis(), (long) date.getDouble());
    try {
      date.getBoolean();
      assertTrue(false, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
    assertNotNull(date.getStream());
  }

  @Test
  public void testDouble() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    double pi = Math.PI;
    Value d = fact.createValue(pi);
    assertEquals(PropertyType.DOUBLE, d.getType());
    assertEquals(pi, d.getDouble(), 0);
    assertEquals(3, d.getLong());
    assertEquals(Double.toString(pi), d.getString());
    assertNotNull(d.getStream());
    assertEquals(3, d.getDate().getTimeInMillis());
    try {
      d.getBoolean();
      assertTrue(false, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
  }

  @Test
  public void testBinary() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    byte arr[] = new byte[3];
    arr[0] = '1';
    arr[1] = '2';
    arr[2] = '3';
    InputStream stream = new ByteArrayInputStream(arr);
    Value bin = fact.createValue(stream);
    assertEquals("123", bin.getString());
    assertEquals(123, bin.getLong());
    assertEquals(123.0, bin.getDouble(), 0);
    assertEquals(false, bin.getBoolean());
    try {
      bin.getStream();
      fail();
    } catch (IllegalStateException e) {
      // OK
    }
    bin = fact.createValue(stream);
    assertNotNull(bin.getStream());

    long time = System.currentTimeMillis() / 1000; // Round to the second
    time = time * 1000;
    Calendar cal = PSValueConverter.convertToCalendar(time);
    String date = PSValueConverter.convertToString(cal);
    arr = date.getBytes();
    stream = new ByteArrayInputStream(arr);
    bin = fact.createValue(stream);
    assertEquals(ms_date.parse(date), bin.getDate().getTime());
  }

  @Test
  public void testLong() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    long foo = 150201;
    Value d = fact.createValue(foo);
    assertEquals(PropertyType.LONG, d.getType());
    assertEquals((double) foo, d.getDouble(), 0);
    assertEquals(foo, d.getLong());
    assertEquals(Long.toString(foo), d.getString());
    assertNotNull(d.getStream());
    assertEquals(foo, d.getDate().getTimeInMillis());
    try {
      d.getBoolean();
      assertTrue(false, "Should have thrown exception");
    } catch (Exception e) {
      // Ignore, correct
    }
  }

  @Test
  public void testLongFromString() throws Exception {
    // PSLongValue(String) uses Long.valueOf (not deprecated Long(String) ctor)
    PSLongValue fromString = new PSLongValue("150201");
    assertEquals(PropertyType.LONG, fromString.getType());
    assertEquals(150201L, fromString.getLong());
    assertEquals("150201", fromString.getString());
    try {
      new PSLongValue("not-a-number");
      fail("Expected ValueFormatException for non-numeric string");
    } catch (javax.jcr.ValueFormatException e) {
      // expected
    }
  }

  @Test
  public void testString() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    Value d = fact.createValue("12345");
    assertEquals(PropertyType.STRING, d.getType());
    assertEquals("12345", d.getString());
    assertEquals(12345, d.getLong());
    assertEquals(12345.0, d.getDouble(), 0);
    assertNotNull(d.getStream());
    assertEquals(false, d.getBoolean());

    long time = System.currentTimeMillis() / 1000; // Round to the second
    time = time * 1000;
    Calendar cal = PSValueConverter.convertToCalendar(time);
    String date = PSValueConverter.convertToString(cal);
    d = fact.createValue(date);
    assertEquals(ms_date.parse(date), d.getDate().getTime());
  }

  @Test
  public void testJcr20BinaryAndDecimal() throws Exception {
    PSValueFactory fact = new PSValueFactory();
    Value longVal = fact.createValue(42L);
    assertEquals(new java.math.BigDecimal("42"), longVal.getDecimal());
    assertNotNull(longVal.getBinary());
    assertEquals(2L, longVal.getBinary().getSize());

    byte[] data = new byte[] {1, 2, 3, 4};
    javax.jcr.Binary binary = fact.createBinary(new ByteArrayInputStream(data));
    assertEquals(4L, binary.getSize());
    Value fromBinary = fact.createValue(binary);
    assertNotNull(fromBinary.getBinary());

    Value fromDecimal = fact.createValue(new java.math.BigDecimal("3.5"));
    assertEquals(3.5d, fromDecimal.getDouble(), 0.0001);
  }

  @Test
  public void testPropertyDefinitionQueryMetadata() {
    PSPropertyDefinition def =
        new PSPropertyDefinition("rx:title", false, PropertyType.STRING, new StubNodeType());
    assertTrue(def.isFullTextSearchable());
    assertTrue(def.isQueryOrderable());
    assertTrue(def.getAvailableQueryOperators().length > 0);
  }

  /** Minimal node type stub for property definition construction in unit tests. */
  private static final class StubNodeType implements javax.jcr.nodetype.NodeType {
    @Override
    public String getName() {
      return "stub";
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
      return new String[0];
    }

    @Override
    public boolean isAbstract() {
      return false;
    }

    @Override
    public boolean isMixin() {
      return false;
    }

    @Override
    public boolean hasOrderableChildNodes() {
      return false;
    }

    @Override
    public boolean isQueryable() {
      return true;
    }

    @Override
    public String getPrimaryItemName() {
      return null;
    }

    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getDeclaredPropertyDefinitions() {
      return new javax.jcr.nodetype.PropertyDefinition[0];
    }

    @Override
    public javax.jcr.nodetype.NodeDefinition[] getDeclaredChildNodeDefinitions() {
      return new javax.jcr.nodetype.NodeDefinition[0];
    }

    @Override
    public boolean canAddChildNode(String childNodeName) {
      return false;
    }

    @Override
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
      return false;
    }

    @Override
    public boolean canRemoveItem(String itemName) {
      return false;
    }

    @Override
    public boolean canRemoveNode(String nodeName) {
      return false;
    }

    @Override
    public boolean canRemoveProperty(String propertyName) {
      return false;
    }

    @Override
    public boolean canSetProperty(String propertyName, Value value) {
      return false;
    }

    @Override
    public boolean canSetProperty(String propertyName, Value[] values) {
      return false;
    }

    @Override
    public javax.jcr.nodetype.NodeDefinition[] getChildNodeDefinitions() {
      return new javax.jcr.nodetype.NodeDefinition[0];
    }

    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getPropertyDefinitions() {
      return new javax.jcr.nodetype.PropertyDefinition[0];
    }

    @Override
    public javax.jcr.nodetype.NodeType[] getDeclaredSupertypes() {
      return new javax.jcr.nodetype.NodeType[0];
    }

    @Override
    public javax.jcr.nodetype.NodeTypeIterator getDeclaredSubtypes() {
      return null;
    }

    @Override
    public javax.jcr.nodetype.NodeType[] getSupertypes() {
      return new javax.jcr.nodetype.NodeType[0];
    }

    @Override
    public javax.jcr.nodetype.NodeTypeIterator getSubtypes() {
      return null;
    }

    @Override
    public boolean isNodeType(String nodeTypeName) {
      return false;
    }
  }

  @Test
  public void testRuntimeCheck() throws Exception {
    Value d = PSValueFactory.createValue((Object) 1.2);
    assertEquals(PropertyType.DOUBLE, d.getType());
    d = PSValueFactory.createValue((Object) 1);
    assertEquals(PropertyType.LONG, d.getType());
    d = PSValueFactory.createValue((Object) 134L);
    assertEquals(PropertyType.LONG, d.getType());
    d = PSValueFactory.createValue((Object) new Date());
    assertEquals(PropertyType.DATE, d.getType());
    d = PSValueFactory.createValue((Object) new GregorianCalendar());
    assertEquals(PropertyType.DATE, d.getType());
    d = PSValueFactory.createValue((Object) "how now");
    assertEquals(PropertyType.STRING, d.getType());
    d = PSValueFactory.createValue((Object) new byte[] {1, 3, 4});
    assertEquals(PropertyType.BINARY, d.getType());
    d = PSValueFactory.createValue((Object) false);
    assertEquals(PropertyType.BOOLEAN, d.getType());

    try {
      d = PSValueFactory.createValue(Boolean.class);
      assertTrue(false, "Should have thrown exception ");
    } catch (Exception e) {
      // OK, Expected
    }
  }
}
