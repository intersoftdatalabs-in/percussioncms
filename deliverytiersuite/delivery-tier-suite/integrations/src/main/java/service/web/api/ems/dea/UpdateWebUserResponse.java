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
 * Java 11 Modernized: Immutable SOAP response for UpdateWebUser.
 * <p>
 * Represents the response payload for the UpdateWebUser endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Web user updated—now your login and your code are both top-notch!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateWebUserResult"
})
@XmlRootElement(name = "UpdateWebUserResponse")
public final class UpdateWebUserResponse {

    @XmlElement(name = "UpdateWebUserResult")
    private final String updateWebUserResult;

    private UpdateWebUserResponse(Builder builder) {
        this.updateWebUserResult = builder.updateWebUserResult;
    }

    /**
     * Gets the update web user result.
     *
     * @return Optional result string
     */
    public Optional<String> getUpdateWebUserResult() {
        return Optional.ofNullable(updateWebUserResult);
    }

    /**
     * Builder for UpdateWebUserResponse.
     */
    public static class Builder {
        private String updateWebUserResult;

        public Builder withUpdateWebUserResult(String updateWebUserResult) {
            this.updateWebUserResult = updateWebUserResult;
            return this;
        }

        public UpdateWebUserResponse build() {
            return new UpdateWebUserResponse(this);
        }
    }
}
