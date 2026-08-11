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

// REFACTORED: CP-JAVA11
package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import java.util.Optional;

/** This object holds traffic activity details of the items under named site by date. */
@JsonRootName(value = "ContentTraffic")
public class PSContentTraffic {

  private String site;
  private String siteId;
  private String startDate;
  private String endDate;
  private List<String> dates;
  private List<Integer> visits;
  private List<Integer> newPages;
  private List<Integer> pageUpdates;
  private List<Integer> takeDowns;
  private List<Integer> livePages;
  private List<Integer> updateTotals;

  public PSContentTraffic() {}

  public PSContentTraffic(String site) {
    this.site = site;
  }

  public Optional<String> getSite() {
    return Optional.ofNullable(site);
  }

  public Optional<String> getSiteId() {
    return Optional.ofNullable(siteId);
  }

  public Optional<String> getStartDate() {
    return Optional.ofNullable(startDate);
  }

  public Optional<String> getEndDate() {
    return Optional.ofNullable(endDate);
  }

  public Optional<List<Integer>> getVisits() {
    return Optional.ofNullable(visits);
  }

  public Optional<List<Integer>> getNewPages() {
    return Optional.ofNullable(newPages);
  }

  public Optional<List<Integer>> getPageUpdates() {
    return Optional.ofNullable(pageUpdates);
  }

  public Optional<List<Integer>> getTakeDowns() {
    return Optional.ofNullable(takeDowns);
  }

  public Optional<List<Integer>> getLivePages() {
    return Optional.ofNullable(livePages);
  }

  public Optional<List<String>> getDates() {
    return Optional.ofNullable(dates);
  }

  public Optional<List<Integer>> getUpdateTotals() {
    return Optional.ofNullable(updateTotals);
  }

  public void setSite(String site) {
    this.site = site;
  }

  public void setSiteId(String siteId) {
    this.siteId = siteId;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public void setVisits(List<Integer> visits) {
    this.visits = visits;
  }

  public void setNewPages(List<Integer> newPages) {
    this.newPages = newPages;
  }

  public void setPageUpdates(List<Integer> pageUpdates) {
    this.pageUpdates = pageUpdates;
  }

  public void setTakeDowns(List<Integer> takeDowns) {
    this.takeDowns = takeDowns;
  }

  public void setLivePages(List<Integer> livePages) {
    this.livePages = livePages;
  }

  public void setDates(List<String> dates) {
    this.dates = dates;
  }

  public void setUpdateTotals(List<Integer> updateTotals) {
    this.updateTotals = updateTotals;
  }
}
