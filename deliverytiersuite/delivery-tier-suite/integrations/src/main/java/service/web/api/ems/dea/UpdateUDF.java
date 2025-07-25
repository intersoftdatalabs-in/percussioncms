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

import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11 Modernized: UpdateUDF SOAP request model.
 * <p>
 * Represents the request payload for the UpdateUDF endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Updating UDFs—because every function deserves a fresh start!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "udfId",
        "newValue"
    }
)
@XmlRootElement(name = "UpdateUDF")
public class UpdateUDF {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "UDFId")
    private int udfId;

    @XmlElement(name = "NewValue")
    private String newValue;

    private UpdateUDF(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.udfId = builder.udfId;
        this.newValue = builder.newValue;
    }

    /**
     * Gets the user name.
     *
     * @return Optional user name
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Gets the password.
     *
     * @return Optional password
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Gets the UDF ID.
     *
     * @return UDF ID
     */
    public int getUdfId() {
        return udfId;
    }

    /**
     * Gets the new value.
     *
     * @return Optional new value
     */
    public Optional<String> getNewValue() {
        return Optional.ofNullable(newValue);
    }

    /**
     * Builder for UpdateUDF.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int udfId;
        private String newValue;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withUdfId(int udfId) {
            this.udfId = udfId;
            return this;
        }

        public Builder withNewValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public UpdateUDF build() {
            return new UpdateUDF(this);
        }
    }
}
