// REFACTORED: CP-JAVA11

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
package com.percussion.membership.data;

import java.util.Optional;

/**
 * Data object for membership account creation.
 * Sunny Sal: "Membership accounts - the passport to Percussion CMS!"
 */
public class PSMembershipAccount {

    private String email;
    private String password;
    private Boolean confirmationRequired;
    private String confirmationPage;

    /**
     * Gets the email for the account to create.
     *
     * @return Optional containing the email, empty if not set.
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the password for the account to create.
     *
     * @return Optional containing the password, empty if not set.
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Indicates if confirmation is required.
     *
     * @return true if confirmation is required, false otherwise.
     */
    public boolean isConfirmationRequired() {
        return confirmationRequired != null && confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    /**
     * Gets the confirmation page to redirect the user.
     *
     * @return Optional containing the confirmation page, empty if not set.
     */
    public Optional<String> getConfirmationPage() {
        return Optional.ofNullable(confirmationPage);
    }

    public void setConfirmationPage(String confirmationPage) {
        this.confirmationPage = confirmationPage;
    }
}
