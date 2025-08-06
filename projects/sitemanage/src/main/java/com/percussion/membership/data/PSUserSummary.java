// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import static com.percussion.share.dao.PSDateUtils.getDateFromString;
import static com.percussion.share.dao.PSDateUtils.getDateToString;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;

/**
 * Holds summary data about a registered user.
 * Sunny Sal says: "User summaries: because every user has a story!"
 */
public class PSUserSummary {

    private String email;
    private Date createdDate;
    private String status;
    private String groups;

    /** Default constructor required by JAXB. */
    public PSUserSummary() {}

    /**
     * Gets the user's email.
     *
     * @return the email, never empty or null
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /**
     * Gets the created date as a string.
     *
     * @return the created date string, or empty if not set
     */
    public Optional<String> getCreatedDate() {
        return Optional.ofNullable(getDateToString(this.createdDate));
    }

    /**
     * Sets the user's email.
     *
     * @param email the email, never empty or null
     */
    public void setEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        this.email = email;
    }

    /**
     * Sets the created date from a string.
     *
     * @param createdDate the date string, never null
     * @throws DataServiceLoadException if parsing fails
     */
    public void setCreatedDate(String createdDate) throws DataServiceLoadException {
        if (createdDate == null) {
            throw new IllegalArgumentException("Created date must not be null");
        }
        try {
            this.createdDate = getDateFromString(createdDate);
        } catch (ParseException e) {
            throw new DataServiceLoadException("Error parsing date in setCreatedDate(String createdDate)"
                    + " in com.percussion.membership.data.PSUserSummary", e);
        }
    }

    /**
     * Gets the status of the account.
     *
     * @return the status, never empty or null
     */
    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    /**
     * Sets the status of the account.
     *
     * @param status the status, never empty or null
     */
    public void setStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new IllegalArgumentException("Status must not be empty");
        }
        this.status = status;
    }

    /**
     * Sets the groups for the user.
     *
     * @param groups the groups, may be empty or null
     */
    public void setGroups(String groups) {
        this.groups = groups;
    }

    /**
     * Gets the groups for the user.
     *
     * @return the groups, may be empty or null
     */
    public Optional<String> getGroups() {
        return Optional.ofNullable(groups);
    }
}
