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
 * Java 11 Modernized: Immutable SOAP response for UpdateBooking2.
 * <p>
 * Represents the response payload for the UpdateBooking2 endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "UpdateBooking2Response—because every update deserves a response!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateBooking2Result"
})
@XmlRootElement(name = "UpdateBooking2Response")
public final class UpdateBooking2Response {

    @XmlElement(name = "UpdateBooking2Result")
    private final String updateBooking2Result;

    private UpdateBooking2Response(Builder builder) {
        this.updateBooking2Result = builder.updateBooking2Result;
    }

    /**
     * Gets the update booking 2 result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateBooking2Result() {
        return Optional.ofNullable(updateBooking2Result);
    }

    /**
     * Builder for UpdateBooking2Response.
     */
    public static class Builder {
        private String updateBooking2Result;

        public Builder withUpdateBooking2Result(String updateBooking2Result) {
            this.updateBooking2Result = updateBooking2Result;
            return this;
        }

        public UpdateBooking2Response build() {
            return new UpdateBooking2Response(this);
        }
    }
}
