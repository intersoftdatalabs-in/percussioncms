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
 * Java 11+ refactored SOAP request for GetBuildingHours.
 * <p>Immutable, builder-based, and Google Java Style. JAXB annotations retained for SOAP compatibility.</p>
 *
 * <p>Schema fragment:</p>
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;element name="Buildings" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/>&lt;element name="BuildingHoursDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>&lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "buildings",
    "buildingHoursDate"
})
@XmlRootElement(name = "GetBuildingHours")
public final class GetBuildingHours {

    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "Buildings")
    private final ArrayOfInt buildings;
    @XmlElement(name = "BuildingHoursDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar buildingHoursDate;

    /**
     * Private constructor for builder.
     */
    private GetBuildingHours(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.buildings = builder.buildings;
        this.buildingHoursDate = Objects.requireNonNull(builder.buildingHoursDate, "buildingHoursDate must not be null");
    }

    /**
     * @return Optional userName
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

    /**
     * @return Optional buildings
     */
    public Optional<ArrayOfInt> getBuildings() {
        return Optional.ofNullable(buildings);
    }

    /**
     * @return buildingHoursDate (never null)
     */
    public XMLGregorianCalendar getBuildingHoursDate() {
        return buildingHoursDate;
    }

    /**
     * Builder for GetBuildingHours (Java 11+ style).
     */
    public static class Builder {
        private String userName;
        private String password;
        private ArrayOfInt buildings;
        private XMLGregorianCalendar buildingHoursDate;

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

        public Builder buildingHoursDate(XMLGregorianCalendar buildingHoursDate) {
            this.buildingHoursDate = buildingHoursDate;
            return this;
        }

        public GetBuildingHours build() {
            return new GetBuildingHours(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildingHours that = (GetBuildingHours) o;
        return Objects.equals(userName, that.userName)
                && Objects.equals(password, that.password)
                && Objects.equals(buildings, that.buildings)
                && Objects.equals(buildingHoursDate, that.buildingHoursDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, buildings, buildingHoursDate);
    }

    @Override
    public String toString() {
        return "GetBuildingHours{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", buildings=" + buildings +
                ", buildingHoursDate=" + buildingHoursDate +
                '}';
    }
}
