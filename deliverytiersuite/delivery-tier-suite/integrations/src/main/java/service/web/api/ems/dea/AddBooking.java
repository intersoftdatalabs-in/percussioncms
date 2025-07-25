// REFACTORED: CP-JAVA11
// REFACTORED: CP-SOAP
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

import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Java 11+ refactored SOAP request for AddBooking.
 * Immutable, builder-based, Google Java Style, OWASP-compliant.
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
public final class AddBooking {

    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "ReservationID")
    private final int reservationID;
    @XmlElement(name = "RoomID")
    private final int roomID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar bookingDate;
    @XmlElement(name = "StartTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar startTime;
    @XmlElement(name = "EndTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar endTime;
    @XmlElement(name = "EventName")
    private final String eventName;
    @XmlElement(name = "StatusID")
    private final int statusID;
    @XmlElement(name = "EventTypeID")
    private final int eventTypeID;

    private AddBooking(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.reservationID = builder.reservationID;
        this.roomID = builder.roomID;
        this.bookingDate = builder.bookingDate;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.eventName = builder.eventName;
        this.statusID = builder.statusID;
        this.eventTypeID = builder.eventTypeID;
    }

    public Optional<String> getUserName() { return Optional.ofNullable(userName); }
    public Optional<String> getPassword() { return Optional.ofNullable(password); }
    public int getReservationID() { return reservationID; }
    public int getRoomID() { return roomID; }
    public Optional<XMLGregorianCalendar> getBookingDate() { return Optional.ofNullable(bookingDate); }
    public Optional<XMLGregorianCalendar> getStartTime() { return Optional.ofNullable(startTime); }
    public Optional<XMLGregorianCalendar> getEndTime() { return Optional.ofNullable(endTime); }
    public Optional<String> getEventName() { return Optional.ofNullable(eventName); }
    public int getStatusID() { return statusID; }
    public int getEventTypeID() { return eventTypeID; }

    public static class Builder {
        private String userName;
        private String password;
        private int reservationID;
        private int roomID;
        private XMLGregorianCalendar bookingDate;
        private XMLGregorianCalendar startTime;
        private XMLGregorianCalendar endTime;
        private String eventName;
        private int statusID;
        private int eventTypeID;
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder reservationID(int reservationID) { this.reservationID = reservationID; return this; }
        public Builder roomID(int roomID) { this.roomID = roomID; return this; }
        public Builder bookingDate(XMLGregorianCalendar bookingDate) { this.bookingDate = bookingDate; return this; }
        public Builder startTime(XMLGregorianCalendar startTime) { this.startTime = startTime; return this; }
        public Builder endTime(XMLGregorianCalendar endTime) { this.endTime = endTime; return this; }
        public Builder eventName(String eventName) { this.eventName = eventName; return this; }
        public Builder statusID(int statusID) { this.statusID = statusID; return this; }
        public Builder eventTypeID(int eventTypeID) { this.eventTypeID = eventTypeID; return this; }
        public AddBooking build() { return new AddBooking(this); }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddBooking that = (AddBooking) o;
        return reservationID == that.reservationID &&
                roomID == that.roomID &&
                statusID == that.statusID &&
                eventTypeID == that.eventTypeID &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(bookingDate, that.bookingDate) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(eventName, that.eventName);
    }
    @Override
    public int hashCode() {
        return Objects.hash(userName, password, reservationID, roomID, bookingDate, startTime, endTime, eventName, statusID, eventTypeID);
    }
    @Override
    public String toString() {
        return "AddBooking{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", reservationID=" + reservationID +
                ", roomID=" + roomID +
                ", bookingDate=" + bookingDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", eventName='" + eventName + '\'' +
                ", statusID=" + statusID +
                ", eventTypeID=" + eventTypeID +
                '}';
    }
}
