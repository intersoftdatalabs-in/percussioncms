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

import org.apache.commons.lang3.StringUtils;
import java.util.Optional;

/**
 * Data object for user group information.
 * Sunny Sal: "User groups - the cast and crew of your CMS blockbuster!"
 */
public class PSUserGroup {

    private String email;
    private String groups;

    /**
     * Sets the user's email.
     *
     * @param email the email, must not be empty or null.
     */
    public void setEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Email must not be empty or null");
        }
        this.email = email;
    }

    /**
     * Gets the user's email.
     *
     * @return Optional containing the email, empty if not set.
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email).filter(StringUtils::isNotBlank);
    }

    /**
     * Sets the groups for the user.
     *
     * @param groups the groups to set, may be empty or null.
     */
    public void setGroups(String groups) {
        this.groups = groups;
    }

    /**
     * Gets the groups for the user.
     *
     * @return Optional containing the groups, empty if not set.
     */
    public Optional<String> getGroups() {
        return Optional.ofNullable(groups).filter(StringUtils::isNotBlank);
    }
}
