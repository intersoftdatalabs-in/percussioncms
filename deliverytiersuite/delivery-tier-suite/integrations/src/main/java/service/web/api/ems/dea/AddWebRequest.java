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
public class AddWebRequest {
    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "EventName")
    protected String eventName;
    @XmlElement(name = "EventTypeID")
    protected int eventTypeID;
    @XmlElement(name = "GroupName")
    protected String groupName;
    @XmlElement(name = "Contact")
    protected String contact;
    @XmlElement(name = "Phone")
    protected String phone;
    @XmlElement(name = "Fax")
    protected String fax;
    @XmlElement(name = "EmailAddress")
    protected String emailAddress;
    @XmlElement(name = "WebUserID")
    protected int webUserID;
    @XmlElement(name = "BuildingID")
    protected int buildingID;
    @XmlElement(name = "RoomID")
    protected int roomID;
    @XmlElement(name = "BookingDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar bookingDate;
    @XmlElement(name = "StartTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startTime;
    @XmlElement(name = "EndTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endTime;
    @XmlElement(name = "SetupTypeID")
    protected int setupTypeID;
    @XmlElement(name = "SetupCount")
    protected int setupCount;
    @XmlElement(name = "Notes")
    protected String notes;

    /**
     * Gets the value of the userName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the value of the userName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserName(String value) {
        this.userName = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the eventName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the value of the eventName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventName(String value) {
        this.eventName = value;
    }

    /**
     * Gets the value of the eventTypeID property.
     * 
     */
    public int getEventTypeID() {
        return eventTypeID;
    }

    /**
     * Sets the value of the eventTypeID property.
     * 
     */
    public void setEventTypeID(int value) {
        this.eventTypeID = value;
    }

    /**
     * Gets the value of the groupName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the value of the groupName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroupName(String value) {
        this.groupName = value;
    }

    /**
     * Gets the value of the contact property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContact() {
        return contact;
    }

    /**
     * Sets the value of the contact property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContact(String value) {
        this.contact = value;
    }

    /**
     * Gets the value of the phone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the value of the phone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhone(String value) {
        this.phone = value;
    }

    /**
     * Gets the value of the fax property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFax() {
        return fax;
    }

    /**
     * Sets the value of the fax property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFax(String value) {
        this.fax = value;
    }

    /**
     * Gets the value of the emailAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the value of the emailAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmailAddress(String value) {
        this.emailAddress = value;
    }

    /**
     * Gets the value of the webUserID property.
     * 
     */
    public int getWebUserID() {
        return webUserID;
    }

    /**
     * Sets the value of the webUserID property.
     * 
     */
    public void setWebUserID(int value) {
        this.webUserID = value;
    }

    /**
     * Gets the value of the buildingID property.
     * 
     */
    public int getBuildingID() {
        return buildingID;
    }

    /**
     * Sets the value of the buildingID property.
     * 
     */
    public void setBuildingID(int value) {
        this.buildingID = value;
    }

    /**
     * Gets the value of the roomID property.
     * 
     */
    public int getRoomID() {
        return roomID;
    }

    /**
     * Sets the value of the roomID property.
     * 
     */
    public void setRoomID(int value) {
        this.roomID = value;
    }

    /**
     * Gets the value of the bookingDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getBookingDate() {
        return bookingDate;
    }

    /**
     * Sets the value of the bookingDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setBookingDate(XMLGregorianCalendar value) {
        this.bookingDate = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setStartTime(XMLGregorianCalendar value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setEndTime(XMLGregorianCalendar value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the setupTypeID property.
     * 
     */
    public int getSetupTypeID() {
        return setupTypeID;
    }

    /**
     * Sets the value of the setupTypeID property.
     * 
     */
    public void setSetupTypeID(int value) {
        this.setupTypeID = value;
    }

    /**
     * Gets the value of the setupCount property.
     * 
     */
    public int getSetupCount() {
        return setupCount;
    }

    /**
     * Sets the value of the setupCount property.
     * 
     */
    public void setSetupCount(int value) {
        this.setupCount = value;
    }

    /**
     * Gets the value of the notes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the value of the notes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
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
