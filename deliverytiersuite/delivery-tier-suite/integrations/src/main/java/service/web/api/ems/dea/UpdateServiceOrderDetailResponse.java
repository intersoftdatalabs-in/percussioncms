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
 * Java 11 Modernized: Immutable SOAP response for UpdateServiceOrderDetail.
 * <p>
 * Represents the response payload for the UpdateServiceOrderDetail endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Service order detail updated—now your code and your chai are both extra detailed!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateServiceOrderDetailResult"
})
@XmlRootElement(name = "UpdateServiceOrderDetailResponse")
public final class UpdateServiceOrderDetailResponse {

    @XmlElement(name = "UpdateServiceOrderDetailResult")
    private final String updateServiceOrderDetailResult;

    private UpdateServiceOrderDetailResponse(Builder builder) {
        this.updateServiceOrderDetailResult = builder.updateServiceOrderDetailResult;
    }

    /**
     * Gets the update service order detail result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateServiceOrderDetailResult() {
        return Optional.ofNullable(updateServiceOrderDetailResult);
    }

    /**
     * Builder for UpdateServiceOrderDetailResponse.
     */
    public static class Builder {
        private String updateServiceOrderDetailResult;

        public Builder withUpdateServiceOrderDetailResult(String updateServiceOrderDetailResult) {
            this.updateServiceOrderDetailResult = updateServiceOrderDetailResult;
            return this;
        }

        public UpdateServiceOrderDetailResponse build() {
            return new UpdateServiceOrderDetailResponse(this);
        }
    }
}
