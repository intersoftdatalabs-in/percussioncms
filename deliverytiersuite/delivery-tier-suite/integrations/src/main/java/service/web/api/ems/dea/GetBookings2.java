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
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ReservationID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="StartDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="Buildings" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="Statuses" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="EventTypes" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="GroupTypes" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="ViewComboRoomComponents" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "reservationID",
    "startDate",
    "endDate",
    "buildings",
    "statuses",
    "eventTypes",
    "groupTypes",
    "viewComboRoomComponents"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBookings2")
public class GetBookings2 {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "ReservationID")
    private int reservationID;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar endDate;
    @XmlElement(name = "Buildings")
    private ArrayOfInt buildings;
    @XmlElement(name = "Statuses")
    private ArrayOfInt statuses;
    @XmlElement(name = "EventTypes")
    private ArrayOfInt eventTypes;
    @XmlElement(name = "GroupTypes")
    private ArrayOfInt groupTypes;
    @XmlElement(name = "ViewComboRoomComponents")
    private boolean viewComboRoomComponents;

    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }

    public void setUserName(String value) {
        userName = value;
    }

    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }

    public void setPassword(String value) {
        password = value;
    }

    public int getReservationID() {
        return reservationID;
    }

    public void setReservationID(int value) {
        reservationID = value;
    }

    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    public void setStartDate(XMLGregorianCalendar value) {
        startDate = value;
    }

    public XMLGregorianCalendar getEndDate() {
        return endDate;
    }

    public void setEndDate(XMLGregorianCalendar value) {
        endDate = value;
    }

    public ArrayOfInt getBuildings() {
        return buildings;
    }

    public void setBuildings(ArrayOfInt value) {
        buildings = value;
    }

    public ArrayOfInt getStatuses() {
        return statuses;
    }

    public void setStatuses(ArrayOfInt value) {
        statuses = value;
    }

    public ArrayOfInt getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(ArrayOfInt value) {
        eventTypes = value;
    }

    public ArrayOfInt getGroupTypes() {
        return groupTypes;
    }

    public void setGroupTypes(ArrayOfInt value) {
        groupTypes = value;
    }

    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    public void setViewComboRoomComponents(boolean value) {
        viewComboRoomComponents = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBookings2)) return false;
        var that = (GetBookings2) o;
        return reservationID == that.reservationID
                && viewComboRoomComponents == that.viewComboRoomComponents
                && java.util.Objects.equals(userName, that.userName)
                && java.util.Objects.equals(password, that.password)
                && java.util.Objects.equals(startDate, that.startDate)
                && java.util.Objects.equals(endDate, that.endDate)
                && java.util.Objects.equals(buildings, that.buildings)
                && java.util.Objects.equals(statuses, that.statuses)
                && java.util.Objects.equals(eventTypes, that.eventTypes)
                && java.util.Objects.equals(groupTypes, that.groupTypes);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, reservationID, startDate, endDate, buildings, statuses, eventTypes, groupTypes, viewComboRoomComponents);
    }

    @Override
    public String toString() {
        return "GetBookings2{"
                + "userName='" + userName + '\''
                + ", password='[PROTECTED]'"
                + ", reservationID=" + reservationID
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", buildings=" + buildings
                + ", statuses=" + statuses
                + ", eventTypes=" + eventTypes
                + ", groupTypes=" + groupTypes
                + ", viewComboRoomComponents=" + viewComboRoomComponents
                + '}';
    }
}
