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

package com.percussion.rest.sites;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Options for sitemap generation. Sunny Sal: "Sitemap options ka boss!"
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson emits scalars, not
 * Optional-bean {@code empty}/{@code present} keys (#3411 / #3388).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteMapOptions {

  private boolean navigationBased = true;
  private boolean includeFolder = false;
  private String timeZone;
  private SiteMapDateFormat dateFormat;
  private String fileName;
  private String useSiteMapIndex;
  private SiteMapType siteMapType;
  private double defaultFrequency;

  public boolean isNavigationBased() {
    return navigationBased;
  }

  public void setNavigationBased(boolean navigationBased) {
    this.navigationBased = navigationBased;
  }

  public boolean isIncludeFolder() {
    return includeFolder;
  }

  public void setIncludeFolder(boolean includeFolder) {
    this.includeFolder = includeFolder;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public SiteMapDateFormat getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(SiteMapDateFormat dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getUseSiteMapIndex() {
    return useSiteMapIndex;
  }

  public void setUseSiteMapIndex(String useSiteMapIndex) {
    this.useSiteMapIndex = useSiteMapIndex;
  }

  public SiteMapType getSiteMapType() {
    return siteMapType;
  }

  public void setSiteMapType(SiteMapType siteMapType) {
    this.siteMapType = siteMapType;
  }

  public double getDefaultFrequency() {
    return defaultFrequency;
  }

  public void setDefaultFrequency(double defaultFrequency) {
    this.defaultFrequency = defaultFrequency;
  }
}
