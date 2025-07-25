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
 * Java 11 Modernized: Immutable SOAP response for ValidateBilling.
 * <p>
 * Represents the response payload for the ValidateBilling endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Billing validated—now your paisa and your code are both sorted!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "validateBillingResult"
})
@XmlRootElement(name = "ValidateBillingResponse")
public final class ValidateBillingResponse {

    @XmlElement(name = "ValidateBillingResult")
    private final String validateBillingResult;

    private ValidateBillingResponse(Builder builder) {
        this.validateBillingResult = builder.validateBillingResult;
    }

    /**
     * Gets the validate billing result.
     *
     * @return Optional result string
     */
    public Optional<String> getValidateBillingResult() {
        return Optional.ofNullable(validateBillingResult);
    }

    /**
     * Builder for ValidateBillingResponse.
     */
    public static class Builder {
        private String validateBillingResult;

        public Builder withValidateBillingResult(String validateBillingResult) {
            this.validateBillingResult = validateBillingResult;
            return this;
        }

        public ValidateBillingResponse build() {
            return new ValidateBillingResponse(this);
        }
    }
}
