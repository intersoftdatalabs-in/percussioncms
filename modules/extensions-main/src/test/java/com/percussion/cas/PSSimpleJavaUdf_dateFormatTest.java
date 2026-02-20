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
package com.percussion.cas;

import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.extensions.general.PSSimpleJavaUdfBaseTest;
import com.percussion.extensions.general.PSSimpleJavaUdf_dateFormat;
import com.percussion.extensions.general.PSSimpleJavaUdf_dateFormatEx;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests both versions: {@link PSSimpleJavaUdf_dateFormat} and {@link PSSimpleJavaUdf_dateFormatEx}.
 *
 * @author DougRand
 */
@Tag("UnitTest")
public class PSSimpleJavaUdf_dateFormatTest extends PSSimpleJavaUdfBaseTest {
  @Test
  public void testDateFormat() throws Exception {
    PSSimpleJavaUdf_dateFormat dateFormat = new PSSimpleJavaUdf_dateFormat();
    PSTextLiteral truevalue = new PSTextLiteral("true");
    PSTextLiteral truevalue2 = new PSTextLiteral("True");
    PSTextLiteral falsevalue = new PSTextLiteral("false");
    PSTextLiteral falsevalue2 = new PSTextLiteral("False");

    // Arguments are format, date, [ returnNullForEmpty]
    String date = (String) callUDF(dateFormat, null, null);

    assertNotNull(date, "Date should be non-null");
    assertTrue(date.trim().length() > 0, "Date should be non-empty");

    String date2 = (String) callUDF(dateFormat, null, "MM/dd/yyyy");
    assertNotNull(date2, "Date should be non-null");
    assertEquals('/', date2.charAt(2), "Date must follow output format");
    assertEquals('/', date2.charAt(5), "Date must follow output format");

    String date3 = (String) callUDF(dateFormat, null, null, null, truevalue);
    assertNull(date3, "Date must be null for input date null and returnNull true");

    String date4 = (String) callUDF(dateFormat, null, null, null, truevalue2);
    assertNull(date4, "Date must be null for input date null and returnNull true");

    String date5 = (String) callUDF(dateFormat, null, null, null, falsevalue);
    assertNotNull(date5, "Date must be non-null for input date null and returnNull false");

    String date6 = (String) callUDF(dateFormat, null, null, null, falsevalue2);
    assertNotNull(date6, "Date must be non-null for input date null and returnNull false");
  }

  @Test
  public void testDateFormatEx() throws Exception {
    PSSimpleJavaUdf_dateFormatEx dateFormatEx = new PSSimpleJavaUdf_dateFormatEx();
    PSTextLiteral truevalue = new PSTextLiteral("true");
    PSTextLiteral truevalue2 = new PSTextLiteral("True");
    PSTextLiteral falsevalue = new PSTextLiteral("false");
    PSTextLiteral falsevalue2 = new PSTextLiteral("False");

    // Arguments are format, date, [ returnNullForEmpty]
    String date = (String) callUDF(dateFormatEx, null, null);

    assertNotNull(date, "Date should be non-null");
    assertTrue(date.trim().length() > 0, "Date should be non-empty");

    String date2 = (String) callUDF(dateFormatEx, null, "MM/dd/yyyy");
    assertNotNull(date2, "Date should be non-null");
    assertEquals('/', date2.charAt(2), "Date must follow output format");
    assertEquals('/', date2.charAt(5), "Date must follow output format");

    String date3 = (String) callUDF(dateFormatEx, null, null, null, null, truevalue);
    assertNull(date3, "Date must be null for input date null and returnNull true");

    String date4 = (String) callUDF(dateFormatEx, null, null, null, null, truevalue2);
    assertNull(date4, "Date must be null for input date null and returnNull true");

    String date5 = (String) callUDF(dateFormatEx, null, null, null, null, falsevalue);
    assertNotNull(date5, "Date must be non-null for input date null and returnNull false");

    String date6 = (String) callUDF(dateFormatEx, null, null, null, null, falsevalue2);
    assertNotNull(date6, "Date must be non-null for input date null and returnNull false");
  }
}
