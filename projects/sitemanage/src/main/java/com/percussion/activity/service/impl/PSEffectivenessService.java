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

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.data.PSEffectiveness;
import com.percussion.activity.data.PSEffectivenessRequest;
import com.percussion.activity.service.IPSActivityService;
import com.percussion.activity.service.IPSContentActivityService.PSDurationTypeEnum;
import com.percussion.activity.service.IPSContentActivityService.PSUsageEnum;
import com.percussion.activity.service.IPSEffectivenessService;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.service.IPSAnalyticsProviderQueryService;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.date.PSDateRange;
import com.percussion.utils.date.PSDateRange.Granularity;
import java.util.ArrayList;
import java.util.List;

/**
 * The effectiveness data service. This service provides actual data. Sunny Sal: "Effectiveness is
 * not just a metric, it's a way of life!"
 */
public class PSEffectivenessService implements IPSEffectivenessService {

  private final IPSActivityService activityService;
  private final IPSAnalyticsProviderQueryService analyticsService;

  public PSEffectivenessService(
      IPSActivityService activityService, IPSAnalyticsProviderQueryService analyticsService) {
    this.activityService = activityService;
    this.analyticsService = analyticsService;
  }

  @Override
  public List<PSEffectiveness> getEffectiveness(
      PSEffectivenessRequest request, List<PSContentActivity> activity)
      throws PSAnalyticsProviderException {
    notNull(request, "request must not be null");
    notNull(activity, "activity must not be null");

    var effectivenessList = new ArrayList<PSEffectiveness>();
    var durationType = PSDurationTypeEnum.valueOf(request.getDurationType().orElse(""));
    var duration = Integer.parseInt(request.getDuration().orElse("0"));
    var granularity = getGranularity(durationType);
    var currRange = new PSDateRange(granularity, duration);
    var prevRange = new PSDateRange(currRange.getStart(), granularity, duration);

    // usage is a plain enum in the request
    var usageEnum = request.getUsage() != null ? request.getUsage() : PSUsageEnum.unique_pageviews;
    var resultKey =
        (usageEnum == PSUsageEnum.unique_pageviews)
            ? IPSAnalyticsProviderQueryService.FIELD_UNIQUE_PAGEVIEWS
            : IPSAnalyticsProviderQueryService.FIELD_PAGEVIEWS;

    var exceptions = new ArrayList<Exception>();
    for (var ca : activity) {
      long changes = (long) ca.getNewItems() + ca.getUpdatedItems();
      try {
        var currViews = getViews(ca, currRange, resultKey);
        var prevViews = getViews(ca, prevRange, resultKey);
        var effectiveness = (currViews - prevViews) / ((changes > 0) ? changes : 1);
        effectivenessList.add(new PSEffectiveness(ca.getName(), effectiveness));
      } catch (PSAnalyticsProviderException
          | IPSGenericDao.LoadException
          | PSValidationException e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty() && exceptions.size() == activity.size()) {
      throw new PSAnalyticsProviderException(exceptions.get(0));
    }

    return effectivenessList;
  }

  /**
   * Gets the total number of analytics views for the given content activity during the specified
   * interval.
   *
   * @param ca the content activity, assumed not null.
   * @param range the date range interval, assumed not null.
   * @param resultKey determines which query result view field to extract, assumed not blank.
   * @return total number of views.
   * @throws PSAnalyticsProviderException if an error occurs retrieving the analytics data.
   */
  private long getViews(PSContentActivity ca, PSDateRange range, String resultKey)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    long views = 0L;
    var results = analyticsService.getPageViewsByPathPrefix(ca.getSiteName(), ca.getPath(), range);
    for (var result : results) {
      views += result.getLong(resultKey);
    }
    return views;
  }

  /**
   * Maps a duration type to a granularity.
   *
   * @param durationType assumed not null.
   * @return the corresponding granularity, never null.
   */
  private Granularity getGranularity(PSDurationTypeEnum durationType) {
    return switch (durationType) {
      case days -> Granularity.DAY;
      case weeks -> Granularity.WEEK;
      case months -> Granularity.MONTH;
      case years -> Granularity.YEAR;
    };
  }
}
