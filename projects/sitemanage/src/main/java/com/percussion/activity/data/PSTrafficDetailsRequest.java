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

package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.analytics.service.IPSAnalyticsProviderQueryService;
import java.io.Serializable;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * A request object used for getting the traffic details data from the REST service.
 */
@JsonRootName(value = "TrafficDetailsRequest")
public class PSTrafficDetailsRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String path;
  private String startDate;
  private String endDate;
  private String usage;

  /**
   * Gets the folder path to the site folder.
   *
   * @return Optional containing the path, or empty if not set.
   */
  public Optional<String> getPath() {
    return Optional.ofNullable(path);
  }

  /**
   * Gets the start date of the date range used for content traffic query.
   *
   * @return Optional containing the start date, or empty if not set.
   */
  public Optional<String> getStartDate() {
    return Optional.ofNullable(startDate);
  }

  /**
   * Gets the end date of the date range used for content traffic query.
   *
   * @return Optional containing the end date, or empty if not set.
   */
  public Optional<String> getEndDate() {
    return Optional.ofNullable(endDate);
  }

  /**
   * Gets the usage for analytics. Default is "uniquepageviews".
   *
   * @return usage string
   */
  public String getUsage() {
    if (StringUtils.equals(usage, IPSAnalyticsProviderQueryService.FIELD_PAGEVIEWS)) {
      return IPSAnalyticsProviderQueryService.FIELD_PAGEVIEWS;
    }
    return IPSAnalyticsProviderQueryService.FIELD_UNIQUE_PAGEVIEWS;
  }

  /**
   * Sets analytics usage to "pageviews" or "uniquepageviews".
   * If not set or set to any other value, default is "uniquepageviews".
   *
   * @param usage the usage string
   */
  public void setUsage(String usage) {
    this.usage = usage;
  }

  /**
   * Sets the folder path to the site folder. Required.
   *
   * @param path the folder path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Sets the start date of the date range used for content traffic query. Required.
   *
   * @param startDate the start date
   */
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  /**
   * Sets the end date of the date range used for content traffic query. Required.
   *
   * @param endDate the end date
   */
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }
}
