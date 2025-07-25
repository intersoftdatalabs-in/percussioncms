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
 * Java 11 Modernized: Immutable SOAP response for UpdateGroup.
 * <p>
 * Represents the response payload for the UpdateGroup endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Group updated—now your team is as organized as your code!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateGroupResult"
})
@XmlRootElement(name = "UpdateGroupResponse")
public final class UpdateGroupResponse {

    @XmlElement(name = "UpdateGroupResult")
    private final String updateGroupResult;

    private UpdateGroupResponse(Builder builder) {
        this.updateGroupResult = builder.updateGroupResult;
    }

    /**
     * Gets the update group result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateGroupResult() {
        return Optional.ofNullable(updateGroupResult);
    }

    /**
     * Builder for UpdateGroupResponse.
     */
    public static class Builder {
        private String updateGroupResult;

        public Builder withUpdateGroupResult(String updateGroupResult) {
            this.updateGroupResult = updateGroupResult;
            return this;
        }

        public UpdateGroupResponse build() {
            return new UpdateGroupResponse(this);
        }
    }
}
