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
 * Java 11+ refactored version of GetAllBookings2Response SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getAllBookings2Result"
})
@XmlRootElement(name = "GetAllBookings2Response")
public final class GetAllBookings2Response {

    @XmlElement(name = "GetAllBookings2Result")
    private String getAllBookings2Result;

    /** @return Optional containing the result string if present. */
    public java.util.Optional<String> getGetAllBookings2Result() {
        return java.util.Optional.ofNullable(getAllBookings2Result);
    }

    /** @param value allowed object is {@link String } */
    public void setGetAllBookings2Result(String value) {
        this.getAllBookings2Result = value;
    }

    @Override
    public String toString() {
        return "GetAllBookings2Response{" +
                "getAllBookings2Result='" + getAllBookings2Result + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllBookings2Response)) return false;
        var that = (GetAllBookings2Response) o;
        return java.util.Objects.equals(getAllBookings2Result, that.getAllBookings2Result);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getAllBookings2Result);
    }
}
