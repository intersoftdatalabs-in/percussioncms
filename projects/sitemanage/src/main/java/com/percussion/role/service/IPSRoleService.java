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
package com.percussion.role.service;

import com.percussion.role.data.PSRole;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSUserList;

/** The role service is responsible for managing roles and their user associations. */
public interface IPSRoleService {
  String ADMINISTRATOR_ROLE = "Admin";
  String DESIGNER_ROLE = "Designer";
  String META_DATA_HOMEPAGE_PREFIX = "perc.role.homepage.";
  String HOMEPAGE_TYPE_DASHBOARD = "Dashboard";
  String HOMEPAGE_TYPE_EDITOR = "Editor";
  String HOMEPAGE_TYPE_HOME = "Home";

  /**
   * Finds a role.
   *
   * @param name of the role to find, wrapped by a {@link PSStringWrapper} object. Never null.
   * @return never null.
   * @throws PSDataServiceException if the role cannot be found.
   */
  PSRole find(PSStringWrapper name) throws PSDataServiceException;

  /**
   * Creates (and saves) a new role. Adds specified users to the role. Also adds the role to each
   * workflow and each workflow state as a Reader.
   *
   * @param role never null.
   * @return never null.
   * @throws PSDataServiceException if the role cannot be created.
   */
  PSRole create(PSRole role) throws PSDataServiceException;

  /**
   * Updates the role with the given object. All properties are overwritten except for the role
   * name.
   *
   * @param role the {@link PSRole#getName()} is used to determine which role to update. Never null.
   * @return never null.
   * @throws PSDataServiceException if the role cannot be updated.
   */
  PSRole update(PSRole role) throws PSDataServiceException;

  /**
   * Deletes the specified role.
   *
   * @param name of the role to find, wrapped by a {@link PSStringWrapper} object. Never null.
   * @throws PSDataServiceException if unable to delete the role.
   */
  void delete(PSStringWrapper name) throws PSDataServiceException;

  /**
   * Finds the users which are currently not assigned to the specified role.
   *
   * @param role never null.
   * @return list of users, sorted alphabetically (case-insensitive), never null.
   * @throws PSDataServiceException if unable to find the role.
   */
  PSUserList getAvailableUsers(PSRole role) throws PSDataServiceException;

  /**
   * Validates that the specified role meets the following for deletion:
   *
   * <ul>
   *   <li>All users assigned to the role are also assigned to at least one other role.
   *   <li>The role is not being used by a workflow, i.e., it is not assigned permissions (other
   *       than READ) in a workflow.
   * </ul>
   *
   * @param role never null. A role object is used instead of a string in order to support non-ascii
   *     characters in the role name.
   * @throws PSDataServiceException with an appropriate message if the role does not meet the
   *     requirements.
   */
  void validateForDelete(PSRole role) throws PSDataServiceException;

  /**
   * Validates the specified users for deletion from a role. This checks to see if the users are in
   * more than one role.
   *
   * @param userList list of user names, never null.
   * @throws PSDataServiceException with an appropriate message if there are users which are only in
   *     one role.
   */
  void validateDeleteUsersFromRole(PSUserList userList) throws PSDataServiceException;

  /**
   * Gets the homepage for the logged in user.
   *
   * @return String never null. Product default is {@link #HOMEPAGE_TYPE_HOME} when unset. When the
   *     user has multiple roles, {@link #HOMEPAGE_TYPE_HOME} wins over Dashboard/Editor (SPA-first
   *     landing).
   */
  String getUserHomepage() throws IPSGenericDao.LoadException;
}
