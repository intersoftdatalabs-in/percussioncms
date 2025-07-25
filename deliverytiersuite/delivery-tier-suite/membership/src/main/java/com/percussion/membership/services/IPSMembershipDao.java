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
// REFACTORED: CP-JAVA11
package com.percussion.membership.services;

import com.percussion.membership.data.IPSMembership;
import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;

import java.util.List;
import java.util.Optional;

/**
 * DAO service for the membership service.
 *
 * @author Jay Seletz
 */
public interface IPSMembershipDao {

    /**
     * Searches for a member with a session matching the supplied id.
     *
     * @param sessionId The session id to use, not null or empty.
     * @return Optional member, empty if not found.
     * @throws Exception if there are any errors.
     */
    Optional<IPSMembership> findMemberBySessionId(String sessionId) throws Exception;

    /**
     * Searches for a member matching the supplied user id.
     *
     * @param userId The id to use, not null or empty.
     * @return Optional member, empty if not found.
     */
    Optional<IPSMembership> findMemberByUserId(String userId);

    /**
     * Searches for a member matching the supplied password reset key.
     *
     * @param pwdResetKey The key to use, not null or empty.
     * @return Optional member, empty if not found.
     */
    Optional<IPSMembership> findMemberByPwdResetKey(String pwdResetKey);

    /**
     * Creates an instance of a member. The member is not yet persisted.
     *
     * @param userId The member's user id, not null or empty.
     * @param password The member's password, not null or empty.
     * @return The member, never null.
     * @throws PSMemberExistsException if a member with that user name already exists.
     * @throws Exception if there are any errors.
     */
    IPSMembership createMember(String userId, String password, PSMemberStatus status) throws PSMemberExistsException, Exception;

    /**
     * Saves the supplied member.
     *
     * @param member The member to save, not null.
     * @throws PSMemberExistsException if a member with that user name already exists.
     * @throws Exception if there are any errors.
     */
    void saveMember(IPSMembership member) throws Exception;

    /**
     * Gets all membership accounts.
     * Deprecated for performance reasons: see {@link IPSMembershipDao#findMembers(PSDefaultRangedPage pager)}
     * @return List of all members, sorted ascending by userId, never null, may be empty.
     * @throws Exception if there are any unexpected errors.
     */
    List<IPSMembership> findMembers() throws Exception;

    /**
     * Changes the state of an account.
     *
     * @param account {@link PSAccountSummary} object with the data to process.
     * @throws Exception if there are any unexpected errors.
     */
    void changeStatusAccount(PSAccountSummary account) throws Exception;

    /**
     * Deletes an account.
     *
     * @param email the email relative to the account to delete, never empty or null.
     * @throws Exception if there are any unexpected errors.
     */
    void deleteAccount(String email) throws Exception;
}
