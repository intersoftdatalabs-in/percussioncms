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
 * Java 11+ refactored version of GetAllBookings2 SOAP request.
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
    "viewComboRoomComponents",
    "udfDefID",
    "udfValue"
})
@XmlRootElement(name = "GetAllBookings2")
public final class GetAllBookings2 {
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
    @XmlElement(name = "UDFDefID")
    private final int udfDefID;
    @XmlElement(name = "UDFValue")
    private final String udfValue;

    private GetAllBookings2(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.buildingID = builder.buildingID;
        this.viewComboRoomComponents = builder.viewComboRoomComponents;
        this.udfDefID = builder.udfDefID;
        this.udfValue = builder.udfValue;
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
    /** @return UDF definition ID. */
    public int getUDFDefID() {
        return udfDefID;
    }
    /** @return Optional UDF value. */
    public java.util.Optional<String> getUDFValue() {
        return java.util.Optional.ofNullable(udfValue);
    }

    @Override
    public String toString() {
        return "GetAllBookings2{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", buildingID=" + buildingID +
                ", viewComboRoomComponents=" + viewComboRoomComponents +
                ", udfDefID=" + udfDefID +
                ", udfValue='" + udfValue + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllBookings2)) return false;
        var that = (GetAllBookings2) o;
        return buildingID == that.buildingID &&
                viewComboRoomComponents == that.viewComboRoomComponents &&
                udfDefID == that.udfDefID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(startDate, that.startDate) &&
                java.util.Objects.equals(endDate, that.endDate) &&
                java.util.Objects.equals(udfValue, that.udfValue);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, startDate, endDate, buildingID, viewComboRoomComponents, udfDefID, udfValue);
    }

    /**
     * Builder for GetAllBookings2. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private XMLGregorianCalendar startDate;
        private XMLGregorianCalendar endDate;
        private int buildingID;
        private boolean viewComboRoomComponents;
        private int udfDefID;
        private String udfValue;

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
        public Builder udfDefID(int udfDefID) {
            this.udfDefID = udfDefID;
            return this;
        }
        public Builder udfValue(String udfValue) {
            this.udfValue = udfValue;
            return this;
        }
        public GetAllBookings2 build() {
            if (startDate == null) throw new IllegalArgumentException("StartDate is required");
            if (endDate == null) throw new IllegalArgumentException("EndDate is required");
            return new GetAllBookings2(this);
        }
    }
}
