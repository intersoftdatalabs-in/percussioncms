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
package com.percussion.delivery.integrations.ems;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Query object for EMS event search.
 * <p>
 * Used for filtering events by date, name, location, calendar, and event type.
 * </p>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PSEventQuery {
    private String startDate;
    private String endDate;
    private String eventName;
    private String location;
    private List<Integer> calendars;
    private List<Integer> eventTypes;

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
        return calendars;
    }
    public void setCalendars(List<Integer> calendars) {
        this.calendars = calendars;
    }
    public List<Integer> getEventTypes() {
        return eventTypes;
    }
    public void setEventTypes(List<Integer> eventTypes) {
        this.eventTypes = eventTypes;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSEventQuery that = (PSEventQuery) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(location, that.location) &&
                Objects.equals(calendars, that.calendars) &&
                Objects.equals(eventTypes, that.eventTypes);
    }
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, eventName, location, calendars, eventTypes);
    }
    @Override
    public String toString() {
        return "PSEventQuery{" +
                "startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", eventName='" + eventName + '\'' +
                ", location='" + location + '\'' +
                ", calendars=" + calendars +
                ", eventTypes=" + eventTypes +
                '}';
    }
}
