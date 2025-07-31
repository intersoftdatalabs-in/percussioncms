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
 * Java 11 Modernized: Immutable SOAP response for GetChangedBookings.
 * <p>
 * Represents the response payload for the GetChangedBookings endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Immutability is like a good cup of chai—keeps things fresh and safe!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getChangedBookingsResult"
})
@XmlRootElement(name = "GetChangedBookingsResponse")
public final class GetChangedBookingsResponse {

    @XmlElement(name = "GetChangedBookingsResult")
    private final String getChangedBookingsResult;

    private GetChangedBookingsResponse(Builder builder) {
        this.getChangedBookingsResult = builder.getChangedBookingsResult;
    }

    /**
     * Gets the value of the getChangedBookingsResult property.
     *
     * @return the changed bookings result, or null if not set
     */
    public String getGetChangedBookingsResult() {
        return getChangedBookingsResult;
    }

    /**
     * Builder for GetChangedBookingsResponse.
     * <p>
     * Example usage:
     * <pre>
     * var response = new GetChangedBookingsResponse.Builder()
     *     .withChangedBookingsResult("result")
     *     .build();
     * </pre>
     */
    public static class Builder {
        private String getChangedBookingsResult;

        /**
         * Sets the changed bookings result value.
         *
         * @param getChangedBookingsResult the result string
         * @return this builder instance
         */
        public Builder withChangedBookingsResult(String getChangedBookingsResult) {
            this.getChangedBookingsResult = getChangedBookingsResult;
            return this;
        }

        /**
         * Builds a new immutable GetChangedBookingsResponse instance.
         *
         * @return a new GetChangedBookingsResponse
         */
        public GetChangedBookingsResponse build() {
            return new GetChangedBookingsResponse(this);
        }
    }
}
