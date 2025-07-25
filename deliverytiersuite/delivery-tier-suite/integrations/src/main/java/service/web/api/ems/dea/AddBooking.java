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
 *         &lt;element name="RoomID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BookingDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="StartTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EventName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="StatusID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="EventTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
    "roomID",
    "bookingDate",
    "startTime",
    "endTime",
    "eventName",
    "statusID",
    "eventTypeID"
})
@XmlRootElement(name = "AddBooking")
public class AddBooking {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "ReservationID")
    protected int reservationID;
    @XmlElement(name = "RoomID")
    protected int roomID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar bookingDate;
    @XmlElement(name = "StartTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startTime;
    @XmlElement(name = "EndTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endTime;
    @XmlElement(name = "EventName")
    protected String eventName;
    @XmlElement(name = "StatusID")
    protected int statusID;
    @XmlElement(name = "EventTypeID")
    protected int eventTypeID;

    /**
     * Gets the value of the userName property.
     * @return Optional of userName
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /**
     * Sets the value of the userName property.
     * @param value allowed object is {@link String }
     */
    public void setUserName(String value) {
        this.userName = value;
    }
    /**
     * Gets the value of the password property.
     * @return Optional of password
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /**
     * Sets the value of the password property.
     * @param value allowed object is {@link String }
     */
    public void setPassword(String value) {
        this.password = value;
    }
    /**
     * Gets the value of the reservationID property.
     * 
     */
    public int getReservationID() {
        return reservationID;
    }

    /**
     * Sets the value of the reservationID property.
     * 
     */
    public void setReservationID(int value) {
        this.reservationID = value;
    }

    /**
     * Gets the value of the roomID property.
     * 
     */
    public int getRoomID() {
        return roomID;
    }

    /**
     * Sets the value of the roomID property.
     * 
     */
    public void setRoomID(int value) {
        this.roomID = value;
    }

    /**
     * Gets the value of the bookingDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getBookingDate() {
        return bookingDate;
    }

    /**
     * Sets the value of the bookingDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setBookingDate(XMLGregorianCalendar value) {
        this.bookingDate = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setStartTime(XMLGregorianCalendar value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setEndTime(XMLGregorianCalendar value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the eventName property.
     * @return Optional of eventName
     */
    public java.util.Optional<String> getEventName() {
        return java.util.Optional.ofNullable(eventName);
    }
    /**
     * Sets the value of the eventName property.
     * @param value allowed object is {@link String }
     */
    public void setEventName(String value) {
        this.eventName = value;
    }

    /**
     * Gets the value of the statusID property.
     * 
     */
    public int getStatusID() {
        return statusID;
    }

    /**
     * Sets the value of the statusID property.
     * 
     */
    public void setStatusID(int value) {
        this.statusID = value;
    }

    /**
     * Gets the value of the eventTypeID property.
     * 
     */
    public int getEventTypeID() {
        return eventTypeID;
    }

    /**
     * Sets the value of the eventTypeID property.
     * 
     */
    public void setEventTypeID(int value) {
        this.eventTypeID = value;
    }

    // REFACTORED: CP-JAVA11
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddBooking that = (AddBooking) o;
        return reservationID == that.reservationID &&
                roomID == that.roomID &&
                statusID == that.statusID &&
                eventTypeID == that.eventTypeID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(bookingDate, that.bookingDate) &&
                java.util.Objects.equals(startTime, that.startTime) &&
                java.util.Objects.equals(endTime, that.endTime) &&
                java.util.Objects.equals(eventName, that.eventName);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, reservationID, roomID, bookingDate, startTime, endTime, eventName, statusID, eventTypeID);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AddBooking{");
        sb.append("userName='").append(userName).append('\'');
        sb.append(", password='[PROTECTED]'");
        sb.append(", reservationID=").append(reservationID);
        sb.append(", roomID=").append(roomID);
        sb.append(", bookingDate=").append(bookingDate);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", eventName='").append(eventName).append('\'');
        sb.append(", statusID=").append(statusID);
        sb.append(", eventTypeID=").append(eventTypeID);
        sb.append('}');
        return sb.toString();
    }
}
