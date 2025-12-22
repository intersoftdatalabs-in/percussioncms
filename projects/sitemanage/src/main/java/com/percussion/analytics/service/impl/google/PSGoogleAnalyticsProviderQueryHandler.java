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
package com.percussion.analytics.service.impl.google;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.google.api.services.analyticsreporting.v4.model.*;
import com.percussion.analytics.data.IPSAnalyticsQueryResult;
import com.percussion.analytics.data.impl.PSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.analytics.service.impl.IPSAnalyticsProviderQueryHandler;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.utils.date.PSDateRange;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handler that does the actual query building and execution to the Google Analytics service. Sunny
 * Sal: "Google Analytics API is like a Bollywood plot—lots of twists!"
 */
public class PSGoogleAnalyticsProviderQueryHandler implements IPSAnalyticsProviderQueryHandler {

  private final IPSAnalyticsProviderService providerService;
  private static final Logger log =
      LogManager.getLogger(PSGoogleAnalyticsProviderQueryHandler.class);

  public PSGoogleAnalyticsProviderQueryHandler(IPSAnalyticsProviderService providerService) {
    this.providerService = providerService;
  }

  @Override
  public List<IPSAnalyticsQueryResult> getPageViewsByPathPrefix(
      String sitename, String pathPrefix, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    notEmpty(sitename);
    notNull(range);

    logPageViewsParameters(sitename, pathPrefix, range);

    range = PSGoogleAnalyticsProviderHelper.getInstance().createValidPSDateRange(range);

    var requestQuery = createQueryForPageViewsByPathPrefix(sitename, pathPrefix, range);
    var entries = executeQuery(sitename, requestQuery);

    return getResultsForPageViewsByPathPrefix(sitename, entries);
  }

  private void logPageViewsParameters(String sitename, String pathPrefix, PSDateRange range) {
    log.debug("getPageViewsByPathPrefix: sitename = '{}', pathPrefix = '{}'", sitename, pathPrefix);
    log.debug("Date begin: {}", range.getStart());
    log.debug("Date end: {}", range.getEnd());
    log.debug("Date getGranularity: {}", range.getGranularity());
  }

