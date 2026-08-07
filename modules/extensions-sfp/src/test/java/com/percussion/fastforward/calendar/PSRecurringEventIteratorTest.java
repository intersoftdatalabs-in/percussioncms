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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSRecurrenceIterator} / {@link
 * PSRecurringEvent#getRecurrenceIterator()} (issue #2035 batch 1).
 */
class PSRecurringEventIteratorTest {

  private static Date date(int year, int month, int day) {
    return new GregorianCalendar(year, month, day).getTime();
  }

  @Test
  void dailyRecurrenceIteratorYieldsCalendarsUntilEnd() {
    // Iterator starts at recurrence index 1 (see PSRecurrenceIterator): for a daily
    // event spanning Jan 1–3, indices 1 and 2 yield Jan 2 and Jan 3; index 0 (start)
    // is not visited by the iterator (historical behavior, not changed in batch 1).
    PSRecurringEvent event =
        new PSRecurringEvent(
            date(2026, Calendar.JANUARY, 1),
            date(2026, Calendar.JANUARY, 3),
            1,
            PSRecurringEvent.DAILY_RECURRENCE,
            0,
            0,
            0);

    Iterator<Calendar> it = event.getRecurrenceIterator();
    assertTrue(it.hasNext());
    Calendar first = it.next();
    assertNotNull(first);
    assertEquals(2, first.get(Calendar.DAY_OF_MONTH));

    assertTrue(it.hasNext());
    Calendar second = it.next();
    assertEquals(3, second.get(Calendar.DAY_OF_MONTH));

    // next recurrence is after end date
    assertFalse(it.hasNext());
  }

  @Test
  void removeUnsupported() {
    PSRecurringEvent event =
        new PSRecurringEvent(
            date(2026, Calendar.JANUARY, 1),
            date(2026, Calendar.JANUARY, 2),
            1,
            PSRecurringEvent.DAILY_RECURRENCE,
            0,
            0,
            0);
    Iterator<Calendar> it = event.getRecurrenceIterator();
    assertThrows(UnsupportedOperationException.class, it::remove);
  }
}
