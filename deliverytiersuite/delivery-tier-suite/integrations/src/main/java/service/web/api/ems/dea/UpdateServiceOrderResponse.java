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
 * Java 11 Modernized: Immutable SOAP response for UpdateServiceOrder.
 * <p>
 * Represents the response payload for the UpdateServiceOrder endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Service order updated—now your chai and your code are both piping hot!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateServiceOrderResult"
})
@XmlRootElement(name = "UpdateServiceOrderResponse")
public final class UpdateServiceOrderResponse {

    @XmlElement(name = "UpdateServiceOrderResult")
    private final String updateServiceOrderResult;

    private UpdateServiceOrderResponse(Builder builder) {
        this.updateServiceOrderResult = builder.updateServiceOrderResult;
    }

    /**
     * Gets the update service order result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateServiceOrderResult() {
        return Optional.ofNullable(updateServiceOrderResult);
    }

    /**
     * Builder for UpdateServiceOrderResponse.
     */
    public static class Builder {
        private String updateServiceOrderResult;

        public Builder withUpdateServiceOrderResult(String updateServiceOrderResult) {
            this.updateServiceOrderResult = updateServiceOrderResult;
            return this;
        }

        public UpdateServiceOrderResponse build() {
            return new UpdateServiceOrderResponse(this);
        }
    }
}
