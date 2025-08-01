/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Query object for event search in EMS integration.
 * Sunny Sal says: "EventQuery, now Java 11 and Google-styled!"
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PSEventQuery {

    private String startDate;
    private String endDate;
    private String eventName;
    private String location;
    private List<Integer> calendars = new ArrayList<>();
    private List<Integer> eventTypes = new ArrayList<>();

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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Integer> getCalendars() {
        return Optional.ofNullable(calendars).orElseGet(ArrayList::new);
    }

    public void setCalendars(List<Integer> calendars) {
        this.calendars = Optional.ofNullable(calendars).orElseGet(ArrayList::new);
    }

    public List<Integer> getEventTypes() {
        return Optional.ofNullable(eventTypes).orElseGet(ArrayList::new);
    }

    public void setEventTypes(List<Integer> eventTypes) {
        this.eventTypes = Optional.ofNullable(eventTypes).orElseGet(ArrayList::new);
    }
}
