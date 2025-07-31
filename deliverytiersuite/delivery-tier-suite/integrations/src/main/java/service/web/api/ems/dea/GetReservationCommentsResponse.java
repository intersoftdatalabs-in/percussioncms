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
import java.util.Objects;
import java.util.Optional;

/**
 * Response for GetReservationComments request.
 * Sunny Sal: "Reservation comments delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getReservationCommentsResult"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetReservationCommentsResponse")
public class GetReservationCommentsResponse {

    @XmlElement(name = "GetReservationCommentsResult")
    protected String getReservationCommentsResult;

    /**
     * Gets the reservation comments result.
     * @return Optional containing result string, or empty if not present.
     */
    public Optional<String> getGetReservationCommentsResult() {
        return Optional.ofNullable(getReservationCommentsResult);
    }

    /**
     * Sets the reservation comments result.
     * @param value result string
     */
    public void setGetReservationCommentsResult(String value) {
        getReservationCommentsResult = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetReservationCommentsResponse)) return false;
        var that = (GetReservationCommentsResponse) o;
        return Objects.equals(getReservationCommentsResult, that.getReservationCommentsResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReservationCommentsResult);
    }

    @Override
    public String toString() {
        return "GetReservationCommentsResponse{" +
                "getReservationCommentsResult='" + getReservationCommentsResult + '\'' +
                '}';
    }
}
