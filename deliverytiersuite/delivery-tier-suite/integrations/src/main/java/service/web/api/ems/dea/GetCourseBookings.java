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

package service.web.api.ems.dea;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Optional;

/**
 * Request for fetching course bookings.
 * Sunny Sal: "Course bookings, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "startDate",
    "endDate",
    "buildings",
    "statuses",
    "eventTypes",
    "viewComboRoomComponents"
})
@XmlRootElement(name = "GetCourseBookings")
public class GetCourseBookings {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endDate;
    @XmlElement(name = "Buildings")
    protected ArrayOfInt buildings;
    @XmlElement(name = "Statuses")
    protected ArrayOfInt statuses;
    @XmlElement(name = "EventTypes")
    protected ArrayOfInt eventTypes;
    @XmlElement(name = "ViewComboRoomComponents")
    protected boolean viewComboRoomComponents;

    /**
     * Gets the user name.
     * @return user name or null
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Sets the user name.
     * @param value user name
     */
    public void setUserName(String value) {
        userName = value;
    }

    /**
     * Gets the password.
     * @return password or null
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Sets the password.
     * @param value password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the start date.
     * @return start date or null
     */
    public Optional<XMLGregorianCalendar> getStartDate() {
        return Optional.ofNullable(startDate);
    }

    /**
     * Sets the start date.
     * @param value start date
     */
    public void setStartDate(XMLGregorianCalendar value) {
        startDate = value;
    }

    /**
     * Gets the end date.
     * @return end date or null
     */
    public Optional<XMLGregorianCalendar> getEndDate() {
        return Optional.ofNullable(endDate);
    }

    /**
     * Sets the end date.
     * @param value end date
     */
    public void setEndDate(XMLGregorianCalendar value) {
        endDate = value;
    }

    /**
     * Gets the buildings.
     * @return buildings or null
     */
    public Optional<ArrayOfInt> getBuildings() {
        return Optional.ofNullable(buildings);
    }

    /**
     * Sets the buildings.
     * @param value buildings
     */
    public void setBuildings(ArrayOfInt value) {
        buildings = value;
    }

    /**
     * Gets the statuses.
     * @return statuses or null
     */
    public Optional<ArrayOfInt> getStatuses() {
        return Optional.ofNullable(statuses);
    }

    /**
     * Sets the statuses.
     * @param value statuses
     */
    public void setStatuses(ArrayOfInt value) {
        statuses = value;
    }

    /**
     * Gets the event types.
     * @return event types or null
     */
    public Optional<ArrayOfInt> getEventTypes() {
        return Optional.ofNullable(eventTypes);
    }

    /**
     * Sets the event types.
     * @param value event types
     */
    public void setEventTypes(ArrayOfInt value) {
        eventTypes = value;
    }

    /**
     * Gets the viewComboRoomComponents flag.
     * @return true if combo room components should be viewed
     */
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    /**
     * Sets the viewComboRoomComponents flag.
     * @param value true to view combo room components
     */
    public void setViewComboRoomComponents(boolean value) {
        viewComboRoomComponents = value;
    }
}
