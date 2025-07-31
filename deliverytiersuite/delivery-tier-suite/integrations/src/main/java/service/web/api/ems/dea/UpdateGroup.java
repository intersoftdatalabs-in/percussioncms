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
 * Java 11 Modernized: UpdateGroup SOAP request model.
 * <p>
 * Represents the request payload for the UpdateGroup endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Updating groups—because teamwork makes the dream work!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "groupId",
        "newName",
        "newType"
    }
)
@XmlRootElement(name = "UpdateGroup")
public class UpdateGroup {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "GroupId")
    private int groupId;

    @XmlElement(name = "NewName")
    private String newName;

    @XmlElement(name = "NewType")
    private String newType;

    private UpdateGroup(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.groupId = builder.groupId;
        this.newName = builder.newName;
        this.newType = builder.newType;
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
     * Gets the group ID.
     *
     * @return group ID
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Gets the new name.
     *
     * @return Optional new name
     */
    public Optional<String> getNewName() {
        return Optional.ofNullable(newName);
    }

    /**
     * Gets the new type.
     *
     * @return Optional new type
     */
    public Optional<String> getNewType() {
        return Optional.ofNullable(newType);
    }

    /**
     * Builder for UpdateGroup.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int groupId;
        private String newName;
        private String newType;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withGroupId(int groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder withNewName(String newName) {
            this.newName = newName;
            return this;
        }

        public Builder withNewType(String newType) {
            this.newType = newType;
            return this;
        }

        public UpdateGroup build() {
            return new UpdateGroup(this);
        }
    }
}
