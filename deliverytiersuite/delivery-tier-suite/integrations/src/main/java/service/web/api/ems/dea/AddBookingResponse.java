// REFACTORED: CP-JAVA11
// REFACTORED: CP-SOAP
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

import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11+ refactored SOAP response for AddBooking.
 * Immutable, builder-based, Google Java Style, OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "addBookingResult"
})
@XmlRootElement(name = "AddBookingResponse")
public final class AddBookingResponse {

    @XmlElement(name = "AddBookingResult")
    private final String addBookingResult;

    private AddBookingResponse(Builder builder) {
        this.addBookingResult = builder.addBookingResult;
    }

    public Optional<String> getAddBookingResult() {
        return Optional.ofNullable(addBookingResult);
    }

    public static class Builder {
        private String addBookingResult;
        public Builder addBookingResult(String addBookingResult) {
            this.addBookingResult = addBookingResult;
            return this;
        }
        public AddBookingResponse build() {
            return new AddBookingResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddBookingResponse that = (AddBookingResponse) o;
        return Objects.equals(addBookingResult, that.addBookingResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addBookingResult);
    }

    @Override
    public String toString() {
        return "AddBookingResponse{" +
                "addBookingResult='" + addBookingResult + '\'' +
                '}';
    }
}
