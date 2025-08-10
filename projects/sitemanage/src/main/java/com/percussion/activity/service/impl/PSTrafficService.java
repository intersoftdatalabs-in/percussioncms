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
import com.percussion.analytics.service.IPSAnalyticsProviderQueryService;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
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
  private static final String NOT_FOUND_ERROR =
      "Unable to retrieve analytics data. Please use the Google Setup gadget to select a profile"
          + " for the desired site(s).";

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
    var results = new PSContentTraffic();
    var df = FastDateFormat.getInstance("MM/dd/yyyy");
    var dates = new ArrayList<String>();
    var updateTotals = new ArrayList<Integer>();
    var newPages = new ArrayList<Integer>();
    var pageUpdates = new ArrayList<Integer>();
    var takeDowns = new ArrayList<Integer>();
    var livePages = new ArrayList<Integer>();
    var visits = new ArrayList<Integer>();
    PSSiteSummary siteInfo;
    try {
      siteInfo = siteDataService.findByPath(request.getPath());
    } catch (Exception e) {
      throw new PSTrafficServiceException(NOT_FOUND_ERROR);
    }

    PSDateRange range;
    try {
      range =
          createPSDateRange(request.getStartDate(), request.getEndDate(), request.getGranularity());
    } catch (ParseException e) {
      throw new PSTrafficServiceException(e.getMessage());
    }

    for (var date : range.getGranularityBreakdown()) {
      dates.add(df.format(date));
    }

    var dateList = new ArrayList<>(range.getGranularityBreakdown());
    dateList.add(range.getEnd());
    var dataReq = request.getTrafficRequested();
    var pageIds = activityService.findPageIdsByPath(request.getPath());

    if (dataReq.contains(PSTrafficTypeEnum.LIVE_PAGES.toString())) {
      livePages.addAll(activityService.findPublishedItems(pageIds, dateList));
    }
    if (dataReq.contains(PSTrafficTypeEnum.NEW_PAGES.toString())) {
      newPages.addAll(activityService.findNewContentActivities(pageIds, dateList));
    }
    if (dataReq.contains(PSTrafficTypeEnum.UPDATED_PAGES.toString())) {
      var activity =
          activityService.findNumberContentActivities(pageIds, dateList, WF_STATE_LIVE, null);
      for (int i = 0; i < activity.size(); i++) {
        pageUpdates.add(i, activity.get(i) - newPages.get(i));
      }
    }
    if (dataReq.contains(PSTrafficTypeEnum.TAKE_DOWNS.toString())) {
      takeDowns.addAll(
          activityService.findNumberContentActivities(
              pageIds, dateList, WF_STATE_ARCHIVE, WF_TAKE_DOWN_TRANSITION));
    }
    if (dataReq.contains(PSTrafficTypeEnum.VISITS.toString())) {
      try {
        visits.addAll(
            createAnalyticsActivity(range, dateList, siteInfo.getName(), request.getUsage()));
      } catch (PSAnalyticsProviderException e) {
        var errorHandler = providerService.getErrorMessageHandler();
        throw new PSTrafficServiceException(errorHandler.getMessage(e), e);
      } catch (IPSGenericDao.LoadException e) {
        throw new PSTrafficServiceException(e.getMessage(), e);
      }
    }

    for (int i = 0; i < dates.size(); i++) {
      updateTotals.add(i, newPages.get(i) + pageUpdates.get(i) + takeDowns.get(i));
    }

    results.setStartDate(request.getStartDate());
    results.setEndDate(request.getEndDate());
    results.setDates(removeLast(dates));
    results.setPageUpdates(removeLast(pageUpdates));
    results.setNewPages(removeLast(newPages));
    results.setLivePages(removeLast(livePages));
    results.setTakeDowns(removeLast(takeDowns));
    results.setVisits(removeLast(visits));
    results.setUpdateTotals(removeLast(updateTotals));
    results.setSite(siteInfo.getName());
    results.setSiteId(siteInfo.getId());

    return results;
  }

  @Override
  public List<PSTrafficDetails> getTrafficDetails(PSTrafficDetailsRequest request)
      throws PSTrafficServiceException,
          PSDataServiceException,
          IPSPathService.PSPathServiceException {
    PSDateRange range;
    try {
      range =
          createPSDateRange(
              request.getStartDate(), request.getEndDate(), PSDateRange.Granularity.DAY.toString());
    } catch (ParseException e) {
      throw new PSTrafficServiceException(e.getMessage());
    }

    var previousRange =
        new PSDateRange(range.getStart(), PSDateRange.Granularity.DAY, range.getDaysInRange());
    var pageIds = activityService.findPageIdsByPath(request.getPath());
    var activityIds =
        activityService.findPageIdsContentActivities(
            pageIds, range.getStart(), range.getEnd(), WF_STATE_LIVE, null);

    var itemPropList = new ArrayList<PSTrafficDetails>();
    for (var pageId : activityIds) {
      try {
        var pathItem = folderHelper.findItemById(pageId);
        var path = pathItem.getFolderPaths().get(0) + "/" + pathItem.getName();
        var finderPath = PSPathUtils.getFinderPath(path);
        var itemProp = pathService.findItemProperties(finderPath);
        itemProp.setPath(finderPath);
        PSPage page = pageService.findPageByPath(path);
        if (page != null) {
          itemProp.setSummary(page.getSummary());
        }
        itemPropList.add(createTrafficDetail(itemProp));
      } catch (PSNotFoundException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    var siteInfo = siteDataService.findByPath(request.getPath());
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
