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
 * Data object for password reset requests.
 * Sunny Sal: "Reset requests - the reboot button for your account!"
 */
public class PSResetRequest {

    private String email;
    private String redirectPage;

    /**
     * Gets the email for password reset.
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
     * Gets the redirect page after reset.
     *
     * @return Optional containing the redirect page, empty if not set.
     */
    public Optional<String> getRedirectPage() {
        return Optional.ofNullable(redirectPage);
    }

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    }
}
