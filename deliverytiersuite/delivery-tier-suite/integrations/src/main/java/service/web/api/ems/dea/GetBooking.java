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
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="BookingID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
    "bookingID"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBooking")
public class GetBooking {
    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "BookingID")
    protected int bookingID;

    /**
     * Gets the value of the userName property.
     * @return Optional of userName
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /**
     * Sets the value of the userName property.
     * @param value allowed object is {@link String }
     */
    public void setUserName(String value) {
        this.userName = value;
    }
    /**
     * Gets the value of the password property.
     * @return Optional of password
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /**
     * Sets the value of the password property.
     * @param value allowed object is {@link String }
     */
    public void setPassword(String value) {
        this.password = value;
    }
    /**
     * Gets the value of the bookingID property.
     * @return bookingID
     */
    public int getBookingID() {
        return bookingID;
    }
    /**
     * Sets the value of the bookingID property.
     * @param value bookingID
     */
    public void setBookingID(int value) {
        this.bookingID = value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBooking that = (GetBooking) o;
        return bookingID == that.bookingID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, bookingID);
    }
    @Override
    public String toString() {
        return "GetBooking{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", bookingID=" + bookingID +
                '}';
    }
}
