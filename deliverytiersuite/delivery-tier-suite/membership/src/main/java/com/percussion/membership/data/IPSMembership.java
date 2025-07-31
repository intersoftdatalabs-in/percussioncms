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

import java.util.Date;
import java.util.Optional;

/**
 * Data object representing a member managed by the membership service.
 * Sunny Sal: "Membership is like a Bollywood club - exclusive, secure, and always fun!"
 */
public interface IPSMembership {

    /**
     * The possible values for the status column in the table.
     */
    enum PSMemberStatus {
        BLOCKED,
        ACTIVE,
        UNCONFIRMED
    }

    /**
     * Gets the account id of this membership.
     *
     * @return The account id, never null or empty, "0" if not persisted.
     */
    String getId();

    /**
     * Gets the user id of this membership.
     *
     * @return The user id, never null or empty.
     */
    String getUserId();

    /**
     * Gets the email address of this membership.
     *
     * @return The email, never empty, may be null if not set.
     */
    Optional<String> getEmailAddress();

    /**
     * Gets the password value stored with this membership.
     *
     * @return The password, never null or empty.
     */
    String getPassword();

    /**
     * Gets the date and time when this membership account was last accessed.
     * May be used for determining if a session has timed out.
     *
     * @return The date-time when last accessed, never null.
     */
    Optional<Date> getLastAccessed();

    /**
     * Gets the last session id used by this account.
     *
     * @return The session id, never empty, may be null if no session has been created
     * or if the previously used session has been expired and thus cleared.
     */
    Optional<String> getSessionId();

    /**
     * Sets the account id.
     *
     * @param accountId The id, may not be null or empty.
     */
    void setId(String accountId);

    /**
     * Sets the user Id of this membership account.
     *
     * @param userId The id, may not be null or empty.
     */
    void setUserId(String userId);

    /**
     * Sets the email address of this membership account.
     *
     * @param email The email address, may not be null or empty.
     */
    void setEmailAddress(String email);

    /**
     * Sets the password for this membership account.
     *
     * @param password The password to set, may not be null or empty.
     */
    void setPassword(String password);

    /**
     * Sets the last accessed date for this membership account.
     *
     * @param lastAccessed The last accessed date, may not be null.
     */
    void setLastAccessed(Date lastAccessed);

    /**
     * Sets the session id for this membership account.
     *
     * @param sessionId The session id to set, may be empty, never null.
     */
    void setSessionId(String sessionId);

    /**
     * Sets the key used to identify a password reset request for this membership account.
     *
     * @param pwdResetKey The key, never empty, may be null to clear the key.
     */
    void setPwdResetKey(String pwdResetKey);

    /**
     * Gets the key used to identify a password reset request for this membership account.
     *
     * @return The key, never empty, may be null.
     */
    Optional<String> getPwdResetKey();

    /**
     * Sets the date this membership account was created.
     *
     * @param createdDate The date, never null.
     */
    void setCreatedDate(Date createdDate);

    /**
     * Gets the date this membership account was created.
     *
     * @return The date, may be null if never set.
     */
    Optional<Date> getCreatedDate();

    /**
     * Gets the status of the membership account.
     *
     * @return The status, a PSMemberStatus object, never empty or null.
     */
    PSMemberStatus getStatus();

    /**
     * Sets the status of the membership account.
     *
     * @param status The status, a PSMemberStatus object, never empty or null.
     */
    void setStatus(PSMemberStatus status);

    /**
     * Gets the groups of the membership account.
     *
     * @return The groups, may be empty but never null.
     */
    Optional<String> getGroups();

    /**
     * Sets the groups of the membership account.
     *
     * @param groups The groups, may be empty but never null.
     */
    void setGroups(String groups);
}
