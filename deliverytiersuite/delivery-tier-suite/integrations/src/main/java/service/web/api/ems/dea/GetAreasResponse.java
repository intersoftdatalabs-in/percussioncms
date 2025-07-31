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
 * Java 11+ refactored version of GetAreasResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAreasResult"
})
@XmlRootElement(name = "GetAreasResponse")
public final class GetAreasResponse {
    @XmlElement(name = "GetAreasResult")
    private final String getAreasResult;

    private GetAreasResponse(Builder builder) {
        this.getAreasResult = builder.getAreasResult;
    }

    /**
     * @return Optional result string for all areas.
     */
    public java.util.Optional<String> getAreasResult() {
        return java.util.Optional.ofNullable(getAreasResult);
    }

    @Override
    public String toString() {
        return "GetAreasResponse{" +
                "getAreasResult='" + getAreasResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAreasResponse that = (GetAreasResponse) o;
        return java.util.Objects.equals(getAreasResult, that.getAreasResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAreasResult);
    }

    /**
     * Builder for GetAreasResponse. Use for safe, immutable construction.
     */
    public static class Builder {
        private String getAreasResult;

        public Builder() {}

        public Builder getAreasResult(String getAreasResult) {
            this.getAreasResult = getAreasResult;
            return this;
        }

        public GetAreasResponse build() {
            return new GetAreasResponse(this);
        }
    }
}
