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
 * Java 11+ refactored version of GetAPIVersionResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAPIVersionResult"
})
@XmlRootElement(name = "GetAPIVersionResponse")
public final class GetAPIVersionResponse {
    @XmlElement(name = "GetAPIVersionResult")
    private final String getAPIVersionResult;

    private GetAPIVersionResponse(Builder builder) {
        this.getAPIVersionResult = builder.getAPIVersionResult;
    }

    /**
     * @return Optional API version result string.
     */
    public java.util.Optional<String> getAPIVersionResult() {
        return java.util.Optional.ofNullable(getAPIVersionResult);
    }

    @Override
    public String toString() {
        return "GetAPIVersionResponse{" +
                "getAPIVersionResult='" + getAPIVersionResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAPIVersionResponse that = (GetAPIVersionResponse) o;
        return java.util.Objects.equals(getAPIVersionResult, that.getAPIVersionResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAPIVersionResult);
    }

    /**
     * Builder for GetAPIVersionResponse. Use for safe, immutable construction.
     */
    public static class Builder {
        private String getAPIVersionResult;

        public Builder() {}

        public Builder getAPIVersionResult(String getAPIVersionResult) {
            this.getAPIVersionResult = getAPIVersionResult;
            return this;
        }

        public GetAPIVersionResponse build() {
            return new GetAPIVersionResponse(this);
        }
    }
}
