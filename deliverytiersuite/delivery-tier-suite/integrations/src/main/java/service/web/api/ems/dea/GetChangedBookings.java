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
 * Java 11 Modernized: GetChangedBookings SOAP request model.
 * <p>
 * Represents the request payload for the GetChangedBookings endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "startDate",
        "endDate",
        "rooms",
        "statuses",
        "eventTypes",
        "groupTypes",
        "viewComboRoomComponents"
    }
)
@XmlRootElement(name = "GetChangedBookings")
public class GetChangedBookings {

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

    @XmlElement(name = "Rooms")
    protected ArrayOfInt rooms;

    @XmlElement(name = "Statuses")
    protected ArrayOfInt statuses;

    @XmlElement(name = "EventTypes")
    protected ArrayOfInt eventTypes;

    @XmlElement(name = "GroupTypes")
    protected ArrayOfInt groupTypes;

    @XmlElement(name = "ViewComboRoomComponents")
    protected boolean viewComboRoomComponents;

    /**
     * Gets the value of the userName property.
     *
     * @return the user name, or null if not set
     */
    public String getUserName() {
        // Java 11: var for local variable (if any logic added)
        return userName;
    }

    /**
     * Sets the value of the userName property.
     *
     * @param value the user name to set
     */
    public void setUserName(String value) {
        userName = value;
    }

    /**
     * Gets the value of the password property.
     *
     * @return the password, or null if not set
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     *
     * @param value the password to set
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the value of the startDate property.
     *
     * @return the start date, never null
     */
    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    /**
     * Sets the value of the startDate property.
     *
     * @param value the start date to set
     */
    public void setStartDate(XMLGregorianCalendar value) {
        startDate = value;
    }

    /**
     * Gets the value of the endDate property.
     *
     * @return the end date, never null
     */
    public XMLGregorianCalendar getEndDate() {
        return endDate;
    }

    /**
     * Sets the value of the endDate property.
     *
     * @param value the end date to set
     */
    public void setEndDate(XMLGregorianCalendar value) {
        endDate = value;
    }

    /**
     * Gets the value of the rooms property.
     *
     * @return the rooms, or null if not set
     */
    public ArrayOfInt getRooms() {
        return rooms;
    }

    /**
     * Sets the value of the rooms property.
     *
     * @param value the rooms to set
     */
    public void setRooms(ArrayOfInt value) {
        rooms = value;
    }

    /**
     * Gets the value of the statuses property.
     *
     * @return the statuses, or null if not set
     */
    public ArrayOfInt getStatuses() {
        return statuses;
    }

    /**
     * Sets the value of the statuses property.
     *
     * @param value the statuses to set
     */
    public void setStatuses(ArrayOfInt value) {
        statuses = value;
    }

    /**
     * Gets the value of the eventTypes property.
     *
     * @return the event types, or null if not set
     */
    public ArrayOfInt getEventTypes() {
        return eventTypes;
    }

    /**
     * Sets the value of the eventTypes property.
     *
     * @param value the event types to set
     */
    public void setEventTypes(ArrayOfInt value) {
        eventTypes = value;
    }

    /**
     * Gets the value of the groupTypes property.
     *
     * @return the group types, or null if not set
     */
    public ArrayOfInt getGroupTypes() {
        return groupTypes;
    }

    /**
     * Sets the value of the groupTypes property.
     *
     * @param value the group types to set
     */
    public void setGroupTypes(ArrayOfInt value) {
        groupTypes = value;
    }

    /**
     * Gets the value of the viewComboRoomComponents property.
     *
     * @return true if combo room components should be viewed, false otherwise
     */
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    /**
     * Sets the value of the viewComboRoomComponents property.
     *
     * @param value true to view combo room components, false otherwise
     */
    public void setViewComboRoomComponents(boolean value) {
        viewComboRoomComponents = value;
    }
}
