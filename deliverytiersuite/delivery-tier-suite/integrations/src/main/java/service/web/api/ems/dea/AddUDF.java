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
 * Java 11 modernized: AddUDF for EMS SOAP API.
 * <p>
 * Represents a request to add a UDF via the EMS web service.
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "parentLevelID",
    "parentID",
    "udfDefID",
    "udfValue"
})
@XmlRootElement(name = "AddUDF")
public final class AddUDF {

    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "ParentLevelID")
    private int parentLevelID;
    @XmlElement(name = "ParentID")
    private int parentID;
    @XmlElement(name = "UDFDefID")
    private int udfDefID;
    @XmlElement(name = "UDFValue")
    private String udfValue;

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
    public int getParentLevelID() {
        return parentLevelID;
    }
    public void setParentLevelID(int value) {
        this.parentLevelID = value;
    }
    public int getParentID() {
        return parentID;
    }
    public void setParentID(int value) {
        this.parentID = value;
    }
    public int getUDFDefID() {
        return udfDefID;
    }
    public void setUDFDefID(int value) {
        this.udfDefID = value;
    }
    public java.util.Optional<String> getUDFValue() {
        return java.util.Optional.ofNullable(udfValue);
    }
    public void setUDFValue(String value) {
        this.udfValue = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddUDF addUDF = (AddUDF) o;
        return parentLevelID == addUDF.parentLevelID &&
                parentID == addUDF.parentID &&
                udfDefID == addUDF.udfDefID &&
                java.util.Objects.equals(userName, addUDF.userName) &&
                java.util.Objects.equals(password, addUDF.password) &&
                java.util.Objects.equals(udfValue, addUDF.udfValue);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, parentLevelID, parentID, udfDefID, udfValue);
    }
    @Override
    public String toString() {
        return "AddUDF{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", parentLevelID=" + parentLevelID +
                ", parentID=" + parentID +
                ", udfDefID=" + udfDefID +
                ", udfValue='" + udfValue + '\'' +
                '}';
    }
}
