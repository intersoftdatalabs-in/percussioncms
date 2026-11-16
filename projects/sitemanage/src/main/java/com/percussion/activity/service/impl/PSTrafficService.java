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
package com.percussion.activity.service.impl;

import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_STATE_ARCHIVE;
import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_STATE_LIVE;
import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_TAKE_DOWN_TRANSITION;

import com.percussion.activity.data.PSContentTraffic;
import com.percussion.activity.data.PSContentTrafficRequest;
import com.percussion.activity.data.PSTrafficDetails;
import com.percussion.activity.data.PSTrafficDetailsRequest;
import com.percussion.activity.service.IPSActivityService;
import com.percussion.activity.service.IPSTrafficService;
import com.percussion.analytics.data.IPSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderQueryService;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.utils.date.PSDateRange;
import java.text.ParseException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The traffic data service. This service provides actual data. Sunny Sal: "If you can measure it,
 * you can improve it!"
 */
public class PSTrafficService implements IPSTrafficService {

  private static final Logger log = LogManager.getLogger(PSTrafficService.class);

  private final IPSActivityService activityService;
  private final IPSAnalyticsProviderQueryService analyticsService;
  private final IPSAnalyticsProviderService providerService;
  private final IPSSiteDataService siteDataService;
  private final IPSPathService pathService;
  private final IPSFolderHelper folderHelper;
  private final IPSPageService pageService;

  public PSTrafficService(
      IPSActivityService activityService,
      IPSAnalyticsProviderQueryService analyticsService,
      IPSAnalyticsProviderService providerService,
      IPSSiteDataService siteDataService,
      IPSPathService pathService,
      IPSFolderHelper folderHelper,
      IPSPageService pageService) {
    this.activityService = activityService;
    this.analyticsService = analyticsService;
    this.providerService = providerService;
    this.siteDataService = siteDataService;
    this.pathService = pathService;
    this.folderHelper = folderHelper;
    this.pageService = pageService;
  }

