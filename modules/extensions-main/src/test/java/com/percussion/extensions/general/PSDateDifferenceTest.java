/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.extensions.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.data.PSConversionException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.Test;

/** Behavioral unit tests for {@link PSDateDifference} (javac warning cleanup #2029). */
public class PSDateDifferenceTest {

  @Test
  public void processUdf_returnsLongDayDifference() throws Exception {
    PSDateDifference udf = new PSDateDifference();
    Calendar cal = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    Date start = cal.getTime();
    cal.add(Calendar.DAY_OF_MONTH, 10);
    Date end = cal.getTime();

    Object result = udf.processUdf(new Object[] {start, end}, null);
    assertTrue(result instanceof Long);
    assertEquals(10L, ((Long) result).longValue());
  }

  @Test
  public void processUdf_absoluteDifferenceOrderIndependent() throws Exception {
    PSDateDifference udf = new PSDateDifference();
    Calendar cal = new GregorianCalendar(2021, Calendar.MARCH, 1);
    Date earlier = cal.getTime();
    cal.add(Calendar.DAY_OF_MONTH, 3);
    Date later = cal.getTime();

    Long forward = (Long) udf.processUdf(new Object[] {earlier, later}, null);
    Long reverse = (Long) udf.processUdf(new Object[] {later, earlier}, null);
    assertEquals(3L, forward.longValue());
    assertEquals(3L, reverse.longValue());
  }

  @Test
  public void processUdf_missingParamsThrows() {
    PSDateDifference udf = new PSDateDifference();
    assertThrows(PSConversionException.class, () -> udf.processUdf(null, null));
    assertThrows(PSConversionException.class, () -> udf.processUdf(new Object[] {}, null));
  }
}
