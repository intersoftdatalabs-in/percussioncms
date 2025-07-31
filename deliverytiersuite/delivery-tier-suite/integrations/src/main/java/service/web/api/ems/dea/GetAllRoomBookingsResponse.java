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
 * Java 11+ refactored version of GetAllRoomBookingsResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAllRoomBookingsResult"
})
@XmlRootElement(name = "GetAllRoomBookingsResponse")
public final class GetAllRoomBookingsResponse {
    @XmlElement(name = "GetAllRoomBookingsResult")
    private final String getAllRoomBookingsResult;

    private GetAllRoomBookingsResponse(Builder builder) {
        this.getAllRoomBookingsResult = builder.getAllRoomBookingsResult;
    }

    /**
     * @return Optional result string for all room bookings.
     */
    public java.util.Optional<String> getAllRoomBookingsResult() {
        return java.util.Optional.ofNullable(getAllRoomBookingsResult);
    }

    @Override
    public String toString() {
        return "GetAllRoomBookingsResponse{" +
                "getAllRoomBookingsResult='" + getAllRoomBookingsResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAllRoomBookingsResponse that = (GetAllRoomBookingsResponse) o;
        return java.util.Objects.equals(getAllRoomBookingsResult, that.getAllRoomBookingsResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAllRoomBookingsResult);
    }

    /**
     * Builder for GetAllRoomBookingsResponse. Use for safe, immutable construction.
     */
    public static class Builder {
        private String getAllRoomBookingsResult;

        public Builder() {}

        public Builder getAllRoomBookingsResult(String getAllRoomBookingsResult) {
            this.getAllRoomBookingsResult = getAllRoomBookingsResult;
            return this;
        }

        public GetAllRoomBookingsResponse build() {
            return new GetAllRoomBookingsResponse(this);
        }
    }
}
