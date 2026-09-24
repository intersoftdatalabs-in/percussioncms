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

import com.percussion.services.publisher.IPSPubStatus.EndingState;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * HQL fragments and page limits for publish-log windows. The day bound and
 * failures-only ending states are part of the query, not a post-load trim.
 */
public final class PSPubStatusLogQuery {

  public static final String FROM_DATE = "fromDate";
  public static final String ENDING_STATES = "endingStates";

  private PSPubStatusLogQuery() {}

  /** Inclusive lower bound. {@code days == -1} means no date predicate. */
  public static Date fromDate(int days, Date now) {
    if (days == -1) {
      return null;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.DAY_OF_YEAR, -days);
    return cal.getTime();
  }

  /**
   * Ending-state ordinals treated as failures by the Logs section
   * (aborted, canceled, completed with failures, restart needed).
   */
  public static List<Integer> failureEndingOrdinals() {
    return List.of(
        EndingState.ABORTED.ordinal(),
        EndingState.CANCELED_BY_USER.ordinal(),
        EndingState.COMPLETED_W_FAILURE.ordinal(),
        EndingState.RESTARTNEEDED.ordinal());
  }

  /**
   * Appends an inclusive start-date predicate when {@code days != -1} and an
   * ending-state predicate when {@code failuresOnly} is true.
   */
  public static void appendWindow(StringBuilder hql, String alias, int days, boolean failuresOnly) {
    if (days != -1) {
      hql.append(" and ").append(alias).append(".startDate >= :").append(FROM_DATE);
    }
    if (failuresOnly) {
      hql.append(" and ").append(alias).append(".endingStatus in (:").append(ENDING_STATES).append(")");
    }
  }

  /** {@code firstResult} is null when skip is not positive; {@code maxResults} is null when max is -1. */
  public static Limits limits(int skipCount, int maxCount) {
    Integer first = skipCount > 0 ? skipCount : null;
    Integer max = maxCount != -1 ? maxCount : null;
    return new Limits(first, max);
  }

  public record Limits(Integer firstResult, Integer maxResults) {}
}
