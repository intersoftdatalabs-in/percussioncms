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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11+ refactored SOAP response for GetBuildingHours.
 * <p>Immutable, builder-based, and Google Java Style. JAXB annotations retained for SOAP compatibility.</p>
 *
 * <p>Schema fragment:</p>
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="GetBuildingHoursResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>&lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getBuildingHoursResult"
})
@XmlRootElement(name = "GetBuildingHoursResponse")
public final class GetBuildingHoursResponse {

    @XmlElement(name = "GetBuildingHoursResult")
    private final String getBuildingHoursResult;

    private GetBuildingHoursResponse(Builder builder) {
        this.getBuildingHoursResult = builder.getBuildingHoursResult;
    }

    /**
     * @return Optional getBuildingHoursResult
     */
    public Optional<String> getGetBuildingHoursResult() {
        return Optional.ofNullable(getBuildingHoursResult);
    }

    /**
     * Builder for GetBuildingHoursResponse (Java 11+ style).
     */
    public static class Builder {
        private String getBuildingHoursResult;

        public Builder getBuildingHoursResult(String getBuildingHoursResult) {
            this.getBuildingHoursResult = getBuildingHoursResult;
            return this;
        }

        public GetBuildingHoursResponse build() {
            return new GetBuildingHoursResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildingHoursResponse that = (GetBuildingHoursResponse) o;
        return Objects.equals(getBuildingHoursResult, that.getBuildingHoursResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBuildingHoursResult);
    }

    @Override
    public String toString() {
        return "GetBuildingHoursResponse{" +
                "getBuildingHoursResult='" + getBuildingHoursResult + '\'' +
                '}';
    }
}
