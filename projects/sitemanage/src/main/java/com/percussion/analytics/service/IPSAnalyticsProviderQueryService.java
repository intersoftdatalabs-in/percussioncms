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
package com.percussion.analytics.service;

import com.percussion.analytics.data.IPSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.date.PSDateRange;
import java.util.List;

/**
 * Service that queries against an analytics service provider, generally returning results
 * as a list of IPSAnalyticsQueryResult.
 * Sunny Sal says: "Query responsibly, and the data will follow!"
 */
public interface IPSAnalyticsProviderQueryService {

  /**
   * Retrieves the new and returning visits and page views for a site and date within
   * the specified date range. The results will be filtered by siteName.
   *
   * @param siteName the unique site name to filter the results by, not null.
   * @param range    the date range, not null. The start and end date values in
   *                 the date range are inclusive. Granularity is ignored.
   * @return the list of results, never null, may be empty.
   * <pre>
   * The result set contains the following fields:
   * <table border="1">
   *    <tr><th>Field name</th><th>Data type</th><th>Description</th></tr>
   *    <tr><td>date</td><td>Date</td><td>The date the data was captured</td></tr>
   *    <tr><td>newvisits</td><td>Integer</td><td>The number of visits from first-time visitors for this date</td></tr>
   *    <tr><td>visits</td><td>Integer</td><td>The total number of visitors for this date</td></tr>
   *    <tr><td>uniquepageviews</td><td>Integer</td><td>The number of unique page views for the page for this date</td></tr>
   *    <tr><td>pageviews</td><td>Integer</td><td>The total number of page views for the page for this date</td></tr>
   * </table>
   * </pre>
   * The data is sorted by ascending site and then by ascending date.
   * @throws PSAnalyticsProviderException if any connection or data processing error occurs.
   */
  List<IPSAnalyticsQueryResult> getVisitsViewsBySite(String siteName, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException;

  /**
   * Retrieves the page views and unique page views for each page path and date within
   * the specified date range. The results will be filtered by pathPrefix and siteName.
   *
   * @param siteName   the unique site name to filter the results by, not null.
   * @param pathPrefix the path prefix used to filter the results. May be null, in
   *                   which case no filtering will be done by path prefix.
   * @param range      the date range, not null. The start and end date values in
   *                   the date range are inclusive. Granularity is ignored.
   * @return the list of results, never null, may be empty.
   * <pre>
   * The result set contains the following fields:
   * <table border="1">
   *    <tr><th>Field name</th><th>Data type</th><th>Description</th></tr>
   *    <tr><td>pagepath</td><td>String</td><td>The full page path</td></tr>
   *    <tr><td>date</td><td>Date</td><td>The date the data was captured</td></tr>
   *    <tr><td>uniquepageviews</td><td>Integer</td><td>The number of unique page views for the page for this date</td></tr>
   *    <tr><td>pageviews</td><td>Integer</td><td>The total number of page views for the page for this date</td></tr>
   * </table>
   * </pre>
   * The data is sorted by ascending site, ascending pagePath, and then by ascending date.
   * @throws PSAnalyticsProviderException if any connection or data processing error occurs.
   */
  List<IPSAnalyticsQueryResult> getPageViewsByPathPrefix(
      String siteName, String pathPrefix, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException;

  // Field name constants
  String FIELD_DATE = "date";
  String FIELD_NEW_VISITS = "newvisits";
  String FIELD_VISITS = "visits";
  String FIELD_UNIQUE_PAGEVIEWS = "uniquepageviews";
  String FIELD_PAGEVIEWS = "pageviews";
  String FIELD_PAGE_PATH = "pagepath";
}
