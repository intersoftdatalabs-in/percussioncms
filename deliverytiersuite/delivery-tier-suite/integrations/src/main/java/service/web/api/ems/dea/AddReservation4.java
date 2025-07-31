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
// REFACTORED: CP-SOAP
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
 * Java class for AddReservation4 SOAP request.
 * <p>
 * Represents a reservation request with all required and optional fields.
 * <p>
 * Fields are immutable and must be set via the Builder.
 * <p>
 * Password is protected in toString output for security.
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
    "billingReference"
})
@XmlRootElement(name = "AddReservation4")
public final class AddReservation4 {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "GroupID")
    private final int groupID;
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
    @XmlElement(name = "WebUserID")
    private final int webUserID;
    @XmlElement(name = "WebTemplateID")
    private final int webTemplateID;
    @XmlElement(name = "ReservationSourceID")
    private final int reservationSourceID;
    @XmlElement(name = "BillingReference")
    private final String billingReference;

    private AddReservation4(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.groupID = builder.groupID;
        this.roomID = builder.roomID;
        this.bookingDate = builder.bookingDate;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.eventName = builder.eventName;
        this.statusID = builder.statusID;
        this.eventTypeID = builder.eventTypeID;
        this.webUserID = builder.webUserID;
        this.webTemplateID = builder.webTemplateID;
        this.reservationSourceID = builder.reservationSourceID;
        this.billingReference = builder.billingReference;
    }

    /**
     * @return Optional user name
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * @return Optional password
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public int getGroupID() {
        return groupID;
    }

    public int getRoomID() {
        return roomID;
    }

    public XMLGregorianCalendar getBookingDate() {
        return bookingDate;
    }

    public XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    public XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * @return Optional event name
     */
    public Optional<String> getEventName() {
        return Optional.ofNullable(eventName);
    }

    public int getStatusID() {
        return statusID;
    }

    public int getEventTypeID() {
        return eventTypeID;
    }

    public int getWebUserID() {
        return webUserID;
    }

    public int getWebTemplateID() {
        return webTemplateID;
    }

    public int getReservationSourceID() {
        return reservationSourceID;
    }

    /**
     * @return Optional billing reference
     */
    public Optional<String> getBillingReference() {
        return Optional.ofNullable(billingReference);
    }

    /**
     * Builder for AddReservation4. Uses Optional for nullable fields.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int groupID;
        private int roomID;
        private XMLGregorianCalendar bookingDate;
        private XMLGregorianCalendar startTime;
        private XMLGregorianCalendar endTime;
        private String eventName;
        private int statusID;
        private int eventTypeID;
        private int webUserID;
        private int webTemplateID;
        private int reservationSourceID;
        private String billingReference;

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder groupID(int groupID) {
            this.groupID = groupID;
            return this;
        }

        public Builder roomID(int roomID) {
            this.roomID = roomID;
            return this;
        }

        public Builder bookingDate(XMLGregorianCalendar bookingDate) {
            this.bookingDate = bookingDate;
            return this;
        }

        public Builder startTime(XMLGregorianCalendar startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(XMLGregorianCalendar endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder statusID(int statusID) {
            this.statusID = statusID;
            return this;
        }

        public Builder eventTypeID(int eventTypeID) {
            this.eventTypeID = eventTypeID;
            return this;
        }

        public Builder webUserID(int webUserID) {
            this.webUserID = webUserID;
            return this;
        }

        public Builder webTemplateID(int webTemplateID) {
            this.webTemplateID = webTemplateID;
            return this;
        }

        public Builder reservationSourceID(int reservationSourceID) {
            this.reservationSourceID = reservationSourceID;
            return this;
        }

        public Builder billingReference(String billingReference) {
            this.billingReference = billingReference;
            return this;
        }

        public AddReservation4 build() {
            return new AddReservation4(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AddReservation4 that = (AddReservation4) o;
        return groupID == that.groupID
                && roomID == that.roomID
                && statusID == that.statusID
                && eventTypeID == that.eventTypeID
                && webUserID == that.webUserID
                && webTemplateID == that.webTemplateID
                && reservationSourceID == that.reservationSourceID
                && Objects.equals(userName, that.userName)
                && Objects.equals(password, that.password)
                && Objects.equals(bookingDate, that.bookingDate)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(eventName, that.eventName)
                && Objects.equals(billingReference, that.billingReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, groupID, roomID, bookingDate, startTime, endTime, eventName, statusID, eventTypeID, webUserID, webTemplateID, reservationSourceID, billingReference);
    }

    @Override
    public String toString() {
        // Password intentionally not shown for security reasons
        return "AddReservation4{" +
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
                '}';
    }
}
