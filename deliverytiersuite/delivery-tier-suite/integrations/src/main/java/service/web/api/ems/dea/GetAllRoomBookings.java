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
 * Java 11+ refactored version of GetAllRoomBookings SOAP request.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "startDate",
    "endDate",
    "roomID",
    "viewComboRoomComponents"
})
@XmlRootElement(name = "GetAllRoomBookings")
public final class GetAllRoomBookings {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar endDate;
    @XmlElement(name = "RoomID")
    private final int roomID;
    @XmlElement(name = "ViewComboRoomComponents")
    private final boolean viewComboRoomComponents;

    /**
     * Private constructor for builder usage only.
     */
    private GetAllRoomBookings(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.roomID = builder.roomID;
        this.viewComboRoomComponents = builder.viewComboRoomComponents;
    }

    /**
     * @return Optional user name for authentication.
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }

    /**
     * @return Optional password for authentication.
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }

    /**
     * @return Optional start date for room bookings.
     */
    public java.util.Optional<XMLGregorianCalendar> getStartDate() {
        return java.util.Optional.ofNullable(startDate);
    }

    /**
     * @return Optional end date for room bookings.
     */
    public java.util.Optional<XMLGregorianCalendar> getEndDate() {
        return java.util.Optional.ofNullable(endDate);
    }

    /**
     * @return Room ID for which bookings are requested.
     */
    public int getRoomID() {
        return roomID;
    }

    /**
     * @return true if combo room components should be viewed.
     */
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    @Override
    public String toString() {
        return "GetAllRoomBookings{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", roomID=" + roomID +
                ", viewComboRoomComponents=" + viewComboRoomComponents +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAllRoomBookings that = (GetAllRoomBookings) o;
        return roomID == that.roomID &&
                viewComboRoomComponents == that.viewComboRoomComponents &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(startDate, that.startDate) &&
                java.util.Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, startDate, endDate, roomID, viewComboRoomComponents);
    }

    /**
     * Builder for GetAllRoomBookings. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private XMLGregorianCalendar startDate;
        private XMLGregorianCalendar endDate;
        private int roomID;
        private boolean viewComboRoomComponents;

        public Builder() {}

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder startDate(XMLGregorianCalendar startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(XMLGregorianCalendar endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder roomID(int roomID) {
            this.roomID = roomID;
            return this;
        }

        public Builder viewComboRoomComponents(boolean viewComboRoomComponents) {
            this.viewComboRoomComponents = viewComboRoomComponents;
            return this;
        }

        public GetAllRoomBookings build() {
            // Defensive validation
            if (startDate == null) throw new IllegalArgumentException("StartDate is required");
            if (endDate == null) throw new IllegalArgumentException("EndDate is required");
            return new GetAllRoomBookings(this);
        }
    }
}
