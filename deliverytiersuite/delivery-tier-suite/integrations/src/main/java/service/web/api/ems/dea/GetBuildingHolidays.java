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
import java.util.Objects;
import java.util.Optional;

/**
 * Java 11+ refactored SOAP request for GetBuildingHolidays.
 * Immutable, builder-based, Google Java Style. JAXB annotations retained for SOAP compatibility.
 * Sunny Sal: "Building holidays, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "buildings",
    "holidayDate"
})
// REFACTORED: CP-JAVA11
// REFACTORED: CP-SOAP
@XmlRootElement(name = "GetBuildingHolidays")
public final class GetBuildingHolidays {

    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "Buildings")
    private final ArrayOfInt buildings;
    @XmlElement(name = "HolidayDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar holidayDate;

    private GetBuildingHolidays(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.buildings = builder.buildings;
        this.holidayDate = Objects.requireNonNull(builder.holidayDate, "holidayDate must not be null");
    }

    /**
     * Gets the user name.
     * @return Optional user name
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Gets the password.
     * @return Optional password
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Gets the buildings.
     * @return Optional buildings
     */
    public Optional<ArrayOfInt> getBuildings() {
        return Optional.ofNullable(buildings);
    }

    /**
     * Gets the holiday date.
     * @return holiday date (never null)
     */
    public XMLGregorianCalendar getHolidayDate() {
        return holidayDate;
    }

    /**
     * Builder for GetBuildingHolidays (Java 11+ style).
     */
    public static class Builder {
        private String userName;
        private String password;
        private ArrayOfInt buildings;
        private XMLGregorianCalendar holidayDate;

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder buildings(ArrayOfInt buildings) {
            this.buildings = buildings;
            return this;
        }

        public Builder holidayDate(XMLGregorianCalendar holidayDate) {
            this.holidayDate = holidayDate;
            return this;
        }

        public GetBuildingHolidays build() {
            return new GetBuildingHolidays(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBuildingHolidays)) return false;
        var that = (GetBuildingHolidays) o;
        return Objects.equals(userName, that.userName)
                && Objects.equals(password, that.password)
                && Objects.equals(buildings, that.buildings)
                && Objects.equals(holidayDate, that.holidayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, buildings, holidayDate);
    }

    @Override
    public String toString() {
        return "GetBuildingHolidays{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", buildings=" + buildings +
                ", holidayDate=" + holidayDate +
                '}';
    }
}
