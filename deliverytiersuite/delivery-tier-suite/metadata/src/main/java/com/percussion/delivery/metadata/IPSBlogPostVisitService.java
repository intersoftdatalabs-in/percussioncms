/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import com.percussion.delivery.metadata.data.PSVisitQuery;

/**
 * Service for blog post visit tracking and cookie consent logging.
 */
public interface IPSBlogPostVisitService {

    int INTIAL_DELAY_SECONDS = 0;
    int SAVE_INTERVAL_SECONDS = 60;

    /**
     * Returns top visited pages within the given time period.
     * @param visitQuery visit query object.
     * @return list of page paths.
     * @throws Exception on query parsing error.
     */
    List<String> getTopVisitedBlogPosts(PSVisitQuery visitQuery) throws Exception;

    /**
     * Tracks a blog post visit for the given page path.
     * @param pagePath the page path to track.
     */
    void trackBlogPost(String pagePath);

    /**
     * Tracks a cookie consent query.
     * Piggybacks off the existing Runnable to avoid extra thread creation.
     * @param query the consent query to log.
     */
    void logCookieConsentEntry(PSCookieConsentQuery query);

    /**
     * Deletes blog post visits for the given page paths.
     * @param pagepaths collection of page paths.
     */
    void delete(Collection<String> pagepaths);

    /**
     * Converts a limit string to an integer.
     * @param limit the limit string.
     * @return the integer limit.
     */
    int convertToLimit(String limit);

    /**
     * Returns true if the visit scheduler is running.
     * @return true if running.
     */
    boolean visitSchedulerStatus();

    /**
     * Updates blog post visits after a site rename.
     * @param prevSiteName previous site name.
     * @param newSiteName new site name.
     */
    void updatePostsAfterSiteRename(String prevSiteName, String newSiteName);

    /**
     * Starts the visit scheduler.
     * @throws Exception if scheduler fails to start.
     */
    void startScheduler() throws Exception;

    /**
     * Time period enum for visit queries.
     */
    enum TIMEPERIOD {
        TODAY(1), WEEK(7), MONTH(30), YEAR(365), ALLTIME(-1);

        private final int days;

        TIMEPERIOD(int days) {
            this.days = days;
        }

        public int getDays() {
            return days;
        }

        public static TIMEPERIOD fromName(String timePeriod) {
            if (StringUtils.isBlank(timePeriod)) {
                return null;
            }
            for (var val : values()) {
                if (timePeriod.equalsIgnoreCase(val.name())) {
                    return val;
                }
            }
            return null;
        }
    }
}
