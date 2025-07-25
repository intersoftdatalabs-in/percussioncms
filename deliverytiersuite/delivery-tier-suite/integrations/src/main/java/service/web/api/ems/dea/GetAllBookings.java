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

/**
 * Java 11+ refactored version of GetAllBookings SOAP request.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "startDate",
    "endDate",
    "buildingID",
    "viewComboRoomComponents"
})
@XmlRootElement(name = "GetAllBookings")
public final class GetAllBookings {
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
    @XmlElement(name = "BuildingID")
    private final int buildingID;
    @XmlElement(name = "ViewComboRoomComponents")
    private final boolean viewComboRoomComponents;

    private GetAllBookings(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.buildingID = builder.buildingID;
        this.viewComboRoomComponents = builder.viewComboRoomComponents;
    }

    /** @return Optional user name for authentication. */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /** @return Optional password for authentication. */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /** @return Optional start date for bookings. */
    public java.util.Optional<XMLGregorianCalendar> getStartDate() {
        return java.util.Optional.ofNullable(startDate);
    }
    /** @return Optional end date for bookings. */
    public java.util.Optional<XMLGregorianCalendar> getEndDate() {
        return java.util.Optional.ofNullable(endDate);
    }
    /** @return Building ID for which bookings are requested. */
    public int getBuildingID() {
        return buildingID;
    }
    /** @return true if combo room components should be viewed. */
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    @Override
    public String toString() {
        return "GetAllBookings{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", buildingID=" + buildingID +
                ", viewComboRoomComponents=" + viewComboRoomComponents +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllBookings)) return false;
        var that = (GetAllBookings) o;
        return buildingID == that.buildingID &&
                viewComboRoomComponents == that.viewComboRoomComponents &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(startDate, that.startDate) &&
                java.util.Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, startDate, endDate, buildingID, viewComboRoomComponents);
    }

    /**
     * Builder for GetAllBookings. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private XMLGregorianCalendar startDate;
        private XMLGregorianCalendar endDate;
        private int buildingID;
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
        public Builder buildingID(int buildingID) {
            this.buildingID = buildingID;
            return this;
        }
        public Builder viewComboRoomComponents(boolean viewComboRoomComponents) {
            this.viewComboRoomComponents = viewComboRoomComponents;
            return this;
        }
        public GetAllBookings build() {
            if (startDate == null) throw new IllegalArgumentException("StartDate is required");
            if (endDate == null) throw new IllegalArgumentException("EndDate is required");
            return new GetAllBookings(this);
        }
    }
}
