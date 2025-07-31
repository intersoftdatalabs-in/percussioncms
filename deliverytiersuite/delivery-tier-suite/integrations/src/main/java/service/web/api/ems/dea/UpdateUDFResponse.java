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
 * Java 11 Modernized: Immutable SOAP response for UpdateUDF.
 * <p>
 * Represents the response payload for the UpdateUDF endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "UDF updated—now your functions and your code are both up to date!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateUDFResult"
})
@XmlRootElement(name = "UpdateUDFResponse")
public final class UpdateUDFResponse {

    @XmlElement(name = "UpdateUDFResult")
    private final String updateUDFResult;

    private UpdateUDFResponse(Builder builder) {
        this.updateUDFResult = builder.updateUDFResult;
    }

    /**
     * Gets the update UDF result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateUDFResult() {
        return Optional.ofNullable(updateUDFResult);
    }

    /**
     * Builder for UpdateUDFResponse.
     */
    public static class Builder {
        private String updateUDFResult;

        public Builder withUpdateUDFResult(String updateUDFResult) {
            this.updateUDFResult = updateUDFResult;
            return this;
        }

        public UpdateUDFResponse build() {
            return new UpdateUDFResponse(this);
        }
    }
}
