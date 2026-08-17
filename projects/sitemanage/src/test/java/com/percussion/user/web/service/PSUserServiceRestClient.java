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
package com.percussion.user.web.service;

import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.test.PSObjectRestClient;
import com.percussion.user.data.*;
import com.percussion.user.service.IPSUserService;
import java.util.List;

/** REST client for user service. Modernized for Java 11 and Google Java Style. */
public class PSUserServiceRestClient extends PSObjectRestClient implements IPSUserService {

  private String path = "/Rhythmyx/services/user/user";

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  @Override
  public PSUser create(final PSUser user) throws PSDataServiceException {
    return postObjectToPath(concatPath(getPath(), "create"), user, PSUser.class);
  }

  @Override
  public void delete(final String name) {
    super.delete(concatPath(getPath(), "delete", name));
  }

  @Override
  public PSUser find(final String name) throws PSDataServiceException {
    return getObjectFromPath(concatPath(getPath(), "find", name), PSUser.class);
  }

  @Override
  public List<PSExternalUser> findUsersFromDirectoryService(final String query)
      throws PSDirectoryServiceException {
    return getObjectsFromPath(concatPath(getPath(), "external/find", query), PSExternalUser.class);
  }

  @Override
  public List<PSImportedUser> importDirectoryUsers(final PSImportUsers importUsers)
      throws PSDirectoryServiceException {
    return postObjectToPathAndGetObjects(
        concatPath(getPath(), "import"), importUsers, PSImportedUser.class);
  }

  @Override
  public PSDirectoryServiceStatus checkDirectoryService() {
    return getObjectFromPath(
        concatPath(getPath(), "external/status"), PSDirectoryServiceStatus.class);
  }

  @Override
  public PSRoleList getRoles() {
    return getObjectFromPath(concatPath(getPath(), "roles"), PSRoleList.class);
  }

  @Override
  public PSUserList getUsers() throws PSDataServiceException {
    return getObjectFromPath(concatPath(getPath(), "users"), PSUserList.class);
  }

  @Override
  public PSUserList getUsersByRole(final String roleName) {
    return getObjectFromPath(concatPath(getPath(), "usersByRole", roleName), PSUserList.class);
  }

  @Override
  public PSUser update(final PSUser user) throws PSDataServiceException {
    return postObjectToPath(concatPath(getPath(), "update"), user, PSUser.class);
  }

  @Override
  public PSUser changePassword(final PSUser user) {
    return putObjectToPath(concatPath(getPath(), "changepw"), user, PSUser.class);
  }

  @Override
  public PSCurrentUser getCurrentUser() {
    return getObjectFromPath(concatPath(getPath(), "current"), PSCurrentUser.class);
  }

  @Override
  public PSCurrentUser updateMyAccount(final PSUserAccountUpdate update)
      throws PSDataServiceException {
    return putObjectToPath(concatPath(getPath(), "profile"), update, PSCurrentUser.class);
  }

  @Override
  public PSCurrentUser updateMyDefaultCommunity(final String communityName)
      throws PSDataServiceException {
    return putObjectToPath(
        concatPath(getPath(), "defaultCommunity"),
        communityName == null ? "" : communityName,
        PSCurrentUser.class);
  }

  @Override
  public PSAccessLevel getAccessLevel(final PSAccessLevelRequest request) {
    return postObjectToPath(concatPath(getPath(), "accessLevel"), request, PSAccessLevel.class);
  }

  /** Not supported: check if user is admin. */
  @Override
  public boolean isAdminUser(final String userName) {
    throw new UnsupportedOperationException(
        "Checking if current user has Admin role is not yet supported");
  }

  @Override
  public PSUserList getUserNames(final String nameFilter) {
    return getObjectFromPath(concatPath(getPath(), "users/names", nameFilter), PSUserList.class);
  }

  /** Not supported: check if user is design user. */
  @Override
  public boolean isDesignUser(final String userName) {
    throw new UnsupportedOperationException(
        "Checking if current user has Design role is not yet supported");
  }

  @Override
  public String getHomepageOverride(final String userName) throws PSDataServiceException {
    return getObjectFromPath(concatPath(getPath(), "homepage", userName), String.class);
  }

  @Override
  public String setHomepageOverride(final String userName, final String homepage)
      throws PSDataServiceException {
    return putObjectToPath(concatPath(getPath(), "homepage", userName), homepage, String.class);
  }

  @Override
  public void clearHomepageOverride(final String userName) throws PSDataServiceException {
    delete(concatPath(getPath(), "homepage", userName));
  }
}
