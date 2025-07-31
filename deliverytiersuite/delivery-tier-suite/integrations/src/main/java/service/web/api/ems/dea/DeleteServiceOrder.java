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
 * Java 11+ refactored version of DeleteServiceOrder SOAP request.
 * <p>
 * Immutable, thread-safe, OWASP-compliant. Use builder for instantiation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "serviceOrderID"
})
@XmlRootElement(name = "DeleteServiceOrder")
public final class DeleteServiceOrder {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "ServiceOrderID")
    private final int serviceOrderID;

    private DeleteServiceOrder(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.serviceOrderID = builder.serviceOrderID;
    }

    /** @return Optional user name for authentication. */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /** @return Optional password for authentication. */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /** @return Service order ID to delete. */
    public int getServiceOrderID() {
        return serviceOrderID;
    }

    @Override
    public String toString() {
        return "DeleteServiceOrder{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", serviceOrderID=" + serviceOrderID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteServiceOrder)) return false;
        var that = (DeleteServiceOrder) o;
        return serviceOrderID == that.serviceOrderID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, serviceOrderID);
    }

    /**
     * Builder for DeleteServiceOrder. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int serviceOrderID;

        public Builder() {}

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder serviceOrderID(int serviceOrderID) {
            this.serviceOrderID = serviceOrderID;
            return this;
        }
        public DeleteServiceOrder build() {
            return new DeleteServiceOrder(this);
        }
    }
}
