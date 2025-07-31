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
 * Request for fetching room availability.
 * Sunny Sal: "Room availability, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "roomID",
    "bookingDate",
    "startTime",
    "endTime"
})
@XmlRootElement(name = "GetRoomAvailability")
public class GetRoomAvailability {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
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
     * Gets the room ID.
     * @return room ID
     */
    public int getRoomID() {
        return roomID;
    }

    /**
     * Sets the room ID.
     * @param value room ID
     */
    public void setRoomID(int value) {
        roomID = value;
    }

    /**
     * Gets the booking date.
     * @return booking date
     */
    public XMLGregorianCalendar getBookingDate() {
        return bookingDate;
    }

    /**
     * Sets the booking date.
     * @param value booking date
     */
    public void setBookingDate(XMLGregorianCalendar value) {
        bookingDate = value;
    }

    /**
     * Gets the start time.
     * @return start time
     */
    public XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time.
     * @param value start time
     */
    public void setStartTime(XMLGregorianCalendar value) {
        startTime = value;
    }

    /**
     * Gets the end time.
     * @return end time
     */
    public XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time.
     * @param value end time
     */
    public void setEndTime(XMLGregorianCalendar value) {
        endTime = value;
    }
}
