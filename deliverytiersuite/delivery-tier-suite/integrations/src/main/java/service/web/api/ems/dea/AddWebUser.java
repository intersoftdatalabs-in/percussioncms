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
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11 modernized: AddWebUser request for EMS SOAP API.
 *
 * <p>Represents a user to be added via the EMS web service.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
      "userName",
      "password",
      "webUserName",
      "webUserPassword",
      "emailAddress",
      "phone",
      "fax",
      "externalReference",
      "networkID",
      "timeZoneID",
      "statusID",
      "webSecurityTemplateID",
      "webProcessTemplates",
      "groups",
      "validated"
    })
@XmlRootElement(name = "AddWebUser")
public final class AddWebUser {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "WebUserName")
    private String webUserName;
    @XmlElement(name = "WebUserPassword")
    private String webUserPassword;
    @XmlElement(name = "EmailAddress")
    private String emailAddress;
    @XmlElement(name = "Phone")
    private String phone;
    @XmlElement(name = "Fax")
    private String fax;
    @XmlElement(name = "ExternalReference")
    private String externalReference;
    @XmlElement(name = "NetworkID")
    private String networkID;
    @XmlElement(name = "TimeZoneID")
    private int timeZoneID;
    @XmlElement(name = "StatusID")
    private int statusID;
    @XmlElement(name = "WebSecurityTemplateID")
    private int webSecurityTemplateID;
    @XmlElement(name = "WebProcessTemplates")
    private ArrayOfInt webProcessTemplates;
    @XmlElement(name = "Groups")
    private ArrayOfInt groups;
    @XmlElement(name = "Validated")
    private boolean validated;

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
    public java.util.Optional<String> getWebUserName() {
        return java.util.Optional.ofNullable(webUserName);
    }
    public void setWebUserName(String value) {
        this.webUserName = value;
    }
    public java.util.Optional<String> getWebUserPassword() {
        return java.util.Optional.ofNullable(webUserPassword);
    }
    public void setWebUserPassword(String value) {
        this.webUserPassword = value;
    }
    public java.util.Optional<String> getEmailAddress() {
        return java.util.Optional.ofNullable(emailAddress);
    }
    public void setEmailAddress(String value) {
        this.emailAddress = value;
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
    public java.util.Optional<String> getExternalReference() {
        return java.util.Optional.ofNullable(externalReference);
    }
    public void setExternalReference(String value) {
        this.externalReference = value;
    }
    public java.util.Optional<String> getNetworkID() {
        return java.util.Optional.ofNullable(networkID);
    }
    public void setNetworkID(String value) {
        this.networkID = value;
    }
    public int getTimeZoneID() {
        return timeZoneID;
    }
    public void setTimeZoneID(int value) {
        this.timeZoneID = value;
    }
    public int getStatusID() {
        return statusID;
    }
    public void setStatusID(int value) {
        this.statusID = value;
    }
    public int getWebSecurityTemplateID() {
        return webSecurityTemplateID;
    }
    public void setWebSecurityTemplateID(int value) {
        this.webSecurityTemplateID = value;
    }
    public java.util.Optional<ArrayOfInt> getWebProcessTemplates() {
        return java.util.Optional.ofNullable(webProcessTemplates);
    }
    public void setWebProcessTemplates(ArrayOfInt value) {
        this.webProcessTemplates = value;
    }
    public java.util.Optional<ArrayOfInt> getGroups() {
        return java.util.Optional.ofNullable(groups);
    }
    public void setGroups(ArrayOfInt value) {
        this.groups = value;
    }
    public boolean isValidated() {
        return validated;
    }
    public void setValidated(boolean value) {
        this.validated = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddWebUser that = (AddWebUser) o;
        return timeZoneID == that.timeZoneID &&
                statusID == that.statusID &&
                webSecurityTemplateID == that.webSecurityTemplateID &&
                validated == that.validated &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(webUserName, that.webUserName) &&
                Objects.equals(webUserPassword, that.webUserPassword) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(fax, that.fax) &&
                Objects.equals(externalReference, that.externalReference) &&
                Objects.equals(networkID, that.networkID) &&
                Objects.equals(webProcessTemplates, that.webProcessTemplates) &&
                Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, webUserName, webUserPassword, emailAddress, phone, fax, externalReference, networkID, timeZoneID, statusID, webSecurityTemplateID, webProcessTemplates, groups, validated);
    }

    @Override
    public String toString() {
        return "AddWebUser{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", webUserName='" + webUserName + '\'' +
                ", webUserPassword='[PROTECTED]'" +
                ", emailAddress='" + emailAddress + '\'' +
                ", phone='" + phone + '\'' +
                ", fax='" + fax + '\'' +
                ", externalReference='" + externalReference + '\'' +
                ", networkID='" + networkID + '\'' +
                ", timeZoneID=" + timeZoneID +
                ", statusID=" + statusID +
                ", webSecurityTemplateID=" + webSecurityTemplateID +
                ", webProcessTemplates=" + webProcessTemplates +
                ", groups=" + groups +
                ", validated=" + validated +
                '}';
    }
}
