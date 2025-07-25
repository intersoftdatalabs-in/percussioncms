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
 * Java 11+ refactored version of GetAllComboRoomComponentsResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAllComboRoomComponentsResult"
})
@XmlRootElement(name = "GetAllComboRoomComponentsResponse")
public final class GetAllComboRoomComponentsResponse {
    @XmlElement(name = "GetAllComboRoomComponentsResult")
    private final String getAllComboRoomComponentsResult;

    private GetAllComboRoomComponentsResponse(Builder builder) {
        this.getAllComboRoomComponentsResult = builder.getAllComboRoomComponentsResult;
    }

    /**
     * @return Optional result string for all combo room components.
     */
    public java.util.Optional<String> getAllComboRoomComponentsResult() {
        return java.util.Optional.ofNullable(getAllComboRoomComponentsResult);
    }

    @Override
    public String toString() {
        return "GetAllComboRoomComponentsResponse{" +
                "getAllComboRoomComponentsResult='" + getAllComboRoomComponentsResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAllComboRoomComponentsResponse that = (GetAllComboRoomComponentsResponse) o;
        return java.util.Objects.equals(getAllComboRoomComponentsResult, that.getAllComboRoomComponentsResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAllComboRoomComponentsResult);
    }

    /**
     * Builder for GetAllComboRoomComponentsResponse. Use for safe, immutable construction.
     */
    public static class Builder {
        private String getAllComboRoomComponentsResult;

        public Builder() {}

        public Builder getAllComboRoomComponentsResult(String getAllComboRoomComponentsResult) {
            this.getAllComboRoomComponentsResult = getAllComboRoomComponentsResult;
            return this;
        }

        public GetAllComboRoomComponentsResponse build() {
            return new GetAllComboRoomComponentsResponse(this);
        }
    }
}
