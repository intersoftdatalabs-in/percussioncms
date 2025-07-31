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
 * Java 11 Modernized: Immutable SOAP response for UpdateWebUserOptions.
 * <p>
 * Represents the response payload for the UpdateWebUserOptions endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Options updated—now your hero has even more powers!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateWebUserOptionsResult"
})
@XmlRootElement(name = "UpdateWebUserOptionsResponse")
public final class UpdateWebUserOptionsResponse {

    @XmlElement(name = "UpdateWebUserOptionsResult")
    private final String updateWebUserOptionsResult;

    private UpdateWebUserOptionsResponse(Builder builder) {
        this.updateWebUserOptionsResult = builder.updateWebUserOptionsResult;
    }

    /**
     * Gets the update web user options result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateWebUserOptionsResult() {
        return Optional.ofNullable(updateWebUserOptionsResult);
    }

    /**
     * Builder for UpdateWebUserOptionsResponse.
     */
    public static class Builder {
        private String updateWebUserOptionsResult;

        public Builder withUpdateWebUserOptionsResult(String updateWebUserOptionsResult) {
            this.updateWebUserOptionsResult = updateWebUserOptionsResult;
            return this;
        }

        public UpdateWebUserOptionsResponse build() {
            return new UpdateWebUserOptionsResponse(this);
        }
    }
}
