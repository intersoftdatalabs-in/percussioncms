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
 * Java 11 Modernized: UpdateWebUserOptions SOAP request model.
 * <p>
 * Represents the request payload for the UpdateWebUserOptions endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Options updated—because every hero deserves choices!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "webUserId",
        "optionName",
        "optionValue"
    }
)
@XmlRootElement(name = "UpdateWebUserOptions")
public class UpdateWebUserOptions {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "WebUserId")
    private int webUserId;

    @XmlElement(name = "OptionName")
    private String optionName;

    @XmlElement(name = "OptionValue")
    private String optionValue;

    private UpdateWebUserOptions(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.webUserId = builder.webUserId;
        this.optionName = builder.optionName;
        this.optionValue = builder.optionValue;
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
     * Gets the web user ID.
     *
     * @return web user ID
     */
    public int getWebUserId() {
        return webUserId;
    }

    /**
     * Gets the option name.
     *
     * @return Optional option name
     */
    public Optional<String> getOptionName() {
        return Optional.ofNullable(optionName);
    }

    /**
     * Gets the option value.
     *
     * @return Optional option value
     */
    public Optional<String> getOptionValue() {
        return Optional.ofNullable(optionValue);
    }

    /**
     * Builder for UpdateWebUserOptions.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int webUserId;
        private String optionName;
        private String optionValue;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withWebUserId(int webUserId) {
            this.webUserId = webUserId;
            return this;
        }

        public Builder withOptionName(String optionName) {
            this.optionName = optionName;
            return this;
        }

        public Builder withOptionValue(String optionValue) {
            this.optionValue = optionValue;
            return this;
        }

        public UpdateWebUserOptions build() {
            return new UpdateWebUserOptions(this);
        }
    }
}
