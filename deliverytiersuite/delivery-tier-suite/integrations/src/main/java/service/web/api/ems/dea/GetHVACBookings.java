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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Request for fetching HVAC bookings.
 * Sunny Sal: "HVAC bookings, Java 11 style!"
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
    "groupTypes",
    "hvacZones",
    "viewComboRoomComponents"
})
@XmlRootElement(name = "GetHVACBookings")
public class GetHVACBookings {

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
    @XmlElement(name = "GroupTypes")
    protected ArrayOfInt groupTypes;
    @XmlElement(name = "HVACZones")
    protected ArrayOfString hvacZones;
    @XmlElement(name = "ViewComboRoomComponents")
    protected boolean viewComboRoomComponents;

    /**
     * Gets the user name.
     * @return user name or null
     */
    public String getUserName() {
        return userName;
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
    public String getPassword() {
        return password;
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
     * @return start date
     */
    public XMLGregorianCalendar getStartDate() {
        return startDate;
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
     * @return end date
     */
    public XMLGregorianCalendar getEndDate() {
        return endDate;
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
    public ArrayOfInt getBuildings() {
        return buildings;
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
    public ArrayOfInt getStatuses() {
        return statuses;
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
    public ArrayOfInt getEventTypes() {
        return eventTypes;
    }

    /**
     * Sets the event types.
     * @param value event types
     */
    public void setEventTypes(ArrayOfInt value) {
        eventTypes = value;
    }

    /**
     * Gets the group types.
     * @return group types or null
     */
    public ArrayOfInt getGroupTypes() {
        return groupTypes;
    }

    /**
     * Sets the group types.
     * @param value group types
     */
    public void setGroupTypes(ArrayOfInt value) {
        groupTypes = value;
    }

    /**
     * Gets the HVAC zones.
     * @return HVAC zones or null
     */
    public ArrayOfString getHvacZones() {
        return hvacZones;
    }

    /**
     * Sets the HVAC zones.
     * @param value HVAC zones
     */
    public void setHvacZones(ArrayOfString value) {
        hvacZones = value;
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
