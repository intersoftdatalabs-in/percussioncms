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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.system.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.Test;

class PSFormatVersionTest {

  @Test
  void testGetBuildDateWithDayOfMonthPattern() throws Exception {
    java.lang.reflect.Constructor<PSFormatVersion> ctor =
        PSFormatVersion.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    PSFormatVersion version = ctor.newInstance();
    Field buildNumberField = PSFormatVersion.class.getDeclaredField("m_buildNumber");
    buildNumberField.setAccessible(true);
    buildNumberField.set(version, "20260721");

    Date buildDate = version.getBuildDate();
    assertNotNull(buildDate, "Build date should be successfully parsed");

    Calendar cal = new GregorianCalendar();
    cal.setTime(buildDate);
    assertEquals(2026, cal.get(Calendar.YEAR));
    assertEquals(Calendar.JULY, cal.get(Calendar.MONTH));
    assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
  }
}
