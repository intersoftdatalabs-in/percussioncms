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
 *         &lt;element name="GroupName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="GroupTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Address1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Address2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="City" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="State" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ZipCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Country" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Phone" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Fax" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="EmailAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ExternalReference" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "groupName",
    "groupTypeID",
    "address1",
    "address2",
    "city",
    "state",
    "zipCode",
    "country",
    "phone",
    "fax",
    "emailAddress",
    "externalReference"
})
@XmlRootElement(name = "AddGroup")
public final class AddGroup {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "GroupName")
    private final String groupName;
    @XmlElement(name = "GroupTypeID")
    private final int groupTypeID;
    @XmlElement(name = "Address1")
    private final String address1;
    @XmlElement(name = "Address2")
    private final String address2;
    @XmlElement(name = "City")
    private final String city;
    @XmlElement(name = "State")
    private final String state;
    @XmlElement(name = "ZipCode")
    private final String zipCode;
    @XmlElement(name = "Country")
    private final String country;
    @XmlElement(name = "Phone")
    private final String phone;
    @XmlElement(name = "Fax")
    private final String fax;
    @XmlElement(name = "EmailAddress")
    private final String emailAddress;
    @XmlElement(name = "ExternalReference")
    private final String externalReference;

    private AddGroup(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.groupName = builder.groupName;
        this.groupTypeID = builder.groupTypeID;
        this.address1 = builder.address1;
        this.address2 = builder.address2;
        this.city = builder.city;
        this.state = builder.state;
        this.zipCode = builder.zipCode;
        this.country = builder.country;
        this.phone = builder.phone;
        this.fax = builder.fax;
        this.emailAddress = builder.emailAddress;
        this.externalReference = builder.externalReference;
    }

    public java.util.Optional<String> getUserName() { return java.util.Optional.ofNullable(userName); }
    public java.util.Optional<String> getPassword() { return java.util.Optional.ofNullable(password); }
    public java.util.Optional<String> getGroupName() { return java.util.Optional.ofNullable(groupName); }
    public int getGroupTypeID() { return groupTypeID; }
    public java.util.Optional<String> getAddress1() { return java.util.Optional.ofNullable(address1); }
    public java.util.Optional<String> getAddress2() { return java.util.Optional.ofNullable(address2); }
    public java.util.Optional<String> getCity() { return java.util.Optional.ofNullable(city); }
    public java.util.Optional<String> getState() { return java.util.Optional.ofNullable(state); }
    public java.util.Optional<String> getZipCode() { return java.util.Optional.ofNullable(zipCode); }
    public java.util.Optional<String> getCountry() { return java.util.Optional.ofNullable(country); }
    public java.util.Optional<String> getPhone() { return java.util.Optional.ofNullable(phone); }
    public java.util.Optional<String> getFax() { return java.util.Optional.ofNullable(fax); }
    public java.util.Optional<String> getEmailAddress() { return java.util.Optional.ofNullable(emailAddress); }
    public java.util.Optional<String> getExternalReference() { return java.util.Optional.ofNullable(externalReference); }

    public static class Builder {
        private String userName;
        private String password;
        private String groupName;
        private int groupTypeID;
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String phone;
        private String fax;
        private String emailAddress;
        private String externalReference;
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder groupName(String groupName) { this.groupName = groupName; return this; }
        public Builder groupTypeID(int groupTypeID) { this.groupTypeID = groupTypeID; return this; }
        public Builder address1(String address1) { this.address1 = address1; return this; }
        public Builder address2(String address2) { this.address2 = address2; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder state(String state) { this.state = state; return this; }
        public Builder zipCode(String zipCode) { this.zipCode = zipCode; return this; }
        public Builder country(String country) { this.country = country; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder fax(String fax) { this.fax = fax; return this; }
        public Builder emailAddress(String emailAddress) { this.emailAddress = emailAddress; return this; }
        public Builder externalReference(String externalReference) { this.externalReference = externalReference; return this; }
        public AddGroup build() { return new AddGroup(this); }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddGroup that = (AddGroup) o;
        return groupTypeID == that.groupTypeID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(groupName, that.groupName) &&
                java.util.Objects.equals(address1, that.address1) &&
                java.util.Objects.equals(address2, that.address2) &&
                java.util.Objects.equals(city, that.city) &&
                java.util.Objects.equals(state, that.state) &&
                java.util.Objects.equals(zipCode, that.zipCode) &&
                java.util.Objects.equals(country, that.country) &&
                java.util.Objects.equals(phone, that.phone) &&
                java.util.Objects.equals(fax, that.fax) &&
                java.util.Objects.equals(emailAddress, that.emailAddress) &&
                java.util.Objects.equals(externalReference, that.externalReference);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, groupName, groupTypeID, address1, address2, city, state, zipCode, country, phone, fax, emailAddress, externalReference);
    }
    @Override
    public String toString() {
        return "AddGroup{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", groupName='" + groupName + '\'' +
                ", groupTypeID=" + groupTypeID +
                ", address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", country='" + country + '\'' +
                ", phone='" + phone + '\'' +
                ", fax='" + fax + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", externalReference='" + externalReference + '\'' +
                '}';
    }
}
