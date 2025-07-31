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
 * Java 11+ refactored version of DeleteServiceOrderDetail SOAP request.
 * <p>
 * Immutable, thread-safe, OWASP-compliant. Use builder for instantiation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "serviceOrderDetailID"
})
@XmlRootElement(name = "DeleteServiceOrderDetail")
public final class DeleteServiceOrderDetail {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "ServiceOrderDetailID")
    private final int serviceOrderDetailID;

    private DeleteServiceOrderDetail(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.serviceOrderDetailID = builder.serviceOrderDetailID;
    }

    /** @return Optional user name for authentication. */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /** @return Optional password for authentication. */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /** @return Service order detail ID to delete. */
    public int getServiceOrderDetailID() {
        return serviceOrderDetailID;
    }

    @Override
    public String toString() {
        return "DeleteServiceOrderDetail{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", serviceOrderDetailID=" + serviceOrderDetailID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteServiceOrderDetail)) return false;
        var that = (DeleteServiceOrderDetail) o;
        return serviceOrderDetailID == that.serviceOrderDetailID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, serviceOrderDetailID);
    }

    /**
     * Builder for DeleteServiceOrderDetail. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int serviceOrderDetailID;

        public Builder() {}

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder serviceOrderDetailID(int serviceOrderDetailID) {
            this.serviceOrderDetailID = serviceOrderDetailID;
            return this;
        }
        public DeleteServiceOrderDetail build() {
            return new DeleteServiceOrderDetail(this);
        }
    }
}
