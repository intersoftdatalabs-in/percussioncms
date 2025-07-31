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

import javax.xml.bind.annotation.*;

/**
 * Java 11+ refactored version of GetAllBookingsResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAllBookingsResult"
})
@XmlRootElement(name = "GetAllBookingsResponse")
public final class GetAllBookingsResponse {

    @XmlElement(name = "GetAllBookingsResult")
    private String getAllBookingsResult;

    /** @return Optional containing the result string if present. */
    public java.util.Optional<String> getGetAllBookingsResult() {
        return java.util.Optional.ofNullable(getAllBookingsResult);
    }

    /** @param value allowed object is {@link String } */
    public void setGetAllBookingsResult(String value) {
        this.getAllBookingsResult = value;
    }

    @Override
    public String toString() {
        return "GetAllBookingsResponse{" +
                "getAllBookingsResult='" + getAllBookingsResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllBookingsResponse)) return false;
        var that = (GetAllBookingsResponse) o;
        return java.util.Objects.equals(getAllBookingsResult, that.getAllBookingsResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAllBookingsResult);
    }
}
