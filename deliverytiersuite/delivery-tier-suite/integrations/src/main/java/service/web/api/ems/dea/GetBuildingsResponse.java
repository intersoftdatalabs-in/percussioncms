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

// REFACTORED: CP-JAVA11
// REFACTORED: CP-SOAP
package service.web.api.ems.dea;

import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.*;

/**
 * Java 11+ refactored SOAP response for GetBuildings.
 * Immutable, builder-based, Google Java Style. JAXB annotations retained for SOAP compatibility.
 * Sunny Sal: "Buildings response, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getBuildingsResult"
})
@XmlRootElement(name = "GetBuildingsResponse")
public final class GetBuildingsResponse {

    @XmlElement(name = "GetBuildingsResult")
    private final String getBuildingsResult;

    private GetBuildingsResponse(Builder builder) {
        this.getBuildingsResult = builder.getBuildingsResult;
    }

    /**
     * Gets the buildings result.
     * @return Optional result
     */
    public Optional<String> getGetBuildingsResult() {
        return Optional.ofNullable(getBuildingsResult);
    }

    /**
     * Builder for GetBuildingsResponse (Java 11+ style).
     */
    public static class Builder {
        private String getBuildingsResult;

        public Builder getBuildingsResult(String getBuildingsResult) {
            this.getBuildingsResult = getBuildingsResult;
            return this;
        }

        public GetBuildingsResponse build() {
            return new GetBuildingsResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBuildingsResponse)) return false;
        var that = (GetBuildingsResponse) o;
        return Objects.equals(getBuildingsResult, that.getBuildingsResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBuildingsResult);
    }

    @Override
    public String toString() {
        return "GetBuildingsResponse{" +
                "getBuildingsResult='" + getBuildingsResult + '\'' +
                '}';
    }
}
