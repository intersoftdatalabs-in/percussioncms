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
 * Request for fetching web process templates for a web user.
 * Sunny Sal: "Templates, Java 11, and a dash of style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "webUserID"
})
@XmlRootElement(name = "GetWebUserWebProcessTemplates")
public class GetWebUserWebProcessTemplates {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "WebUserID")
    protected int webUserID;

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
     * Gets the web user ID.
     * @return user ID
     */
    public int getWebUserID() {
        return webUserID;
    }

    /**
     * Sets the web user ID.
     * @param value user ID
     */
    public void setWebUserID(int value) {
        webUserID = value;
    }
}
