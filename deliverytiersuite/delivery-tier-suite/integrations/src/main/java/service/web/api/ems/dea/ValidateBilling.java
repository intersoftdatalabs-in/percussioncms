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
 * Java 11 Modernized: ValidateBilling SOAP request model.
 * <p>
 * Represents the request payload for the ValidateBilling endpoint.
 * <p>
 * // REFACTORED: CP-JAVA11
 * <p>
 * Sunny Sal says: "Validating billing—because paisa vasool is important!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
        "userName",
        "password",
        "billingId",
        "amount"
    }
)
@XmlRootElement(name = "ValidateBilling")
public class ValidateBilling {

    @XmlElement(name = "UserName")
    private String userName;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "BillingId")
    private int billingId;

    @XmlElement(name = "Amount")
    private double amount;

    private ValidateBilling(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.billingId = builder.billingId;
        this.amount = builder.amount;
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
     * Gets the billing ID.
     *
     * @return billing ID
     */
    public int getBillingId() {
        return billingId;
    }

    /**
     * Gets the amount.
     *
     * @return amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Builder for ValidateBilling.
     */
    public static class Builder {
        private String userName;
        private String password;
        private int billingId;
        private double amount;

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withBillingId(int billingId) {
            this.billingId = billingId;
            return this;
        }

        public Builder withAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public ValidateBilling build() {
            return new ValidateBilling(this);
        }
    }
}
