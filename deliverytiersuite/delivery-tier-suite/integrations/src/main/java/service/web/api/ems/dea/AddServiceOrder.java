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
 *         &lt;element name="CategoryID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BookingID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="TimeStart" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="TimeEnd" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="ServiceTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="StateID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="EstimatedCount" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="GuaranteedCount" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ActualCount" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
    "categoryID",
    "bookingID",
    "timeStart",
    "timeEnd",
    "serviceTypeID",
    "stateID",
    "estimatedCount",
    "guaranteedCount",
    "actualCount"
})
@XmlRootElement(name = "AddServiceOrder")
public final class AddServiceOrder {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "CategoryID")
    private int categoryID;
    @XmlElement(name = "BookingID")
    private int bookingID;
    @XmlElement(name = "TimeStart", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar timeStart;
    @XmlElement(name = "TimeEnd", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar timeEnd;
    @XmlElement(name = "ServiceTypeID")
    private int serviceTypeID;
    @XmlElement(name = "StateID")
    private int stateID;
    @XmlElement(name = "EstimatedCount")
    private int estimatedCount;
    @XmlElement(name = "GuaranteedCount")
    private int guaranteedCount;
    @XmlElement(name = "ActualCount")
    private int actualCount;

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
    public int getCategoryID() {
        return categoryID;
    }
    public void setCategoryID(int value) {
        this.categoryID = value;
    }
    public int getBookingID() {
        return bookingID;
    }
    public void setBookingID(int value) {
        this.bookingID = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getTimeStart() {
        return java.util.Optional.ofNullable(timeStart);
    }
    public void setTimeStart(XMLGregorianCalendar value) {
        this.timeStart = value;
    }
    public java.util.Optional<XMLGregorianCalendar> getTimeEnd() {
        return java.util.Optional.ofNullable(timeEnd);
    }
    public void setTimeEnd(XMLGregorianCalendar value) {
        this.timeEnd = value;
    }
    public int getServiceTypeID() {
        return serviceTypeID;
    }
    public void setServiceTypeID(int value) {
        this.serviceTypeID = value;
    }
    public int getStateID() {
        return stateID;
    }
    public void setStateID(int value) {
        this.stateID = value;
    }
    public int getEstimatedCount() {
        return estimatedCount;
    }
    public void setEstimatedCount(int value) {
        this.estimatedCount = value;
    }
    public int getGuaranteedCount() {
        return guaranteedCount;
    }
    public void setGuaranteedCount(int value) {
        this.guaranteedCount = value;
    }
    public int getActualCount() {
        return actualCount;
    }
    public void setActualCount(int value) {
        this.actualCount = value;
    }

    @Override
    public String toString() {
        return "AddServiceOrder{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", categoryID=" + categoryID +
                ", bookingID=" + bookingID +
                ", timeStart=" + timeStart +
                ", timeEnd=" + timeEnd +
                ", serviceTypeID=" + serviceTypeID +
                ", stateID=" + stateID +
                ", estimatedCount=" + estimatedCount +
                ", guaranteedCount=" + guaranteedCount +
                ", actualCount=" + actualCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddServiceOrder that = (AddServiceOrder) o;
        return categoryID == that.categoryID &&
                bookingID == that.bookingID &&
                serviceTypeID == that.serviceTypeID &&
                stateID == that.stateID &&
                estimatedCount == that.estimatedCount &&
                guaranteedCount == that.guaranteedCount &&
                actualCount == that.actualCount &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(timeStart, that.timeStart) &&
                java.util.Objects.equals(timeEnd, that.timeEnd);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, categoryID, bookingID, timeStart, timeEnd, serviceTypeID, stateID, estimatedCount, guaranteedCount, actualCount);
    }
}
