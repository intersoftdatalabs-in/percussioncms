// REFACTORED: CP-JAVA11
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
 * Java 11 modernized: AutoCheckin for EMS SOAP API.
 * <p>
 * Represents a request for auto check-in via the EMS web service.
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "emailAddress",
    "webUserID",
    "udfid",
    "udfValue",
    "buildingID",
    "roomID",
    "floorID",
    "bookingDate",
    "eventType"
})
@XmlRootElement(name = "AutoCheckin")
public final class AutoCheckin {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "EmailAddress")
    private String emailAddress;
    @XmlElement(name = "WebUserID")
    private int webUserID;
    @XmlElement(name = "UDFID")
    private int udfid;
    @XmlElement(name = "UDFValue")
    private String udfValue;
    @XmlElement(name = "BuildingID")
    private int buildingID;
    @XmlElement(name = "RoomID")
    private int roomID;
    @XmlElement(name = "FloorID")
    private int floorID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar bookingDate;
    @XmlElement(name = "EventType")
    private int eventType;

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
    public java.util.Optional<String> getEmailAddress() {
        return java.util.Optional.ofNullable(emailAddress);
    }
    public void setEmailAddress(String value) {
        this.emailAddress = value;
    }
    public int getWebUserID() {
        return webUserID;
    }
    public void setWebUserID(int value) {
        this.webUserID = value;
    }
    public int getUDFID() {
        return udfid;
    }
    public void setUDFID(int value) {
        this.udfid = value;
    }
    public java.util.Optional<String> getUDFValue() {
        return java.util.Optional.ofNullable(udfValue);
    }
    public void setUDFValue(String value) {
        this.udfValue = value;
    }
    public int getBuildingID() {
        return buildingID;
    }
    public void setBuildingID(int value) {
        this.buildingID = value;
    }
    public int getRoomID() {
        return roomID;
    }
    public void setRoomID(int value) {
        this.roomID = value;
    }
    public int getFloorID() {
        return floorID;
    }
    public void setFloorID(int value) {
        this.floorID = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getBookingDate() {
        return java.util.Optional.ofNullable(bookingDate);
    }
    public void setBookingDate(XMLGregorianCalendar value) {
        this.bookingDate = value;
    }
    public int getEventType() {
        return eventType;
    }
    public void setEventType(int value) {
        this.eventType = value;
    }

    @Override
    public String toString() {
        return "AutoCheckin{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", emailAddress='" + emailAddress + '\'' +
                ", webUserID=" + webUserID +
                ", udfid=" + udfid +
                ", udfValue='" + udfValue + '\'' +
                ", buildingID=" + buildingID +
                ", roomID=" + roomID +
                ", floorID=" + floorID +
                ", bookingDate=" + bookingDate +
                ", eventType=" + eventType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoCheckin that = (AutoCheckin) o;
        return webUserID == that.webUserID &&
                udfid == that.udfid &&
                buildingID == that.buildingID &&
                roomID == that.roomID &&
                floorID == that.floorID &&
                eventType == that.eventType &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(emailAddress, that.emailAddress) &&
                java.util.Objects.equals(udfValue, that.udfValue) &&
                java.util.Objects.equals(bookingDate, that.bookingDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, emailAddress, webUserID, udfid, udfValue, buildingID, roomID, floorID, bookingDate, eventType);
    }
}
