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
package com.percussion.fastforward.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.IPSRequestContext;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Behavioral tests for typed holiday set APIs on {@link PSHolidays} (issue #2035 batch 1). */
class PSHolidaysTest {

  private static Date date(int year, int month, int day) {
    return new GregorianCalendar(year, month, day).getTime();
  }

  private static PSHolidays holidaysWith(final String name, final Date when) {
    return new PSHolidays(Mockito.mock(IPSRequestContext.class)) {
      @Override
      protected Set<Holiday> loadHolidays(IPSRequestContext req) {
        Set<Holiday> holidays = new HashSet<>();
        holidays.add(new Holiday(when, name));
        return holidays;
      }
    };
  }

  @Test
  void isHolidayAndGetHolidayMatchYearMonthDayIgnoringTime() {
    PSHolidays host = holidaysWith("Christmas", date(2026, Calendar.DECEMBER, 25));

    Date christmasMorning = new GregorianCalendar(2026, Calendar.DECEMBER, 25, 9, 30).getTime();
    Date christmasEve = date(2026, Calendar.DECEMBER, 24);

    assertTrue(host.isHoliday(christmasMorning));
    assertEquals("Christmas", host.getHoliday(christmasMorning));
    assertFalse(host.isHoliday(christmasEve));
    assertNull(host.getHoliday(christmasEve));
  }

  @Test
  void nullDateRejected() {
    PSHolidays host =
        new PSHolidays(Mockito.mock(IPSRequestContext.class)) {
          @Override
          protected Set<Holiday> loadHolidays(IPSRequestContext req) {
            return new HashSet<>();
          }
        };
    assertThrows(IllegalArgumentException.class, () -> host.isHoliday(null));
    assertThrows(IllegalArgumentException.class, () -> host.getHoliday(null));
  }
}
