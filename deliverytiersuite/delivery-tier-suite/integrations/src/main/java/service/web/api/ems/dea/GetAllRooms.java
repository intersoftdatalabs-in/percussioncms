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
 * Java 11+ refactored version of GetAllRooms SOAP request.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "buildingID"
})
@XmlRootElement(name = "GetAllRooms")
public final class GetAllRooms {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "BuildingID")
    private final int buildingID;

    private GetAllRooms(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.buildingID = builder.buildingID;
    }

    /** @return Optional user name for authentication. */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /** @return Optional password for authentication. */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /** @return Building ID for which rooms are requested. */
    public int getBuildingID() {
        return buildingID;
    }

    @Override
    public String toString() {
        return "GetAllRooms{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", buildingID=" + buildingID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetAllRooms)) return false;
        var that = (GetAllRooms) o;
        return buildingID == that.buildingID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, buildingID);
    }

    /**
     * Builder for GetAllRooms. Use for safe, immutable construction.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int buildingID;

        public Builder() {}

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder buildingID(int buildingID) {
            this.buildingID = buildingID;
            return this;
        }
        public GetAllRooms build() {
            return new GetAllRooms(this);
        }
    }
}
