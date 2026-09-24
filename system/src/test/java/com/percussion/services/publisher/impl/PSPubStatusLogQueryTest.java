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
package com.percussion.services.publisher.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.publisher.IPSPubStatus.EndingState;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class PSPubStatusLogQueryTest {

  @Test
  void dayWindowIsInclusiveStartDatePredicate() {
    StringBuilder hql = new StringBuilder("from PSPubStatus p where p.hidden is null");
    PSPubStatusLogQuery.appendWindow(hql, "p", 1, false);
    String sql = hql.toString();
    assertTrue(sql.contains("p.startDate >= :fromDate"));
    assertFalse(sql.contains("endingStatus"));

    Date now = new Date(1_700_000_000_000L);
    Date from = PSPubStatusLogQuery.fromDate(1, now);
    Calendar expected = Calendar.getInstance();
    expected.setTime(now);
    expected.add(Calendar.DAY_OF_YEAR, -1);
    assertEquals(expected.getTime(), from);
    assertNull(PSPubStatusLogQuery.fromDate(-1, now));
  }

  @Test
  void unlimitedDaysOmitsDatePredicate() {
    StringBuilder hql = new StringBuilder("select status from PSPubStatus status where 1=1");
    PSPubStatusLogQuery.appendWindow(hql, "status", -1, false);
    assertEquals("select status from PSPubStatus status where 1=1", hql.toString());
  }

  @Test
  void failuresOnlyAddsEndingStatesMatchingLogFailures() {
    StringBuilder hql = new StringBuilder("from PSPubStatus p where p.hidden is null");
    PSPubStatusLogQuery.appendWindow(hql, "p", 5, true);
    assertTrue(hql.toString().contains("p.startDate >= :fromDate"));
    assertTrue(hql.toString().contains("p.endingStatus in (:endingStates)"));

    List<Integer> ordinals = PSPubStatusLogQuery.failureEndingOrdinals();
    assertTrue(ordinals.contains(EndingState.ABORTED.ordinal()));
    assertTrue(ordinals.contains(EndingState.CANCELED_BY_USER.ordinal()));
    assertTrue(ordinals.contains(EndingState.COMPLETED_W_FAILURE.ordinal()));
    assertTrue(ordinals.contains(EndingState.RESTARTNEEDED.ordinal()));
    assertFalse(ordinals.contains(EndingState.COMPLETED.ordinal()));
    assertFalse(ordinals.contains(EndingState.STARTED.ordinal()));
  }

  @Test
  void limitsMapSkipAndMaxOntoTheQuery() {
    PSPubStatusLogQuery.Limits page = PSPubStatusLogQuery.limits(4, 20);
    assertEquals(4, page.firstResult());
    assertEquals(20, page.maxResults());

    PSPubStatusLogQuery.Limits open = PSPubStatusLogQuery.limits(0, -1);
    assertNull(open.firstResult());
    assertNull(open.maxResults());
  }
}
