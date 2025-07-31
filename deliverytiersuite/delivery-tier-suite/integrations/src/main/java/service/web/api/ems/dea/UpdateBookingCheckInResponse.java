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
 * Java 11 Modernized: Immutable SOAP response for UpdateBookingCheckin.
 * <p>
 * Represents the response payload for the UpdateBookingCheckin endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Checkin response—because every checkin deserves a thumbs up!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateBookingCheckinResult"
})
@XmlRootElement(name = "UpdateBookingCheckinResponse")
public final class UpdateBookingCheckinResponse {

    @XmlElement(name = "UpdateBookingCheckinResult")
    private final String updateBookingCheckinResult;

    private UpdateBookingCheckinResponse(Builder builder) {
        this.updateBookingCheckinResult = builder.updateBookingCheckinResult;
    }

    /**
     * Gets the update booking checkin result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateBookingCheckinResult() {
        return Optional.ofNullable(updateBookingCheckinResult);
    }

    /**
     * Builder for UpdateBookingCheckinResponse.
     */
    public static class Builder {
        private String updateBookingCheckinResult;

        public Builder withUpdateBookingCheckinResult(String updateBookingCheckinResult) {
            this.updateBookingCheckinResult = updateBookingCheckinResult;
            return this;
        }

        public UpdateBookingCheckinResponse build() {
            return new UpdateBookingCheckinResponse(this);
        }
    }
}
