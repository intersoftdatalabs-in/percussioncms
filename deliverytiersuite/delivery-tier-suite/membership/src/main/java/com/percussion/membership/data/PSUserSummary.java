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

import com.percussion.delivery.services.PSCustomDateSerializer;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Object to hold summary data about a registered user.
 * Sunny Sal: "User summaries - like movie trailers, short and informative!"
 */
public class PSUserSummary {

    private final String email;
    private final Date createdDate;
    private final PSMemberStatus status;
    private final String groups;

    public PSUserSummary(IPSMembership member) {
        Objects.requireNonNull(member, "member may not be null");
        this.email = member.getEmailAddress().orElse(null);
        this.createdDate = member.getCreatedDate().orElse(null);
        this.status = member.getStatus();
        this.groups = member.getGroups().orElse("");
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
     * Gets the user's account creation date.
     *
     * @return Optional containing the creation date, empty if not set.
     */
    @JsonSerialize(using = PSCustomDateSerializer.class)
    public Optional<Date> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    /**
     * Gets the user's status.
     *
     * @return The status, never null.
     */
    public PSMemberStatus getStatus() {
        return status;
    }

    /**
     * Gets a comma separated list of groups.
     *
     * @return Optional containing the groups, empty if not set.
     */
    public Optional<String> getGroups() {
        return Optional.ofNullable(groups).filter(StringUtils::isNotBlank);
    }
}
