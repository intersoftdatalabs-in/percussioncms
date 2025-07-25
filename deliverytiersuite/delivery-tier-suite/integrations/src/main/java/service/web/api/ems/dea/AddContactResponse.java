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
 * Java 11+ refactored SOAP response for AddContact.
 * <p>Immutable, builder-based, and Google Java Style. JAXB annotations retained for SOAP compatibility.</p>
 *
 * <p>Schema fragment:</p>
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="AddContactResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "addContactResult"
})
@XmlRootElement(name = "AddContactResponse")
public final class AddContactResponse {

    @XmlElement(name = "AddContactResult")
    private final String addContactResult;

    private AddContactResponse(Builder builder) {
        this.addContactResult = builder.addContactResult;
    }

    /**
     * @return Optional addContactResult
     */
    public Optional<String> getAddContactResult() {
        return Optional.ofNullable(addContactResult);
    }

    /**
     * Builder for AddContactResponse (Java 11+ style).
     */
    public static class Builder {
        private String addContactResult;

        public Builder addContactResult(String addContactResult) {
            this.addContactResult = addContactResult;
            return this;
        }

        public AddContactResponse build() {
            return new AddContactResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddContactResponse that = (AddContactResponse) o;
        return Objects.equals(addContactResult, that.addContactResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addContactResult);
    }

    @Override
    public String toString() {
        return "AddContactResponse{" +
                "addContactResult='" + addContactResult + '\'' +
                '}';
    }
}
