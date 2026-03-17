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

package com.percussion.util;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.design.objectstore.PSNumericLiteral;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.system.utils.PSCalculation;
import com.percussion.system.utils.PSDate;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * The PSCalculationTest is the unit test for PSCalculation class which handles basic mathematical
 * operation.
 *
 * @author Jian Huang
 * @version 2.0
 * @since 1.0
 */
public class PSCalculationTest {
  private static final CalcAdapter calculate = new CalcAdapter();

  private static class CalcAdapter {
    Double add(Object a, Object b) {
      return PSCalculation.add(a, b);
    }

    Double subtract(Object a, Object b) {
      return PSCalculation.subtract(a, b);
    }

    Double multiply(Object a, Object b) {
      return PSCalculation.multiply(a, b);
    }

    Double divide(Object a, Object b) {
      return PSCalculation.divide(a, b);
    }

    Number numberVerify(Object o) {
      return PSCalculation.numberVerify(o);
    }

    PSDate dateAdjust(java.util.Calendar c, int y, int m, int d, int h, int mi, int s) {
      return PSCalculation.dateAdjust(c, y, m, d, h, mi, s);
    }
  }

  public void testAdd() throws Exception {

    Object o1, o2, result;
    double resultValue = 0;
    double value1 = 100;
    double value2 = -50;

    o1 = new Double(value1);
    o2 = new BigDecimal(value2);

    boolean didThrow;

    // test (null + null), (normal + null), (null + normal), (normal + normal),
    // (abnormal + normal), and (normal + abnormal)
    didThrow = false;
    try {
      result = calculate.add(null, null);
      assertTrue(result == null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.add(o1, null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.add(null, o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.add(o1, o2);
      resultValue = ((Number) result).doubleValue();
      assertTrue(resultValue == (value1 + value2));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.add("abnormal", o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.add(o1, "abnormal");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  public void testSubtract() throws Exception {

    Object o1, o2, result;
    double resultValue = 0;
    double value1 = 100;
    double value2 = -50;

    java.text.DecimalFormat format = new java.text.DecimalFormat();
    Double numOne = new Double(value1);
    o1 = new PSNumericLiteral(numOne, format);
    o2 = new PSTextLiteral("-50");

    boolean didThrow;

    // test (null - null), (normal - null), (null - normal), (normal - normal),
    // (abnormal - normal), and (normal - abnormal)
    didThrow = false;
    try {
      result = calculate.subtract(null, null);
      assertTrue(result == null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.subtract(o1, null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.subtract(null, o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.subtract(o1, o2);
      resultValue = ((Number) result).doubleValue();
      assertTrue(resultValue == (value1 - value2));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.subtract("abnormal", o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.subtract(o1, "abnormal");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  public void testMultiply() throws Exception {

    Object o1, o2, result;
    double resultValue = 0;
    double value1 = 100;
    double value2 = -50;

    o1 = new Double(value1);
    o2 = new BigDecimal(value2);

    boolean didThrow;

    // test (null * null), (normal * null), (null * normal), (normal * normal),
    // (abnormal * normal), and (normal * abnormal)
    didThrow = false;
    try {
      result = calculate.multiply(null, null);
      assertTrue(result == null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.multiply(o1, null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.multiply(null, o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.multiply(o1, o2);
      resultValue = ((Number) result).doubleValue();
      assertTrue(resultValue == (value1 * value2));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.multiply("abnormal", o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.multiply(o1, "abnormal");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  public void testDivide() throws Exception {

    Object o1, o2, result;
    double resultValue = 0;
    double value1 = 100;
    double value2 = -50;

    o1 = new Double(value1);
    o2 = new BigDecimal(value2);

    boolean didThrow;

    // test (null / null), (normal / null), (null / normal), (normal / normal),
    // (abnormal / normal), (normal / abnormal), and (normal / 0)
    didThrow = false;
    try {
      result = calculate.divide(null, null);
      assertTrue(result == null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.divide(o1, null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.divide(null, o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.divide(o1, o2);
      resultValue = ((Number) result).doubleValue();
      assertTrue(resultValue == (value1 / value2));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.divide("abnormal", o2);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.divide(o1, "abnormal");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.divide(o1, Integer.valueOf(0));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  public void testNumberVerify() throws Exception {

    Object result;
    boolean didThrow;

    didThrow = false;
    try {
      result = calculate.numberVerify(null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.numberVerify("not a number");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.numberVerify(new Date());
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      result = calculate.numberVerify(Integer.valueOf(0));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);

    didThrow = false;
    try {
      result = calculate.numberVerify("100");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertFalse(didThrow);
  }

  public void testDateAdjust() throws Exception {

    Object result = calculate.dateAdjust(null, 1, 1, 1, 1, 1, 1);
    assertTrue(result == null);

    String strDate = "1999-12-01 00:00:00";
    int nYear = 1;
    int nMonth = 0;
    int nDay = -1;
    int nHour = 24;
    int nMin = 3;
    int nSec = 61;

    PSDate dateNew = null;
    Calendar dateOld = null;
    FastDateFormat df =
        FastDateFormat.getInstance(PSDataTypeConverter.getRecognizedDateFormat(strDate));
    if (df != null) {
      // this should not throw a parse exception
      Date day = df.parse(strDate);
      dateOld = Calendar.getInstance();
      dateOld.setTime(day);
      dateNew = PSCalculation.dateAdjust(dateOld, nYear, nMonth, nDay, nHour, nMin, nSec);
    }

    assertTrue(dateNew != null, "Could not parse date: " + strDate);
    assertTrue(
        (dateNew.toString()).equals("2000-12-01 00:04:01"),
        "(" + dateNew.toString() + ") equals (" + strDate + ")");
  }

  // collect all tests into a TestSuite and return it

}
