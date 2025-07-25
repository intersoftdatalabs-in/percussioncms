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
 *         &lt;element name="Buildings" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="HolidayDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
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
        this.holidayDate = java.util.Objects.requireNonNull(builder.holidayDate, "holidayDate must not be null");
    }

    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    public java.util.Optional<ArrayOfInt> getBuildings() {
        return java.util.Optional.ofNullable(buildings);
    }
    public XMLGregorianCalendar getHolidayDate() {
        return holidayDate;
    }

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
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildingHolidays that = (GetBuildingHolidays) o;
        return java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(buildings, that.buildings) &&
                java.util.Objects.equals(holidayDate, that.holidayDate);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, buildings, holidayDate);
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