  @Override
  public PSContentTraffic getContentTraffic(PSContentTrafficRequest request)
      throws PSTrafficServiceException, PSValidationException {
    String path = request.getPath();
    String startDate = request.getStartDate();
    String endDate = request.getEndDate();
    String granularity = request.getGranularity().orElse("");
    List<String> dataReq = request.getTrafficRequested().orElse(Collections.emptyList());

    PSContentTraffic results = new PSContentTraffic();
    FastDateFormat df = FastDateFormat.getInstance("MM/dd/yyyy");
    List<String> dates = new ArrayList<>();
    TrafficSeries series = new TrafficSeries();

    PSSiteSummary siteInfo;
    try {
      siteInfo = siteDataService.findByPath(path);
    } catch (Exception e) {
      // Empty install / unknown path: Home gadgets should get empty series (HTTP 200), not 500.
      log.warn(
          "Content traffic path not found or site unavailable (path={}); returning empty traffic"
              + " series. Cause: {}",
          path,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return emptyContentTraffic(request);
    }

    PSDateRange range;
    try {
      range = createPSDateRange(startDate, endDate, granularity);
    } catch (ParseException e) {
      // Malformed date / granularity remain real validation errors.
      throw new PSTrafficServiceException(e.getMessage());
    }

    for (Date d : range.getGranularityBreakdown()) {
      dates.add(df.format(d));
    }

    List<Date> dateList = new ArrayList<>(range.getGranularityBreakdown());
    dateList.add(range.getEnd());

    Collection<Integer> pageIds = activityService.findPageIdsByPath(pageLookupPath(siteInfo, path));

    collectActivitySeries(dataReq, pageIds, dateList, series);
    collectVisits(dataReq, range, dateList, siteInfo, request, series);
    assembleContentTraffic(results, siteInfo, dates, series);

    return results;
  }

  /** Mutable per-series accumulators for one {@link #getContentTraffic} call (issue #6435). */
  private static final class TrafficSeries {
    final List<Integer> newPages = new ArrayList<>();
    final List<Integer> pageUpdates = new ArrayList<>();
    final List<Integer> takeDowns = new ArrayList<>();
    final List<Integer> livePages = new ArrayList<>();
    final List<Integer> visits = new ArrayList<>();
  }

  /**
   * Fills the CMS-activity series (live / new / updated / takedown counts). Extracted from {@link
   * #getContentTraffic} to keep that method's cognitive complexity within the gate (issue #6435);
   * ordering is load-bearing — the UPDATED_PAGES block reads {@code series.newPages}.
   */
  private void collectActivitySeries(
      List<String> dataReq, Collection<Integer> pageIds, List<Date> dateList, TrafficSeries series) {
    if (dataReq.contains(PSTrafficTypeEnum.LIVE_PAGES.toString())) {
      series.livePages.addAll(activityService.findPublishedItems(pageIds, dateList));
    }
    if (dataReq.contains(PSTrafficTypeEnum.NEW_PAGES.toString())) {
      series.newPages.addAll(activityService.findNewContentActivities(pageIds, dateList));
    }
    if (dataReq.contains(PSTrafficTypeEnum.UPDATED_PAGES.toString())) {
      List<Integer> activity =
          activityService.findNumberContentActivities(pageIds, dateList, WF_STATE_LIVE, null);
      for (int i = 0; i < activity.size(); i++) {
        series.pageUpdates.add(i, activity.get(i) - series.newPages.get(i));
      }
    }
    if (dataReq.contains(PSTrafficTypeEnum.TAKE_DOWNS.toString())) {
      series.takeDowns.addAll(
          activityService.findNumberContentActivities(
              pageIds, dateList, WF_STATE_ARCHIVE, WF_TAKE_DOWN_TRANSITION));
    }
  }

  /**
   * Fills the analytics visits series, degrading to zeros when analytics is not configured (issue
   * #6399). Extracted from {@link #getContentTraffic} for complexity gate (issue #6435).
   */
  private void collectVisits(
      List<String> dataReq,
      PSDateRange range,
      List<Date> dateList,
      PSSiteSummary siteInfo,
      PSContentTrafficRequest request,
      TrafficSeries series)
      throws PSTrafficServiceException, PSValidationException {
    if (!dataReq.contains(PSTrafficTypeEnum.VISITS.toString())) {
      return;
    }
    try {
      series.visits.addAll(
          createAnalyticsActivity(range, dateList, siteInfo.getName(), request.getUsage()));
    } catch (PSAnalyticsProviderException e) {
      if (e.getCauseType() == CAUSETYPE.ANALYTICS_NOT_CONFIG
          || e.getCauseType() == CAUSETYPE.NO_PROFILE) {
        // Fresh cell or site without a GA mapping: the Home Traffic gadget degrades to a
        // zero-filled visits series instead of HTTP 500 (issue #6399). Auth/transport
        // failures below still throw so real outages stay visible.
        log.warn(
            "Analytics not configured for site {}; returning zero-filled visits series. Cause:"
                + " {}",
            siteInfo.getName(),
            PSExceptionUtils.getMessageForLog(e));
        series.visits.addAll(Collections.nCopies(dateList.size() - 1, 0));
      } else {
        var errorHandler = providerService.getErrorMessageHandler();
        throw new PSTrafficServiceException(errorHandler.getMessage(e), e);
      }
    } catch (IPSGenericDao.LoadException e) {
      throw new PSTrafficServiceException(e.getMessage(), e);
    }
  }

  /**
   * Combines the collected series into update totals and stamps them onto the result payload.
   * Extracted from {@link #getContentTraffic} for complexity gate (issue #6435).
   */
  private void assembleContentTraffic(
      PSContentTraffic results, PSSiteSummary siteInfo, List<String> dates, TrafficSeries series) {
    List<Integer> updateTotals = new ArrayList<>();
    for (int i = 0; i < dates.size(); i++) {
      updateTotals.add(
          i,
          atOrZero(series.newPages, i)
              + atOrZero(series.pageUpdates, i)
              + atOrZero(series.takeDowns, i));
    }
    results.setDates(removeLast(dates));
    results.setPageUpdates(removeLast(series.pageUpdates));
    results.setNewPages(removeLast(series.newPages));
    results.setLivePages(removeLast(series.livePages));
    results.setTakeDowns(removeLast(series.takeDowns));
    results.setVisits(removeLast(series.visits));
    results.setUpdateTotals(removeLast(updateTotals));
    results.setSite(siteInfo.getName());
    results.setSiteId(siteInfo.getId());
  }

  @Override
  public List<PSTrafficDetails> getTrafficDetails(PSTrafficDetailsRequest request)
      throws PSTrafficServiceException,
          PSDataServiceException,
          IPSPathService.PSPathServiceException {
    String path = request.getPath();
    String startDate = request.getStartDate();
    String endDate = request.getEndDate();

    PSDateRange range;
    try {
      range = createPSDateRange(startDate, endDate, PSDateRange.Granularity.DAY.toString());
    } catch (ParseException e) {
      throw new PSTrafficServiceException(e.getMessage());
    }

    PSDateRange previousRange =
        new PSDateRange(range.getStart(), PSDateRange.Granularity.DAY, range.getDaysInRange());

    List<Integer> pageIds = new ArrayList<>(activityService.findPageIdsByPath(path));
    List<String> activityIds =
        activityService.findPageIdsContentActivities(
            pageIds, range.getStart(), range.getEnd(), WF_STATE_LIVE, null);

    List<PSTrafficDetails> itemPropList = new ArrayList<>();
    for (String pageId : activityIds) {
      try {
        PSPathItem pathItem = folderHelper.findItemById(pageId);
        String itemPath = pathItem.getFolderPaths().get(0) + "/" + pathItem.getName();
        String finderPath = PSPathUtils.getFinderPath(itemPath);
        PSItemProperties itemProp = pathService.findItemProperties(finderPath);
        itemProp.setPath(finderPath);
        PSPage page = pageService.findPageByPath(itemPath);
        if (page != null) {
          itemProp.setSummary(page.getSummary());
        }
        itemPropList.add(createTrafficDetail(itemProp));
      } catch (PSNotFoundException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    PSSiteSummary siteInfo = siteDataService.findByPath(path);
    List<IPSAnalyticsQueryResult> currentAnalytics = new ArrayList<>();
    List<IPSAnalyticsQueryResult> previousAnalytics = new ArrayList<>();
    try {
      currentAnalytics = analyticsService.getPageViewsByPathPrefix(siteInfo.getName(), null, range);
      previousAnalytics =
          analyticsService.getPageViewsByPathPrefix(siteInfo.getName(), null, previousRange);
    } catch (PSAnalyticsProviderException e) {
      throw new PSTrafficServiceException(e);
    }

    for (int j = 0; j < itemPropList.size(); j++) {
      int currentViews =
          findPageAnalyticsCount(
              currentAnalytics, itemPropList.get(j), siteInfo.getName(), request.getUsage());
      int previousViews =
          findPageAnalyticsCount(
              previousAnalytics, itemPropList.get(j), siteInfo.getName(), request.getUsage());
      itemPropList.get(j).setVisits(currentViews);
      itemPropList.get(j).setVisitsDelta(currentViews - previousViews);
    }

    return itemPropList;
  }

  /**
   * Empty traffic payload for gadget-friendly empty installs / unknown paths. Series lists are
   * non-null empty so clients can chart without null checks.
   */
  private PSContentTraffic emptyContentTraffic(PSContentTrafficRequest request) {
    PSContentTraffic results = new PSContentTraffic();
    results.setDates(new ArrayList<>());
    results.setPageUpdates(new ArrayList<>());
    results.setNewPages(new ArrayList<>());
    results.setLivePages(new ArrayList<>());
    results.setTakeDowns(new ArrayList<>());
    results.setVisits(new ArrayList<>());
    results.setUpdateTotals(new ArrayList<>());
    if (request != null) {
      if (request.getStartDate() != null) {
        results.setStartDate(request.getStartDate());
      }
      if (request.getEndDate() != null) {
        results.setEndDate(request.getEndDate());
      }
    }
    return results;
  }

  /**
   * Repository path used for page lookups for a site request.
   *
   * <p>The Home dashboard sends the finder-style site-name path ({@code /Sites/<name>}), but JCR
   * content queries need the repository folder ({@code //Sites/<folder>}); the name and folder
   * differ (e.g. {@code Corporate_Investments} vs {@code CorporateInvestments}), and the legacy
   * query engine rejects single-slash paths outright. Prefer the resolved site's folder; fall back
   * to slash-normalizing the request path so callers without site metadata still issue a
   * well-formed (possibly empty) query instead of an ERROR + empty (issue #6399).
   */
  private static String pageLookupPath(PSSiteSummary siteInfo, String requestPath) {
    if (siteInfo != null && StringUtils.isNotBlank(siteInfo.getFolderPath())) {
      return siteInfo.getFolderPath();
    }
    if (requestPath != null && requestPath.startsWith("/") && !requestPath.startsWith("//")) {
      // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6399,
      // same as #6325/#5898/#5910/#5927) — slashes parse the CMS logical finder path, not an OS path.
      return "/" + requestPath;
    }
    return requestPath;
  }

  /**
   * Null-safe indexed read for update-totals math: callers may request any subset of
   * NEW_PAGES/UPDATED_PAGES/TAKE_DOWNS, and unrequested series stay empty (never {@code null}).
   * Missing entries count as zero instead of throwing {@code IndexOutOfBoundsException} → HTTP
   * 500 (issue #6399).
   */
  private static int atOrZero(List<Integer> series, int index) {
    return index < series.size() ? series.get(index) : 0;
  }

  /**
   * Helper method to create PSDateRange from string dates and granularity. The end date of the
   * created range will be the specified end date plus one.
   */
  private PSDateRange createPSDateRange(String start, String end, String granularity)
      throws ParseException {
    var formatter = FastDateFormat.getInstance("MM/dd/yyyy");
    var startDate = formatter.parse(start);
    var endDate = formatter.parse(addDay(end, formatter));
    return new PSDateRange(startDate, endDate, PSDateRange.Granularity.valueOf(granularity));
  }

  /** Helper method to convert PSItemProperties to PSTrafficDetails. */
  private PSTrafficDetails createTrafficDetail(PSItemProperties itemProp) {
    var tDetail = new PSTrafficDetails();
    tDetail.setId(itemProp.getId());
    tDetail.setLastModifiedDate(itemProp.getLastModifiedDate());
    tDetail.setLastModifier(itemProp.getLastModifier());
    tDetail.setLastPublishedDate(itemProp.getLastPublishedDate());
    tDetail.setName(itemProp.getName());
    tDetail.setPath(itemProp.getPath());
    tDetail.setStatus(itemProp.getStatus());
    tDetail.setType(itemProp.getType());
    tDetail.setSummary(itemProp.getSummary());
    return tDetail;
  }

  /** Helper method to loop through Analytics and find matches for a page. */
  private int findPageAnalyticsCount(
      List<IPSAnalyticsQueryResult> visitResults,
      PSItemProperties itemProp,
      String siteName,
      String usage) {
    int visitCount = 0;
    var itemPath =
        itemProp.getPath().toLowerCase().replaceFirst("/sites/" + siteName.toLowerCase(), "");
    for (var visit : visitResults) {
      var analyticsPath = visit.getString(IPSAnalyticsProviderQueryService.FIELD_PAGE_PATH);
      if (StringUtils.equalsIgnoreCase(analyticsPath, itemPath)) {
        visitCount += visit.getInt(usage);
      }
    }
    return visitCount;
  }

  /** Helper method to get analytics by list of dates. */
  private String addDay(String dt, FastDateFormat sdf) throws ParseException {
    var c = Calendar.getInstance();
    c.setTime(sdf.parse(dt));
    c.add(Calendar.DATE, 1);
    return sdf.format(c.getTime());
  }

  /** Helper method to remove last index in List. */
  private <T> List<T> removeLast(List<T> list) {
    if (!list.isEmpty()) {
      list.remove(list.size() - 1);
    }
    return list;
  }

  /** Helper method to get analytics by list of dates. */
  public List<Integer> createAnalyticsActivity(
      PSDateRange range, List<Date> dates, String siteName, String usage)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    var counts = new ArrayList<Integer>(Collections.nCopies(dates.size() - 1, 0));
    var visitResults = analyticsService.getVisitsViewsBySite(siteName, range);
    for (var visit : visitResults) {
      var visitDate = visit.getDate(IPSAnalyticsProviderQueryService.FIELD_DATE);
      var visitCount = visit.getInt(usage);
      for (int i = 0; i < dates.size() - 1; i++) {
        if (visitDate.equals(dates.get(i))
            || (visitDate.after(dates.get(i)) && visitDate.before(dates.get(i + 1)))) {
          counts.set(i, counts.get(i) + visitCount);
          break;
        }
      }
    }
    return counts;
  }
}
