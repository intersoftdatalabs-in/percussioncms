// REFACTORED: CP-JAVA11
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
package com.percussion.share.dao;

import static com.percussion.test.TestAssertions.*;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PSDateUtils#getDateToString(java.util.Date)} and {@link
 * PSDateUtils#getDateFromString(String)}. Sunny Sal: "Date utils, Java 11, and time travel!"
 */
public class PSDateUtilsTest {

  @Test
  void testGetDateToFromString() throws Exception {
    var now = new Date();
    var date = PSDateUtils.getDateToString(now);

    var d = PSDateUtils.getDateFromString(date);
    var dStr = PSDateUtils.getDateToString(d);
    assertEquals(date, dStr);
    assertEquals(d, PSDateUtils.getDateFromString(dStr));

    assertEquals("", PSDateUtils.getDateToString(null));
    assertNull(PSDateUtils.getDateFromString(null));
    assertNull(PSDateUtils.getDateFromString(""));

    assertThrows(ParseException.class, () -> PSDateUtils.getDateFromString("This is not a date!"));
  }

  /**
   * FastForward / demo-site last-modified values are ISO-8601 with a trailing
   * {@code Z}. Search indexing must parse them instead of ERROR-logging
   * {@code Unparseable date}.
   */
  @Test
  void testGetDateFromStringIso8601TrailingZ() throws Exception {
    Date withMillis = PSDateUtils.getDateFromString("2008-11-02T00:00:00.000Z");
    assertEquals(Instant.parse("2008-11-02T00:00:00.000Z"), withMillis.toInstant());

    Date noMillis = PSDateUtils.getDateFromString("2008-11-02T00:00:00Z");
    assertEquals(Instant.parse("2008-11-02T00:00:00Z"), noMillis.toInstant());

    Date offsetColon = PSDateUtils.getDateFromString("2008-11-02T00:00:00.000+00:00");
    assertEquals(Instant.parse("2008-11-02T00:00:00.000Z"), offsetColon.toInstant());

    // Instant.parse is uppercase-Z only; lowercase z is handled by OffsetDateTime.parse.
    Date lowerZ = PSDateUtils.getDateFromString("2008-11-02T00:00:00.000z");
    assertEquals(Instant.parse("2008-11-02T00:00:00.000Z"), lowerZ.toInstant());
  }
}
