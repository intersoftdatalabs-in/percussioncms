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
 * Java 11 Modernized: Immutable SOAP response for UpdateContact.
 * <p>
 * Represents the response payload for the UpdateContact endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Contact updated—now your address book is as fresh as your code!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateContactResult"
})
@XmlRootElement(name = "UpdateContactResponse")
public final class UpdateContactResponse {

    @XmlElement(name = "UpdateContactResult")
    private final String updateContactResult;

    private UpdateContactResponse(Builder builder) {
        this.updateContactResult = builder.updateContactResult;
    }

    /**
     * Gets the update contact result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateContactResult() {
        return Optional.ofNullable(updateContactResult);
    }

    /**
     * Builder for UpdateContactResponse.
     */
    public static class Builder {
        private String updateContactResult;

        public Builder withUpdateContactResult(String updateContactResult) {
            this.updateContactResult = updateContactResult;
            return this;
        }

        public UpdateContactResponse build() {
            return new UpdateContactResponse(this);
        }
    }
}
