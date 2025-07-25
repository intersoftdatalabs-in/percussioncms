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
 * Java 11 Modernized: UpdateServiceOrderDetail SOAP request model.
 * <p>
 * Represents the request payload for the UpdateServiceOrderDetail endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Details matter—especially in service orders and in code!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "serviceOrderDetailId",
        "newStatus",
        "notes"
    }
)
@XmlRootElement(name = "UpdateServiceOrderDetail")
public class UpdateServiceOrderDetail {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "ServiceOrderDetailId")
    private int serviceOrderDetailId;

    @XmlElement(name = "NewStatus")
    private String newStatus;

    @XmlElement(name = "Notes")
    private String notes;

    private UpdateServiceOrderDetail(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.serviceOrderDetailId = builder.serviceOrderDetailId;
        this.newStatus = builder.newStatus;
        this.notes = builder.notes;
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
     * Gets the service order detail ID.
     *
     * @return service order detail ID
     */
    public int getServiceOrderDetailId() {
        return serviceOrderDetailId;
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
     * Gets the notes.
     *
     * @return Optional notes
     */
    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    /**
     * Builder for UpdateServiceOrderDetail.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int serviceOrderDetailId;
        private String newStatus;
        private String notes;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withServiceOrderDetailId(int serviceOrderDetailId) {
            this.serviceOrderDetailId = serviceOrderDetailId;
            return this;
        }

        public Builder withNewStatus(String newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public Builder withNotes(String notes) {
            this.notes = notes;
            return this;
        }

        public UpdateServiceOrderDetail build() {
            return new UpdateServiceOrderDetail(this);
        }
    }
}
