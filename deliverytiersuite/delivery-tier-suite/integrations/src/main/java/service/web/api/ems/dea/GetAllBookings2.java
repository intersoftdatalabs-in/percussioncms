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
 *         &lt;element name="StartDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="BuildingID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ViewComboRoomComponents" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="UDFDefID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="UDFValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar endDate;
    @XmlElement(name = "BuildingID")
    private int buildingID;
    @XmlElement(name = "ViewComboRoomComponents")
    private boolean viewComboRoomComponents;
    @XmlElement(name = "UDFDefID")
    private int udfDefID;
    @XmlElement(name = "UDFValue")
    private String udfValue;

    // --- Modernized Getters/Setters ---
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    public void setUserName(String value) {
        this.userName = value;
    }
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    public void setPassword(String value) {
        this.password = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getStartDate() {
        return java.util.Optional.ofNullable(startDate);
    }
    public void setStartDate(XMLGregorianCalendar value) {
        this.startDate = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getEndDate() {
        return java.util.Optional.ofNullable(endDate);
    }
    public void setEndDate(XMLGregorianCalendar value) {
        this.endDate = value;
    }
    public int getBuildingID() {
        return buildingID;
    }
    public void setBuildingID(int value) {
        this.buildingID = value;
    }
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }
    public void setViewComboRoomComponents(boolean value) {
        this.viewComboRoomComponents = value;
    }
    public int getUDFDefID() {
        return udfDefID;
    }
    public void setUDFDefID(int value) {
        this.udfDefID = value;
    }
    public java.util.Optional<String> getUDFValue() {
        return java.util.Optional.ofNullable(udfValue);
    }
    public void setUDFValue(String value) {
        this.udfValue = value;
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
        if (o == null || getClass() != o.getClass()) return false;
        GetAllBookings2 that = (GetAllBookings2) o;
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
}
