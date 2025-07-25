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
 * Java 11+ refactored SOAP response for GetBuildingHolidays.
 * Immutable, builder-based, Google Java Style. JAXB annotations retained for SOAP compatibility.
 * Sunny Sal: "Building holidays response, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getBuildingHolidaysResult"
})
@XmlRootElement(name = "GetBuildingHolidaysResponse")
public final class GetBuildingHolidaysResponse {

    @XmlElement(name = "GetBuildingHolidaysResult")
    private final String getBuildingHolidaysResult;

    private GetBuildingHolidaysResponse(Builder builder) {
        this.getBuildingHolidaysResult = builder.getBuildingHolidaysResult;
    }

    /**
     * Gets the building holidays result.
     * @return Optional result
     */
    public Optional<String> getGetBuildingHolidaysResult() {
        return Optional.ofNullable(getBuildingHolidaysResult);
    }

    /**
     * Builder for GetBuildingHolidaysResponse (Java 11+ style).
     */
    public static class Builder {
        private String getBuildingHolidaysResult;

        public Builder getBuildingHolidaysResult(String getBuildingHolidaysResult) {
            this.getBuildingHolidaysResult = getBuildingHolidaysResult;
            return this;
        }

        public GetBuildingHolidaysResponse build() {
            return new GetBuildingHolidaysResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBuildingHolidaysResponse)) return false;
        var that = (GetBuildingHolidaysResponse) o;
        return Objects.equals(getBuildingHolidaysResult, that.getBuildingHolidaysResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBuildingHolidaysResult);
    }

    @Override
    public String toString() {
        return "GetBuildingHolidaysResponse{" +
                "getBuildingHolidaysResult='" + getBuildingHolidaysResult + '\'' +
                '}';
    }
}
