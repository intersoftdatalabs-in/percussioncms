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

import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11 Modernized: UpdateReservation SOAP request model.
 * <p>
 * Represents the request payload for the UpdateReservation endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Updating reservations—because plans change, but code should stay clean!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "reservationId",
        "newDate",
        "newStatus"
    }
)
@XmlRootElement(name = "UpdateReservation")
public class UpdateReservation {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "ReservationId")
    private int reservationId;

    @XmlElement(name = "NewDate")
    private String newDate;

    @XmlElement(name = "NewStatus")
    private String newStatus;

    private UpdateReservation(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.reservationId = builder.reservationId;
        this.newDate = builder.newDate;
        this.newStatus = builder.newStatus;
    }

    /**
     * Gets the user name.
     *
     * @return Optional user name
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Gets the password.
     *
     * @return Optional password
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Gets the reservation ID.
     *
     * @return reservation ID
     */
    public int getReservationId() {
        return reservationId;
    }

    /**
     * Gets the new date.
     *
     * @return Optional new date
     */
    public Optional<String> getNewDate() {
        return Optional.ofNullable(newDate);
    }

    /**
     * Gets the new status.
     *
     * @return Optional new status
     */
    public Optional<String> getNewStatus() {
        return Optional.ofNullable(newStatus);
    }

    /**
     * Builder for UpdateReservation.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int reservationId;
        private String newDate;
        private String newStatus;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withReservationId(int reservationId) {
            this.reservationId = reservationId;
            return this;
        }

        public Builder withNewDate(String newDate) {
            this.newDate = newDate;
            return this;
        }

        public Builder withNewStatus(String newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public UpdateReservation build() {
            return new UpdateReservation(this);
        }
    }
}
