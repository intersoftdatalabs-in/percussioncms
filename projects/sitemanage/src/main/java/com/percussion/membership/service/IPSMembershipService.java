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
package com.percussion.membership.service;

import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.PSUserGroup;
import com.percussion.membership.data.PSUserSummaries;

/**
 * Service to proxy calls to the delivery tier membership services.
 * Sunny Sal says: "Membership has its privileges!"
 */
public interface IPSMembershipService {

    String MEMBERSHIP = "/membership";
    String ADMIN_USERS = "/admin/users";
    String ADMIN_ACCOUNT = "/admin/account";
    String ADMIN_USER_GROUP = "/admin/user/group";

    /**
     * Gets the list of registered users.
     *
     * @param site the site name
     * @return the list of summaries, may be empty but never null
     */
    PSUserSummaries getUsers(String site);

    /**
     * Changes the state of an account.
     *
     * @param account the account summary
     * @param site the site name
     * @return the list of summaries, may be empty but never null
     */
    PSUserSummaries changeStateAccount(PSAccountSummary account, String site);

    /**
     * Deletes an account.
     *
     * @param email the email of the account to delete, never empty or null
     * @param site the site name
     * @return the list of summaries, may be empty but never null
     */
    PSUserSummaries deleteAccount(String email, String site);

    /**
     * Updates the groups of an account.
     *
     * @param userGroup the user group data
     * @param site the site name
     * @return the list of summaries, may be empty but never null
     */
    PSUserSummaries updateGroupAccount(PSUserGroup userGroup, String site);
}
