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
 * Java 11+ refactored version of GetAllRoomsResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAllRoomsResult"
})
@XmlRootElement(name = "GetAllRoomsResponse")
public final class GetAllRoomsResponse {

    @XmlElement(name = "GetAllRoomsResult")
    private String getAllRoomsResult;

    /** @return Optional containing the result string if present. */
    public java.util.Optional<String> getGetAllRoomsResult() {
        return java.util.Optional.ofNullable(getAllRoomsResult);
    }

    /** @param value allowed object is {@link String } */
    public void setGetAllRoomsResult(String value) {
        this.getAllRoomsResult = value;
    }

    @Override
    public String toString() {
        return "GetAllRoomsResponse{" +
                "getAllRoomsResult='" + getAllRoomsResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllRoomsResponse)) return false;
        var that = (GetAllRoomsResponse) o;
        return java.util.Objects.equals(getAllRoomsResult, that.getAllRoomsResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAllRoomsResult);
    }
}
