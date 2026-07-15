/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.analytics.service.impl.google;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Filter;
import com.google.analytics.data.v1beta.Filter.StringFilter;
import com.google.analytics.data.v1beta.Filter.StringFilter.MatchType;
import com.google.analytics.data.v1beta.FilterExpression;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.OrderBy;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.percussion.analytics.data.IPSAnalyticsQueryResult;
import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.data.impl.PSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.analytics.service.impl.IPSAnalyticsProviderQueryHandler;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.utils.date.PSDateRange;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handler that does the actual query building and execution to the Google Analytics service.
 * Refactored to support GA4 Data API v1beta.
 *
 * @author erikserating
 */
public class PSGoogleAnalyticsProviderQueryHandler implements IPSAnalyticsProviderQueryHandler {
  public PSGoogleAnalyticsProviderQueryHandler(IPSAnalyticsProviderService providerService) {
    this.providerService = providerService;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.percussion.analytics.service.IPSAnalyticsProviderQueryService#
   * getPageViewsByPathPrefix( java.lang.String, java.lang.String,
   * com.percussion.utils.date.PSDateRange)
   */
  public List<IPSAnalyticsQueryResult> getPageViewsByPathPrefix(
      String sitename, String pathPrefix, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    notEmpty(sitename);
    notNull(range);

    logPageViewsParameters(sitename, pathPrefix, range);

    range = PSGoogleAnalyticsProviderHelper.getInstance().createValidPSDateRange(range);

    RunReportRequest requestQuery =
        createQueryForPageViewsByPathPrefix(sitename, pathPrefix, range);

    RunReportResponse response = executeQuery(sitename, requestQuery);

    return getResultsForPageViewsByPathPrefix(sitename, response);
  }

  private void logPageViewsParameters(String sitename, String pathPrefix, PSDateRange range) {
    log.debug("getPageViewsByPathPrefix: sitename = '{}', pathPrefix = '{}'", sitename, pathPrefix);
    log.debug("Date begin: {}", range.getStart());
    log.debug("Date end: {}", range.getEnd());
    log.debug("Date getGranularity: {}", range.getGranularity());
  }

  private void logResultsForPageViewsByPathPrefix(List<IPSAnalyticsQueryResult> results) {
    int i = 0;
    for (IPSAnalyticsQueryResult r : results) {
      i++;
      log.debug("[{}] ({}) {}", i, FIELD_PAGE_PATH, r.getString(FIELD_PAGE_PATH));
      log.debug("[{}] ({}) {}", i, FIELD_PAGEVIEWS, r.getInt(FIELD_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_UNIQUE_PAGEVIEWS, r.getInt(FIELD_UNIQUE_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_DATE, r.getDate(FIELD_DATE));
    }

    log.debug("PageViewsByPathPrefix result size: {}", results.size());
  }

  private List<IPSAnalyticsQueryResult> getResultsForPageViewsByPathPrefix(
      String sitename, RunReportResponse report) throws PSAnalyticsProviderException {
    List<IPSAnalyticsQueryResult> results = new ArrayList<>();
    if (report == null) return results;

    for (Row row : report.getRowsList()) {
      PSAnalyticsQueryResult result = new PSAnalyticsQueryResult();

      // Dimensions: 0: date, 1: pagePath
      String dateStr = row.getDimensionValues(0).getValue();
      String path = row.getDimensionValues(1).getValue();

      // Metrics: 0: screenPageViews, 1: sessions (approx for unique pageviews)
      String pageViewsStr = row.getMetricValues(0).getValue();
      String sessionsStr = row.getMetricValues(1).getValue();

      result.put(FIELD_DATE, PSGoogleAnalyticsProviderHelper.getInstance().parseDate(dateStr));
      result.put(FIELD_PAGE_PATH, path != null ? path : "");
      result.put(FIELD_PAGEVIEWS, Integer.parseInt(pageViewsStr));
      result.put(FIELD_UNIQUE_PAGEVIEWS, Integer.parseInt(sessionsStr));

      results.add(result);
    }

    logResultsForPageViewsByPathPrefix(results);

    return results;
  }

  /*
   * (non-Javadoc)
   *
   * @seecom.percussion.analytics.service.IPSAnalyticsProviderQueryService#
   * getVisitsViewsBySite( java.lang.String,
   * com.percussion.utils.date.PSDateRange)
   */
  public List<IPSAnalyticsQueryResult> getVisitsViewsBySite(String sitename, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    notEmpty(sitename);
    notNull(range);

    logPageViewsParameters(sitename, null, range);
    range = PSGoogleAnalyticsProviderHelper.getInstance().createValidPSDateRange(range);
    RunReportRequest requestQuery = createQueryForVisitsViews(range);
    RunReportResponse response = executeQuery(sitename, requestQuery);

    return getResultsForVisitsViewsBySite(sitename, response);
  }

  private List<IPSAnalyticsQueryResult> getResultsForVisitsViewsBySite(
      String sitename, RunReportResponse report) throws PSAnalyticsProviderException {
    List<IPSAnalyticsQueryResult> results = new ArrayList<>();
    if (report == null) return results;

    for (Row row : report.getRowsList()) {
      PSAnalyticsQueryResult result = new PSAnalyticsQueryResult();

      // Dimensions: 0: date
      String dateStr = row.getDimensionValues(0).getValue();

      // Metrics: 0: screenPageViews, 1: sessions, 2: newUsers
      String pageViewsStr = row.getMetricValues(0).getValue();
      String sessionsStr = row.getMetricValues(1).getValue();
      String newUsersStr = row.getMetricValues(2).getValue();

      result.put(FIELD_DATE, PSGoogleAnalyticsProviderHelper.getInstance().parseDate(dateStr));
      result.put(FIELD_PAGEVIEWS, Integer.parseInt(pageViewsStr));
      result.put(FIELD_UNIQUE_PAGEVIEWS, Integer.parseInt(sessionsStr));
      result.put(FIELD_VISITS, sessionsStr);
      result.put(FIELD_NEW_VISITS, newUsersStr);

      results.add(result);
    }

    logVisitsViewsBySiteResults(results);
    return results;
  }

  private void logVisitsViewsBySiteResults(List<IPSAnalyticsQueryResult> results) {
    int i = 0;
    for (IPSAnalyticsQueryResult r : results) {
      i++;
      log.debug("[{}] ({}) {}", i, FIELD_NEW_VISITS, r.getString(FIELD_NEW_VISITS));
      log.debug("[{}] ({}) {}", i, FIELD_VISITS, r.getString(FIELD_VISITS));
      log.debug("[{}] ({}) {}", i, FIELD_PAGEVIEWS, r.getInt(FIELD_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_UNIQUE_PAGEVIEWS, r.getInt(FIELD_UNIQUE_PAGEVIEWS));
      log.debug("[{}] ({})", i, r.getDate(FIELD_DATE));
    }
    log.debug("VisitsViewsBySite result size: {}", results.size());
  }

  private RunReportRequest createQueryForPageViewsByPathPrefix(
      String siteName, String pathPrefix, PSDateRange range) throws PSAnalyticsProviderException {

    String startDate =
        PSGoogleAnalyticsProviderHelper.getInstance().getDateFormat().format(range.getStart());
    String endDate =
        PSGoogleAnalyticsProviderHelper.getInstance().getDateFormat().format(range.getEnd());

    DateRange dateRange =
        DateRange.newBuilder().setStartDate(startDate).setEndDate(endDate).build();

    RunReportRequest.Builder requestBuilder =
        RunReportRequest.newBuilder()
            .addDateRanges(dateRange)
            .addDimensions(Dimension.newBuilder().setName("date"))
            .addDimensions(Dimension.newBuilder().setName("pagePath"))
            .addMetrics(Metric.newBuilder().setName("screenPageViews"))
            .addMetrics(Metric.newBuilder().setName("sessions"))
            .addOrderBys(
                OrderBy.newBuilder()
                    .setDimension(OrderBy.DimensionOrderBy.newBuilder().setDimensionName("date")));

    FilterExpression pagePathFilter = getPagePathFilter(siteName, pathPrefix);
    if (pagePathFilter != null) {
      requestBuilder.setDimensionFilter(pagePathFilter);
    }
    return requestBuilder.build();
  }

  private RunReportRequest createQueryForVisitsViews(PSDateRange range)
      throws PSAnalyticsProviderException {
    String startDate =
        PSGoogleAnalyticsProviderHelper.getInstance().getDateFormat().format(range.getStart());
    String endDate =
        PSGoogleAnalyticsProviderHelper.getInstance().getDateFormat().format(range.getEnd());

    DateRange dateRange =
        DateRange.newBuilder().setStartDate(startDate).setEndDate(endDate).build();

    return RunReportRequest.newBuilder()
        .addDateRanges(dateRange)
        .addDimensions(Dimension.newBuilder().setName("date"))
        .addMetrics(Metric.newBuilder().setName("screenPageViews"))
        .addMetrics(Metric.newBuilder().setName("sessions"))
        .addMetrics(Metric.newBuilder().setName("newUsers"))
        .addOrderBys(
            OrderBy.newBuilder()
                .setDimension(OrderBy.DimensionOrderBy.newBuilder().setDimensionName("date")))
        .build();
  }

  private FilterExpression getPagePathFilter(String siteName, String pathPrefix) {
    if (StringUtils.isBlank(pathPrefix)) return null;

    /*
     * Path prefix is the full URL so before setting the filter, we need to
     * remove "//Sites/{sitename} prefix". Example:
     * //Sites/schoolofdesign.edu/admissions/graduate will result in
     * /admissions/graduate. Sometimes URL will begin with "//", and others
     * with a single "/"
     */
    String pagePath = pathPrefix;

    if (!pathPrefix.startsWith("//")) {
      pagePath = "/" + pathPrefix;
    }

    pagePath = pagePath.replace("//Sites/" + siteName, "");

    return FilterExpression.newBuilder()
        .setFilter(
            Filter.newBuilder()
                .setFieldName("pagePath")
                .setStringFilter(
                    StringFilter.newBuilder()
                        .setMatchType(MatchType.BEGINS_WITH)
                        .setValue(pagePath)))
        .build();
  }

  /**
   * Execute the passed in query against GA4 Data API.
   *
   * @param sitename the site name is required so we can retrieve the proper property to use. Cannot
   *     be <code>null</code> or empty.
   * @param requestQuery the Google DataQuery. Cannot be <code>null</code>.
   * @return response data, never <code>null</code>, may be empty.
   * @throws PSAnalyticsProviderException on any error that occurs while executing the query.
   */
  private RunReportResponse executeQuery(String sitename, RunReportRequest requestQuery)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    // Get user ID and password
    PSAnalyticsProviderConfig config = providerService.loadConfig(false);
    if (config == null) {
      throw new PSAnalyticsProviderException(
          "Analytics has not been setup yet.", CAUSETYPE.ANALYTICS_NOT_CONFIG);
    }
    String uid = config.getUserid();
    String pwd = config.getPassword();

    String pid = getProfileId(sitename);
    return executeGoogleQuery(requestQuery, pid, uid, pwd);
  }

  private String getProfileId(String sitename)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    String profileId = providerService.getProfileId(sitename);
    if (profileId == null) {
      PSValidationErrorsBuilder builder =
          new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
      String msg = "No property set for site <" + sitename + ">.";
      builder.reject(CAUSETYPE.NO_PROFILE.toString(), msg).throwIfInvalid();
    }

    return profileId;
  }

  private synchronized RunReportResponse executeGoogleQuery(
      RunReportRequest requestQuery, String pid, String uid, String pwd)
      throws PSAnalyticsProviderException {

    try (BetaAnalyticsDataClient analyticsDataClient =
        PSGoogleAnalyticsProviderHelper.getInstance().getGa4DataClient(uid, pwd)) {
      if (analyticsDataClient == null) {
        throw new PSAnalyticsProviderException("Failed to initialize GA4 Data API client.");
      }

      RunReportRequest finalRequest = requestQuery.toBuilder().setProperty(pid).build();

      return analyticsDataClient.runReport(finalRequest);
    } catch (Exception e) {
      log.error(e);
      throw new PSAnalyticsProviderException(e.getMessage(), e);
    }
  }

  /** Analytics provider service, initialized in ctor, never <code>null</code> after that. */
  private final IPSAnalyticsProviderService providerService;

  /** Logger for this class */
  private static final Logger log =
      LogManager.getLogger(PSGoogleAnalyticsProviderQueryHandler.class);
}
