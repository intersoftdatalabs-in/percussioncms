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
 * Java 11 Modernized: UpdateBooking SOAP request model.
 * <p>
 * Represents the request payload for the UpdateBooking endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Updating bookings is easier than updating your relationship status!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "bookingId",
        "newStatus"
    }
)
@XmlRootElement(name = "UpdateBooking")
public class UpdateBooking {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "BookingId")
    private int bookingId;

    @XmlElement(name = "NewStatus")
    private int newStatus;

    private UpdateBooking(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.bookingId = builder.bookingId;
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
     * Gets the booking ID.
     *
     * @return booking ID
     */
    public int getBookingId() {
        return bookingId;
    }

    /**
     * Gets the new status.
     *
     * @return new status
     */
    public int getNewStatus() {
        return newStatus;
    }

    /**
     * Builder for UpdateBooking.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int bookingId;
        private int newStatus;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withBookingId(int bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder withNewStatus(int newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public UpdateBooking build() {
            return new UpdateBooking(this);
        }
    }
}
