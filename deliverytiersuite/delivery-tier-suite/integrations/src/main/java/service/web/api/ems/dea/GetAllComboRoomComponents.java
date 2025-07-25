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
 * Java 11+ refactored version of GetAllComboRoomComponents SOAP request.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password"
})
@XmlRootElement(name = "GetAllComboRoomComponents")
public final class GetAllComboRoomComponents {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;

    private GetAllComboRoomComponents(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
    }

    /**
     * @return Optional user name for authentication.
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }

    /**
     * @return Optional password for authentication.
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }

    @Override
    public String toString() {
        return "GetAllComboRoomComponents{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAllComboRoomComponents that = (GetAllComboRoomComponents) o;
        return java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password);
    }

    /**
     * Builder for GetAllComboRoomComponents. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;

        public Builder() {}

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public GetAllComboRoomComponents build() {
            return new GetAllComboRoomComponents(this);
        }
    }
}
