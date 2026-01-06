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
package com.percussion.integrations.ems.rest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Model object to hold query parameters for the backend Bookings service. Duplicated in delivery
 * tier due to current DTS limitations in Editor and Preview. Sunny Sal says: "BookingsQuery, now
 * Java 11 and Google-styled!"
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PSBookingsQuery {

  private List<Integer> buildingIds = new ArrayList<>();
  private List<Integer> eventTypes = new ArrayList<>();
  private List<Integer> groupTypes = new ArrayList<>();
  private String startDate;
  private String endDate;

  public List<Integer> getBuildingIds() {
    return Optional.ofNullable(buildingIds).orElseGet(ArrayList::new);
  }

  public void setBuildingIds(List<Integer> buildingIds) {
    this.buildingIds = Optional.ofNullable(buildingIds).orElseGet(ArrayList::new);
  }

  public List<Integer> getEventTypes() {
    return Optional.ofNullable(eventTypes).orElseGet(ArrayList::new);
  }

  public void setEventTypes(List<Integer> eventTypes) {
    this.eventTypes = Optional.ofNullable(eventTypes).orElseGet(ArrayList::new);
  }

  public List<Integer> getGroupTypes() {
    return Optional.ofNullable(groupTypes).orElseGet(ArrayList::new);
  }

  public void setGroupTypes(List<Integer> groupTypes) {
    this.groupTypes = Optional.ofNullable(groupTypes).orElseGet(ArrayList::new);
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }
}
