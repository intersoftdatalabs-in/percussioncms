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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the PR #1589 hot-fix #2 notify map: Hibernate {@code
 * PSContentStatusContext.commit()} must populate all 15 legacy CONTENTSTATUS columns for
 * item-summary cache invalidation (not CONTENTID-only).
 *
 * <p>Tests {@link PSContentStatusNotifyMap} only — pure map building with no JDBC or Spring static
 * initializers — so the suite does not hit {@code ExceptionInInitializerError} when other tests
 * have already poisoned class statics.
 */
public class PSContentStatusContextCommitTest {

  private static TimeZone previousTz;

  @BeforeAll
  static void beforeAll() {
    previousTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @AfterAll
  static void afterAll() {
    if (previousTz != null) {
      TimeZone.setDefault(previousTz);
    }
  }

  @Test
  void buildLegacyColumnMap_populatesAllFifteenColumns() {
    Map<String, String> columns =
        PSContentStatusNotifyMap.build(
            7,
            11,
            "alice",
            5,
            6,
            7,
            true,
            toSqlDate(Instant.parse("2026-07-28T12:00:00Z")),
            toSqlDate(Instant.parse("2026-07-27T08:30:00Z")),
            3,
            toSqlDate(Instant.parse("2026-08-01T00:00:00Z")),
            toSqlDate(Instant.parse("2026-07-01T00:00:00Z")),
            toSqlDate(Instant.parse("2026-12-31T23:59:59Z")),
            toSqlDate(Instant.parse("2026-12-24T09:00:00Z")),
            toSqlDate(Instant.parse("2026-07-28T00:00:00Z")));

    assertEquals(15, columns.size(), "Legacy map must contain exactly 15 columns");
    assertEquals("11", columns.get(PSContentStatusNotifyMap.CONTENTSTATEID));
    assertEquals("alice", columns.get(PSContentStatusNotifyMap.CONTENTCHECKOUTUSERNAME));
    assertEquals("5", columns.get(PSContentStatusNotifyMap.CURRENTREVISION));
    assertEquals("6", columns.get(PSContentStatusNotifyMap.EDITREVISION));
    assertEquals("7", columns.get(PSContentStatusNotifyMap.TIPREVISION));
    assertEquals("Y", columns.get(PSContentStatusNotifyMap.REVISIONLOCK));
    assertNotNull(columns.get(PSContentStatusNotifyMap.LASTTRANSITIONDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.LASTTRANSITIONDATE).isEmpty());
    assertNotNull(columns.get(PSContentStatusNotifyMap.STATEENTEREDDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.STATEENTEREDDATE).isEmpty());
    assertEquals("3", columns.get(PSContentStatusNotifyMap.NEXTAGINGTRANSITION));
    assertNotNull(columns.get(PSContentStatusNotifyMap.NEXTAGINGDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.NEXTAGINGDATE).isEmpty());
    assertNotNull(columns.get(PSContentStatusNotifyMap.CONTENTSTARTDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.CONTENTSTARTDATE).isEmpty());
    assertNotNull(columns.get(PSContentStatusNotifyMap.CONTENTEXPIRYDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.CONTENTEXPIRYDATE).isEmpty());
    assertNotNull(columns.get(PSContentStatusNotifyMap.REMINDERDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.REMINDERDATE).isEmpty());
    assertNotNull(columns.get(PSContentStatusNotifyMap.REPEATEDAGINGTRANSSTARTDATE));
    assertFalse(columns.get(PSContentStatusNotifyMap.REPEATEDAGINGTRANSSTARTDATE).isEmpty());
    assertEquals("7", columns.get(PSContentStatusNotifyMap.CONTENTID));
  }

  @Test
  void buildLegacyColumnMap_handlesNullDatesAndEmptyUser() {
    // Null date inputs must remain null map values (legacy setDate / PSItemSummaryCache
    // contract) — never coerced to "".
    Map<String, String> columns =
        PSContentStatusNotifyMap.build(
            7, 11, "", 1, 1, 1, false, null, null, 0, null, null, null, null, null);

    assertEquals(15, columns.size());
    assertEquals("", columns.get(PSContentStatusNotifyMap.CONTENTCHECKOUTUSERNAME));
    assertEquals("N", columns.get(PSContentStatusNotifyMap.REVISIONLOCK));
    assertNull(columns.get(PSContentStatusNotifyMap.LASTTRANSITIONDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.STATEENTEREDDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.NEXTAGINGDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.CONTENTSTARTDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.CONTENTEXPIRYDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.REMINDERDATE));
    assertNull(columns.get(PSContentStatusNotifyMap.REPEATEDAGINGTRANSSTARTDATE));
  }

  @Test
  void formatDate_nullSafeAndNonEmptyForInstant() {
    // Intentional: null → null (not ""), distinct from PSWorkFlowUtils.DateString(null) → "".
    assertNull(PSContentStatusNotifyMap.formatDate(null));
    // 14:05 UTC → Calendar.HOUR is 2 (0–11 clock, same as PSWorkFlowUtils.DateString — not
    // HOUR_OF_DAY).
    String s =
        PSContentStatusNotifyMap.formatDate(toSqlDate(Instant.parse("2026-07-28T14:05:00Z")));
    assertNotNull(s);
    assertTrue(
        s.startsWith("7/28/2026 2:"),
        "expected mm/dd/yyyy H:… with Calendar.HOUR 0–11 for 14:05 UTC, got: " + s);
  }

  /**
   * Epoch millis → {@link Date}; format assertions rely on {@code TimeZone.setDefault(UTC)} in
   * {@link #beforeAll} (and reset in {@link #afterAll}). Do not change only one of those pieces.
   */
  private static Date toSqlDate(Instant instant) {
    return new Date(instant.toEpochMilli());
  }
}
