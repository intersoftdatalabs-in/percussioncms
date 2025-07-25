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

// REFACTORED: CP-JAVA11
// REFACTORED: CP-SOAP
package service.web.api.ems.dea;

import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11+ refactored SOAP request for GetBuildings.
 * <p>Immutable, builder-based, and Google Java Style. JAXB annotations retained for SOAP compatibility.</p>
 *
 * <p>Schema fragment:</p>
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password"
})
@XmlRootElement(name = "GetBuildings")
public final class GetBuildings {

    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;

    private GetBuildings(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
    }

    /**
     * @return Optional userName
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * @return Optional password
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Builder for GetBuildings (Java 11+ style).
     */
    public static class Builder {
        private String userName;
        private String password;

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public GetBuildings build() {
            return new GetBuildings(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildings that = (GetBuildings) o;
        return Objects.equals(userName, that.userName) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password);
    }

    @Override
    public String toString() {
        return "GetBuildings{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
