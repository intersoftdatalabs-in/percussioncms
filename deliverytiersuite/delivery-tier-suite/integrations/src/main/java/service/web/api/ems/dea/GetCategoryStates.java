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
 *         &lt;element name="CategoryID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
    "categoryID"
})
@XmlRootElement(name = "GetCategoryStates")
public final class GetCategoryStates {

    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "CategoryID")
    private final int categoryID;

    private GetCategoryStates(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.categoryID = builder.categoryID;
    }

    /**
     * Gets the value of the userName property.
     * @return the user name, or null if not set
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the value of the password property.
     * @return the password, or null if not set
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the value of the categoryID property.
     * @return category ID as int
     */
    public int getCategoryID() {
        return categoryID;
    }

    /**
     * Builder for GetCategoryStates.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int categoryID;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withCategoryID(int categoryID) {
            this.categoryID = categoryID;
            return this;
        }

        public GetCategoryStates build() {
            return new GetCategoryStates(this);
        }
    }
}
