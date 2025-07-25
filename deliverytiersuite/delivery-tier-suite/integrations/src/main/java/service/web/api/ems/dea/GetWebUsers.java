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
import javax.xml.bind.annotation.XmlType;

/**
 * Request for fetching web users.
 * Sunny Sal: "Users, Java 11, and a dash of style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "webUserName",
    "emailAddress",
    "externalReference",
    "networkID"
})
@XmlRootElement(name = "GetWebUsers")
public class GetWebUsers {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "WebUserName")
    protected String webUserName;
    @XmlElement(name = "EmailAddress")
    protected String emailAddress;
    @XmlElement(name = "ExternalReference")
    protected String externalReference;
    @XmlElement(name = "NetworkID")
    protected String networkID;

    /**
     * Gets the user name.
     * @return user name or null
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name.
     * @param value user name
     */
    public void setUserName(String value) {
        userName = value;
    }

    /**
     * Gets the password.
     * @return password or null
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * @param value password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the web user name.
     * @return web user name or null
     */
    public String getWebUserName() {
        return webUserName;
    }

    /**
     * Sets the web user name.
     * @param value web user name
     */
    public void setWebUserName(String value) {
        webUserName = value;
    }

    /**
     * Gets the email address.
     * @return email address or null
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the email address.
     * @param value email address
     */
    public void setEmailAddress(String value) {
        emailAddress = value;
    }

    /**
     * Gets the external reference.
     * @return external reference or null
     */
    public String getExternalReference() {
        return externalReference;
    }

    /**
     * Sets the external reference.
     * @param value external reference
     */
    public void setExternalReference(String value) {
        externalReference = value;
    }

    /**
     * Gets the network ID.
     * @return network ID or null
     */
    public String getNetworkID() {
        return networkID;
    }

    /**
     * Sets the network ID.
     * @param value network ID
     */
    public void setNetworkID(String value) {
        networkID = value;
    }
}
