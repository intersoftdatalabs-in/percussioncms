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
 * Java 11 Modernized: Immutable SOAP response for UpdateBooking.
 * <p>
 * Represents the response payload for the UpdateBooking endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "If only updating your code was as easy as updating a booking!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateBookingResult"
})
@XmlRootElement(name = "UpdateBookingResponse")
public final class UpdateBookingResponse {

    @XmlElement(name = "UpdateBookingResult")
    private final String updateBookingResult;

    private UpdateBookingResponse(Builder builder) {
        this.updateBookingResult = builder.updateBookingResult;
    }

    /**
     * Gets the update booking result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateBookingResult() {
        return Optional.ofNullable(updateBookingResult);
    }

    /**
     * Builder for UpdateBookingResponse.
     */
    public static class Builder {
        private String updateBookingResult;

        public Builder withUpdateBookingResult(String updateBookingResult) {
            this.updateBookingResult = updateBookingResult;
            return this;
        }

        public UpdateBookingResponse build() {
            return new UpdateBookingResponse(this);
        }
    }
}
