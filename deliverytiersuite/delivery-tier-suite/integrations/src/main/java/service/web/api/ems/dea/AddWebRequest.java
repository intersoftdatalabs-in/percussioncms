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

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Java 11 modernized: AddWebRequest for EMS SOAP API.
 * <p>
 * Represents a web request to add an event via the EMS web service.
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "eventName",
    "eventTypeID",
    "groupName",
    "contact",
    "phone",
    "fax",
    "emailAddress",
    "webUserID",
    "buildingID",
    "roomID",
    "bookingDate",
    "startTime",
    "endTime",
    "setupTypeID",
    "setupCount",
    "notes"
})
@XmlRootElement(name = "AddWebRequest")
public final class AddWebRequest {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "EventName")
    private String eventName;
    @XmlElement(name = "EventTypeID")
    private int eventTypeID;
    @XmlElement(name = "GroupName")
    private String groupName;
    @XmlElement(name = "Contact")
    private String contact;
    @XmlElement(name = "Phone")
    private String phone;
    @XmlElement(name = "Fax")
    private String fax;
    @XmlElement(name = "EmailAddress")
    private String emailAddress;
    @XmlElement(name = "WebUserID")
    private int webUserID;
    @XmlElement(name = "BuildingID")
    private int buildingID;
    @XmlElement(name = "RoomID")
    private int roomID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar bookingDate;
    @XmlElement(name = "StartTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar startTime;
    @XmlElement(name = "EndTime", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar endTime;
    @XmlElement(name = "SetupTypeID")
    private int setupTypeID;
    @XmlElement(name = "SetupCount")
    private int setupCount;
    @XmlElement(name = "Notes")
    private String notes;

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
    public java.util.Optional<String> getEventName() {
        return java.util.Optional.ofNullable(eventName);
    }
    public void setEventName(String value) {
        this.eventName = value;
    }
    public int getEventTypeID() {
        return eventTypeID;
    }
    public void setEventTypeID(int value) {
        this.eventTypeID = value;
    }
    public java.util.Optional<String> getGroupName() {
        return java.util.Optional.ofNullable(groupName);
    }
    public void setGroupName(String value) {
        this.groupName = value;
    }
    public java.util.Optional<String> getContact() {
        return java.util.Optional.ofNullable(contact);
    }
    public void setContact(String value) {
        this.contact = value;
    }
    public java.util.Optional<String> getPhone() {
        return java.util.Optional.ofNullable(phone);
    }
    public void setPhone(String value) {
        this.phone = value;
    }
    public java.util.Optional<String> getFax() {
        return java.util.Optional.ofNullable(fax);
    }
    public void setFax(String value) {
        this.fax = value;
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
    public java.util.Optional<XMLGregorianCalendar> getBookingDate() {
        return java.util.Optional.ofNullable(bookingDate);
    }
    public void setBookingDate(XMLGregorianCalendar value) {
        this.bookingDate = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getStartTime() {
        return java.util.Optional.ofNullable(startTime);
    }
    public void setStartTime(XMLGregorianCalendar value) {
        this.startTime = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getEndTime() {
        return java.util.Optional.ofNullable(endTime);
    }
    public void setEndTime(XMLGregorianCalendar value) {
        this.endTime = value;
    }
    public int getSetupTypeID() {
        return setupTypeID;
    }
    public void setSetupTypeID(int value) {
        this.setupTypeID = value;
    }
    public int getSetupCount() {
        return setupCount;
    }
    public void setSetupCount(int value) {
        this.setupCount = value;
    }
    public java.util.Optional<String> getNotes() {
        return java.util.Optional.ofNullable(notes);
    }
    public void setNotes(String value) {
        this.notes = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddWebRequest that = (AddWebRequest) o;
        return eventTypeID == that.eventTypeID &&
                webUserID == that.webUserID &&
                buildingID == that.buildingID &&
                roomID == that.roomID &&
                setupTypeID == that.setupTypeID &&
                setupCount == that.setupCount &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(groupName, that.groupName) &&
                Objects.equals(contact, that.contact) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(fax, that.fax) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(bookingDate, that.bookingDate) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(notes, that.notes);
    }
    @Override
    public int hashCode() {
        return Objects.hash(userName, password, eventName, eventTypeID, groupName, contact, phone, fax, emailAddress, webUserID, buildingID, roomID, bookingDate, startTime, endTime, setupTypeID, setupCount, notes);
    }
    @Override
    public String toString() {
        return "AddWebRequest{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", eventName='" + eventName + '\'' +
                ", eventTypeID=" + eventTypeID +
                ", groupName='" + groupName + '\'' +
                ", contact='" + contact + '\'' +
                ", phone='" + phone + '\'' +
                ", fax='" + fax + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", webUserID=" + webUserID +
                ", buildingID=" + buildingID +
                ", roomID=" + roomID +
                ", bookingDate=" + bookingDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", setupTypeID=" + setupTypeID +
                ", setupCount=" + setupCount +
                ", notes='" + notes + '\'' +
                '}';
    }
}
