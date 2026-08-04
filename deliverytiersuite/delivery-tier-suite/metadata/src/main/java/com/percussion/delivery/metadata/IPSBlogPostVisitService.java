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
package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import com.percussion.delivery.metadata.data.PSVisitQuery;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Service contract for the DTS blog-post visit tracker. Periodically flushes tracked page visits
 * and piggy-backs cookie-consent logging on the same scheduled runnable to avoid spawning an
 * additional thread. Implementations are typically Spring-managed singletons configured in the DTS
 * {@code beans.xml}.
 */
public interface IPSBlogPostVisitService {
  /**
   * Initial delay (in seconds) before the scheduled visit-flush runnable executes after startup.
   */
  public static final int INTIAL_DELAY_SECONDS = 0;

  /** Interval (in seconds) between successive visit-flush runs by the scheduled runnable. */
  public static final int SAVE_INTERVAL_SECONDS = 60;

  /**
   * Returns top visited pages within the given time period.
   *
   * @param visitQuery visit query object containing the time window, section path and other filter
   *     parameters; may not be <code>null</code>.
   * @return a list of page paths sorted by visit count, never <code>null</code>, may be empty.
   * @throws Exception if the underlying persistence layer fails.
   */
  public List<String> getTopVisitedBlogPosts(PSVisitQuery visitQuery) throws Exception;

  /**
   * Returns top visited pages within the given time period.
   *
   * @param pagePath The page path to track.
   */
  public void trackBlogPost(String pagePath);

  /**
   * Tracks a cookie consent query. This method is added to the blog post visit service to piggyback
   * off of the existing Runnable to avoid expenses of creating a new thread to post updates in
   * bulk.
   *
   * @param query - obj with values to save.
   */
  public void logCookieConsentEntry(PSCookieConsentQuery query);

  /**
   * Deletes stored visit entries for the supplied page paths.
   *
   * @param pagepaths collection of page path strings whose visit records should be removed; may not
   *     be <code>null</code> and may be empty.
   */
  public void delete(Collection<String> pagepaths);

  /**
   * Converts the supplied textual limit to an integer, applying any default upper bound enforced by
   * the implementation.
   *
   * @param limit the textual representation of the limit; may be <code>null</code> or empty.
   * @return the resolved integer limit value, never negative.
   */
  public int convertToLimit(String limit);

  /**
   * Reports whether the visit-tracking scheduler is currently running.
   *
   * @return <code>true</code> if the scheduler is running, <code>false</code> otherwise.
   */
  public boolean visitSchedulerStatus();

  /**
   * Updates stored visit records after a site has been renamed so they reference the new site name.
   *
   * @param prevSiteName the previous site name; may be <code>null</code>.
   * @param newSiteName the new site name; may be <code>null</code>.
   */
  public void updatePostsAfterSiteRename(String prevSiteName, String newSiteName);

  /**
   * Starts the visit-tracking scheduled runnable. Idempotent: subsequent calls when the scheduler
   * is already running should be no-ops.
   *
   * @throws Exception if the scheduler cannot be started due to an underlying failure.
   */
  public void startScheduler() throws Exception;

  /** Time windows supported by the visit-tracking service when ranking most-visited pages. */
  public enum TIMEPERIOD {
    /** Restrict the window to today only. */
    TODAY(1),
    /** Restrict the window to the last seven days. */
    WEEK(7),
    /** Restrict the window to the last thirty days. */
    MONTH(30),
    /** Restrict the window to the last year. */
    YEAR(365),
    /** No time restriction; consider the full history. */
    ALLTIME(-1);
    private int days;

    private TIMEPERIOD(int days) {
      this.days = days;
    }

    /**
     * Returns the numeric day window represented by this enum value.
     *
     * @return the day count, or {@code -1} for {@link #ALLTIME}.
     */
    public int getDays() {
      return days;
    }

    /**
     * Resolves a {@link TIMEPERIOD} from its textual name using a case-insensitive comparison.
     *
     * @param timePeriod the textual name to resolve; may be blank.
     * @return the matching enum value, or {@code null} if the name is blank or does not match any
     *     declared constant.
     */
    public static TIMEPERIOD fromName(String timePeriod) {
      if (StringUtils.isBlank(timePeriod)) {
        return null;
      }
      TIMEPERIOD res = null;
      for (TIMEPERIOD val : values()) {
        if (timePeriod.equalsIgnoreCase(val.name())) {
          res = val;
          break;
        }
      }
      return res;
    }
  }
}
