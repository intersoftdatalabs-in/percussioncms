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

import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.PSUserSummary;
import java.util.List;

/**
 * Membership service for storing, retrieving, and locating members.
 *
 * @author Jay Seletz
 */
public interface IPSMembershipService {

    /**
     * Locates a matching session for the supplied session id and returns the user's name.
     *
     * @param sessionId Session id, not null or empty.
     * @return User summary, null if no valid session is found.
     * @throws Exception If there are any unexpected errors.
     */
    PSUserSummary getUser(String sessionId) throws Exception;

    /**
     * Creates a membership account and a valid session for the user.
     *
     * @param email User id for the account, not null or empty.
     * @param password Password for the account, not null or empty.
     * @param confirmationRequired Indicates if activation through email is required.
     * @param confirmationPage Confirmation page for email, if required.
     * @param customerSite Customer website host address, never empty or null.
     * @return Session id for the user, not null or empty.
     * @throws PSMemberExistsException if a member with that user name already exists.
     * @throws PSAuthenticationFailedException if the member cannot be authenticated.
     * @throws Exception If there are any unexpected errors.
     */
    String createAccount(String email, String password, boolean confirmationRequired, String confirmationPage, String customerSite) throws PSMemberExistsException, PSAuthenticationFailedException, Exception;

    /**
     * Authenticates the supplied credentials and creates a session.
     *
     * @param email User id for the account, not null or empty.
     * @param password Password for the account, not null or empty.
     * @return Session id for the user, not null or empty.
     * @throws PSMemberExistsException if a member with that user name already exists.
     * @throws PSAuthenticationFailedException if the member cannot be authenticated.
     * @throws Exception If there are any unexpected errors.
     */
    String login(String email, String password) throws PSAuthenticationFailedException, Exception;

    /**
     * Destroys the session for the supplied session id.
     *
     * @param sessionId The id, if not valid then method silently returns.
     */
    void logout(String sessionId) throws Exception;

    /**
     * Finds all users registered and returns summary info.
     *
     * @return List of summaries, never null, may be empty.
     * @throws Exception if there are any unexpected errors.
     */
    List<PSUserSummary> findUsers() throws Exception;

    /**
     * Sets a reset key for the supplied email account. Valid for 24 hours.
     *
     * @param email User id for the account, not null or empty.
     * @param linkUrl URL link to the reset page for the email.
     * @return Email for the user, not null or empty.
     * @throws PSAuthenticationFailedException if the member cannot be authenticated.
     * @throws Exception If there are any unexpected errors.
     */
    String setResetKey(String email, String linkUrl) throws PSAuthenticationFailedException, Exception;

    /**
     * Validates the reset key for the supplied email account.
     *
     * @param resetKey Reset key to validate, not null or empty.
     * @return Summary for the user, not null.
     * @throws PSResetPwdException if the reset key is invalid.
     * @throws Exception If there are any unexpected errors.
     */
    PSUserSummary validatePwdResetKey(String resetKey) throws PSAuthenticationFailedException, Exception;

    /**
     * Finds the user account and sets the new password.
     *
     * @param resetKey Token key, not null.
     * @param email User id for the account, not null or empty.
     * @param password Password for the account, not null or empty.
     * @return Session id for the user, not null or empty.
     * @throws PSMemberExistsException if a member with that user name already exists.
     * @throws PSAuthenticationFailedException if the member cannot be authenticated.
     * @throws Exception If there are any unexpected errors.
     */
    String resetPwd(String resetKey, String email, String password) throws PSAuthenticationFailedException, Exception;

    /**
     * Changes the state of an account.
     *
     * @param account {@link PSAccountSummary} with data to process.
     */
    void changeStateAccount(PSAccountSummary account) throws Exception;

    /**
     * Deletes an account.
     *
     * @param email Email of account to delete, never empty or null.
     */
    void deleteAccount(String email) throws Exception;

    /**
     * Confirms an existing account by changing the account status to Enabled.
     *
     * @param confirmKey Token key, not null.
     * @return Id for the user, not null, may be empty.
     * @throws PSAuthenticationFailedException if the member cannot be authenticated.
     * @throws Exception If there are any unexpected errors.
     */
    String confirmAccount(String confirmKey) throws PSAuthenticationFailedException, Exception;

    /**
     * Sets the groups for a given user.
     *
     * @param email User email to update.
     * @param groups User groups to set.
     */
    void setUserGroups(String email, String groups) throws PSAuthenticationFailedException, Exception;
}
