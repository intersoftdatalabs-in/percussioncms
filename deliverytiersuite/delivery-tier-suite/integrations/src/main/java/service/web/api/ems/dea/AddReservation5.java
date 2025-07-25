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
 *         &lt;element name="GroupID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="RoomID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BookingDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="StartTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EventName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="StatusID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="EventTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="WebUserID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="WebTemplateID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ReservationSourceID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BillingReference" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="PONumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="SetupCount" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
    "groupID",
    "roomID",
    "bookingDate",
    "startTime",
    "endTime",
    "eventName",
    "statusID",
    "eventTypeID",
    "webUserID",
    "webTemplateID",
    "reservationSourceID",
    "billingReference",
    "poNumber",
    "setupCount"
})
@XmlRootElement(name = "AddReservation5")
public final class AddReservation5 {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "GroupID")
    private int groupID;
    @XmlElement(name = "RoomID")
    private int roomID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar bookingDate;
    @XmlElement(name = "StartTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar startTime;
    @XmlElement(name = "EndTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar endTime;
    @XmlElement(name = "EventName")
    private String eventName;
    @XmlElement(name = "StatusID")
    private int statusID;
    @XmlElement(name = "EventTypeID")
    private int eventTypeID;
    @XmlElement(name = "WebUserID")
    private int webUserID;
    @XmlElement(name = "WebTemplateID")
    private int webTemplateID;
    @XmlElement(name = "ReservationSourceID")
    private int reservationSourceID;
    @XmlElement(name = "BillingReference")
    private String billingReference;
    @XmlElement(name = "PONumber")
    private String poNumber;
    @XmlElement(name = "SetupCount")
    private int setupCount;

    // --- Modernized Getters/Setters ---
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    public void setUserName(String value) {
        this.userName = value;
    }
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    public void setPassword(String value) {
        this.password = value;
    }
    public int getGroupID() {
        return groupID;
    }
    public void setGroupID(int value) {
        this.groupID = value;
    }
    public int getRoomID() {
        return roomID;
    }
    public void setRoomID(int value) {
        this.roomID = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getBookingDate() {
        return java.util.Optional.ofNullable(bookingDate);
    }
    public void setBookingDate(XMLGregorianCalendar value) {
        this.bookingDate = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getStartTime() {
        return java.util.Optional.ofNullable(startTime);
    }
    public void setStartTime(XMLGregorianCalendar value) {
        this.startTime = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getEndTime() {
        return java.util.Optional.ofNullable(endTime);
    }
    public void setEndTime(XMLGregorianCalendar value) {
        this.endTime = value;
    }
    public java.util.Optional<String> getEventName() {
        return java.util.Optional.ofNullable(eventName);
    }
    public void setEventName(String value) {
        this.eventName = value;
    }
    public int getStatusID() {
        return statusID;
    }
    public void setStatusID(int value) {
        this.statusID = value;
    }
    public int getEventTypeID() {
        return eventTypeID;
    }
    public void setEventTypeID(int value) {
        this.eventTypeID = value;
    }
    public int getWebUserID() {
        return webUserID;
    }
    public void setWebUserID(int value) {
        this.webUserID = value;
    }
    public int getWebTemplateID() {
        return webTemplateID;
    }
    public void setWebTemplateID(int value) {
        this.webTemplateID = value;
    }
    public int getReservationSourceID() {
        return reservationSourceID;
    }
    public void setReservationSourceID(int value) {
        this.reservationSourceID = value;
    }
    public java.util.Optional<String> getBillingReference() {
        return java.util.Optional.ofNullable(billingReference);
    }
    public void setBillingReference(String value) {
        this.billingReference = value;
    }
    public java.util.Optional<String> getPONumber() {
        return java.util.Optional.ofNullable(poNumber);
    }
    public void setPONumber(String value) {
        this.poNumber = value;
    }
    public int getSetupCount() {
        return setupCount;
    }
    public void setSetupCount(int value) {
        this.setupCount = value;
    }

    @Override
    public String toString() {
        return "AddReservation5{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", groupID=" + groupID +
                ", roomID=" + roomID +
                ", bookingDate=" + bookingDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", eventName='" + eventName + '\'' +
                ", statusID=" + statusID +
                ", eventTypeID=" + eventTypeID +
                ", webUserID=" + webUserID +
                ", webTemplateID=" + webTemplateID +
                ", reservationSourceID=" + reservationSourceID +
                ", billingReference='" + billingReference + '\'' +
                ", poNumber='" + poNumber + '\'' +
                ", setupCount=" + setupCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddReservation5 that = (AddReservation5) o;
        return groupID == that.groupID &&
                roomID == that.roomID &&
                statusID == that.statusID &&
                eventTypeID == that.eventTypeID &&
                webUserID == that.webUserID &&
                webTemplateID == that.webTemplateID &&
                reservationSourceID == that.reservationSourceID &&
                setupCount == that.setupCount &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(bookingDate, that.bookingDate) &&
                java.util.Objects.equals(startTime, that.startTime) &&
                java.util.Objects.equals(endTime, that.endTime) &&
                java.util.Objects.equals(eventName, that.eventName) &&
                java.util.Objects.equals(billingReference, that.billingReference) &&
                java.util.Objects.equals(poNumber, that.poNumber);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, groupID, roomID, bookingDate, startTime, endTime, eventName, statusID, eventTypeID, webUserID, webTemplateID, reservationSourceID, billingReference, poNumber, setupCount);
    }
}