  private void logResultsForPageViewsByPathPrefix(List<IPSAnalyticsQueryResult> results) {
    int i = 0;
    for (var r : results) {
      i++;
      log.debug("[{}] ({}) {}", i, FIELD_PAGE_PATH, r.getString(FIELD_PAGE_PATH));
      log.debug("[{}] ({}) {}", i, FIELD_PAGEVIEWS, r.getInt(FIELD_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_UNIQUE_PAGEVIEWS, r.getInt(FIELD_UNIQUE_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_DATE, r.getDate(FIELD_DATE));
    }
    log.debug("PageViewsByPathPrefix result size: {}", results.size());
  }

  private List<IPSAnalyticsQueryResult> getResultsForPageViewsByPathPrefix(
      String sitename, Report report) throws PSAnalyticsProviderException {
    var results = new ArrayList<IPSAnalyticsQueryResult>();
    var header = report.getColumnHeader();
    var dimensionHeaders = header.getDimensions();
    var metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
    var rows = report.getData().getRows();

    if (rows != null) {
      for (var row : rows) {
        var dimensions = row.getDimensions();
        var metrics = row.getMetrics();
        var result = new PSAnalyticsQueryResult();

        for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
          log.debug("{}:{}", dimensionHeaders.get(i), dimensions.get(i));
          if (dimensionHeaders.get(i).equalsIgnoreCase("ga:date")) {
            result.put(
                FIELD_DATE,
                PSGoogleAnalyticsProviderHelper.getInstance()
                    .parseDate(String.valueOf(dimensions.get(i))));
          }
          if (dimensionHeaders.get(i).equalsIgnoreCase("ga:pagePath")) {
            result.put(FIELD_PAGE_PATH, dimensions.get(i) != null ? dimensions.get(i) : "");
          }
        }

        for (var values : metrics) {
          for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
            log.debug("{}: {}", metricHeaders.get(k).getName(), values.getValues().get(k));
            if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:pageviews")) {
              result.put(
                  FIELD_PAGEVIEWS, Integer.parseInt(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:uniquePageviews")) {
              result.put(
                  FIELD_UNIQUE_PAGEVIEWS,
                  Integer.parseInt(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:date")) {
              result.put(
                  FIELD_DATE,
                  PSGoogleAnalyticsProviderHelper.getInstance()
                      .parseDate(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:pagePath")) {
              result.put(
                  FIELD_PAGE_PATH,
                  values.getValues().get(k) != null ? values.getValues().get(k) : "");
            }
          }
        }
        results.add(result);
      }
    }
    logResultsForPageViewsByPathPrefix(results);
    return results;
  }

  @Override
  public List<IPSAnalyticsQueryResult> getVisitsViewsBySite(String sitename, PSDateRange range)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    notEmpty(sitename);
    notNull(range);

    logPageViewsParameters(sitename, null, range);
    var requestQuery = createQueryForVisitsViews(range);
    var entries = executeQuery(sitename, requestQuery);

    return getResultsForVisitsViewsBySite(sitename, entries);
  }

  private List<IPSAnalyticsQueryResult> getResultsForVisitsViewsBySite(
      String sitename, Report report) throws PSAnalyticsProviderException {
    var results = new ArrayList<IPSAnalyticsQueryResult>();
    var header = report.getColumnHeader();
    var dimensionHeaders = header.getDimensions();
    var metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
    var rows = report.getData().getRows();

    if (rows != null) {
      for (var row : rows) {
        var dimensions = row.getDimensions();
        var metrics = row.getMetrics();
        var result = new PSAnalyticsQueryResult();

        for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
          log.debug("{}:{}", dimensionHeaders.get(i), dimensions.get(i));
          if (dimensionHeaders.get(i).equalsIgnoreCase("ga:date")) {
            result.put(
                FIELD_DATE,
                PSGoogleAnalyticsProviderHelper.getInstance()
                    .parseDate(String.valueOf(dimensions.get(i))));
          }
          if (dimensionHeaders.get(i).equalsIgnoreCase("ga:pagePath")) {
            result.put(FIELD_PAGE_PATH, dimensions.get(i) != null ? dimensions.get(i) : "");
          }
        }

        for (var values : metrics) {
          for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
            log.debug("{}:{}", metricHeaders.get(k).getName(), values.getValues().get(k));
            if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:newVisits")) {
              result.put(FIELD_NEW_VISITS, values.getValues().get(k));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:visits")) {
              result.put(FIELD_VISITS, values.getValues().get(k));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:pageviews")) {
              result.put(
                  FIELD_PAGEVIEWS, Integer.parseInt(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:uniquePageviews")) {
              result.put(
                  FIELD_UNIQUE_PAGEVIEWS,
                  Integer.parseInt(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:date")) {
              result.put(
                  FIELD_DATE,
                  PSGoogleAnalyticsProviderHelper.getInstance()
                      .parseDate(String.valueOf(values.getValues().get(k))));
            } else if (metricHeaders.get(k).getName().equalsIgnoreCase("ga:pagePath")) {
              result.put(
                  FIELD_PAGE_PATH,
                  values.getValues().get(k) != null ? values.getValues().get(k) : "");
            }
          }
        }
        results.add(result);
      }
    }
    logVisitsViewsBySiteResults(results);
    return results;
  }

  private void logVisitsViewsBySiteResults(List<IPSAnalyticsQueryResult> results) {
    int i = 0;
    for (var r : results) {
      i++;
      log.debug("[{}] ({}) {}", i, FIELD_NEW_VISITS, r.getString(FIELD_NEW_VISITS));
      log.debug("[{}] ({}) {}", i, FIELD_VISITS, r.getString(FIELD_VISITS));
      log.debug("[{}] ({}) {}", i, FIELD_PAGEVIEWS, r.getInt(FIELD_PAGEVIEWS));
      log.debug("[{}] ({}) {}", i, FIELD_UNIQUE_PAGEVIEWS, r.getInt(FIELD_UNIQUE_PAGEVIEWS));
      log.debug("[{}] ({})", i, r.getDate(FIELD_DATE));
    }
    log.debug("VisitsViewsBySite result size: {}", results.size());
  }

  private ReportRequest createQueryForPageViewsByPathPrefix(
      String siteName, String pathPrefix, PSDateRange range) throws PSAnalyticsProviderException {
    var request = PSGoogleAnalyticsProviderHelper.getInstance().createNewDataQuery(range);
    request.setDimensions(
        Arrays.asList(new Dimension().setName("ga:date"), new Dimension().setName("ga:pagePath")));
    request.setMetrics(
        Arrays.asList(
            new Metric().setExpression("ga:pageviews"),
            new Metric().setExpression("ga:uniquePageviews")));
    request.setOrderBys(Collections.singletonList(new OrderBy().setFieldName("ga:date")));

    var pagePathFilter = getPagePathFilter(siteName, pathPrefix);
    if (pagePathFilter != null) {
      request.setFiltersExpression(pagePathFilter);
    }
    return request;
  }

  private ReportRequest createQueryForVisitsViews(PSDateRange range)
      throws PSAnalyticsProviderException {
    range = PSGoogleAnalyticsProviderHelper.getInstance().createValidPSDateRange(range);
    var request = PSGoogleAnalyticsProviderHelper.getInstance().createNewDataQuery(range);
    var ordering = new OrderBy().setFieldName("ga:date");
    request.setDimensions(Collections.singletonList(new Dimension().setName("ga:date")));
    request.setMetrics(
        Arrays.asList(
            new Metric().setExpression("ga:pageviews"),
            new Metric().setExpression("ga:uniquePageviews"),
            new Metric().setExpression("ga:visits"),
            new Metric().setExpression("ga:newVisits")));
    request.setOrderBys(Collections.singletonList(ordering));
    return request;
  }

  private String getPagePathFilter(String siteName, String pathPrefix) {
    if (StringUtils.isBlank(pathPrefix)) return null;
    var pagePath = pathPrefix;
    if (!pathPrefix.startsWith("//")) {
      pagePath = "/" + pathPrefix;
    }
    pagePath = pagePath.replace("//Sites/" + siteName, "");
    var pagePathFilter = "ga:pagePath=~";
    if (pagePath.length() >= 126) {
      pagePath = pagePath.substring(pagePath.length() - 126);
    } else {
      pagePathFilter += "^";
    }
    pagePathFilter += pagePath + "/*";
    return pagePathFilter;
  }

  private Report executeQuery(String sitename, ReportRequest requestQuery)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    var config = providerService.loadConfig(false);
    if (config == null) {
      throw new PSAnalyticsProviderException(
          "Analytics has not been setup yet.", CAUSETYPE.ANALYTICS_NOT_CONFIG);
    }
    var uid = config.getUserid();
    var pwd = config.getPassword();
    var pid = getProfileId(sitename);
    return executeGoogleQuery(requestQuery, pid, uid, pwd);
  }

  private String getProfileId(String sitename)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    var profileId = providerService.getProfileId(sitename);
    if (profileId == null) {
      var builder = new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
      var msg = "No profile set for site <" + sitename + ">.";
      builder.reject(CAUSETYPE.NO_PROFILE.toString(), msg).throwIfInvalid();
    }
    return profileId;
  }

  private synchronized Report executeGoogleQuery(
      ReportRequest requestQuery, String pid, String uid, String pwd)
      throws PSAnalyticsProviderException {
    Report resultReport = null;
    try {
      var analyticsReporting =
          PSGoogleAnalyticsProviderHelper.getInstance().initializeAnalyticsReporting(uid, pwd);
      requestQuery.setViewId(pid);
      var requests = new ArrayList<ReportRequest>();
      requests.add(requestQuery);
      var getReport = new GetReportsRequest().setReportRequests(requests);
      var response = analyticsReporting.reports().batchGet(getReport).execute();
      for (var report : response.getReports()) {
        resultReport = report;
      }
    } catch (Exception e) {
      log.error(e);
      throw new PSAnalyticsProviderException(e.getMessage(), e);
    }
    return resultReport;
  }
}
