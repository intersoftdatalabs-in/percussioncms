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

// REFACTORED: CP-JAVA11

package com.percussion.rest.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.LinkRef;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a User. Sunny Sal: "User ka hero, login ka zero!"
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code
 * userName}, {@code firstName}, and related scalars when set. Optional-returning getters
 * historically serialized as empty/present beans or dropped fields under {@code
 * @JsonInclude(NON_NULL)} (issue #3388). Matches {@link
 * com.percussion.rest.contenttypes.ContentType} getter style (issue #1693).
 */
@XmlRootElement(name = "User")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "User", description = "Represents a User.")
public class User {

  @Schema(name = "userName", required = true, description = "The User Id of the user")
  private String userName;

  @Schema(name = "firstName", description = "The first name of the user - Read only for LDAP users")
  private String firstName;

  @Schema(name = "lastName", description = "The last name of the user - Read only for LDAP users")
  private String lastName;

  @Schema(name = "email", description = "The email address of the User - read only for LDAP users.")
  private String emailAddress;

  @Schema(
      name = "userType",
      required = true,
      description = "The UserType of the user. INTERNAL or DIRECTORY ")
  private String userType;

  @Schema(name = "password", required = true, description = "The user's password. May only be set.")
  private String password;

  @Schema(name = "bookmarkedPages", description = "List of Pages bookmarked by this user.")
  private List<LinkRef> bookmarkedPages;

  @Schema(name = "recentPages", description = "List of Pages Recently edited by this user.")
  private List<LinkRef> recentPages;

  @Schema(
      name = "recentAssetTypes",
      description = "List of Recently used Asset types by this user.")
  private List<LinkRef> recentAssetTypes;

  @Schema(
      name = "recentAssetFolders",
      description = "List of Recently used Asset folders by this user.")
  private List<LinkRef> recentAssetFolders;

  @Schema(name = "recentSiteFolders", description = "List of Recently used Site Folders this user.")
  private List<LinkRef> recentSiteFolders;

  @Schema(name = "recentTemplates", description = "List of Recently used templates by this user.")
  private List<LinkRef> recentTemplates;

  @Schema(
      name = "personalPage",
      description = "The qualified folder path to this user's Personal Page")
  private LinkRef personalPage;

  @Schema(name = "personAssets", description = "A list of PersonAssets that represent this user.")
  private List<LinkRef> personAssets;

  @Schema(name = "roles", description = "A list of the Role names that this user belongs to.")
  private List<String> roles;

  @Schema(
      name = "selectedCommunity",
      description = "The Community that the user currently has selected.")
  private Community selectedCommunity;

  @Schema(
      name = "userCommunities",
      description = "The list of communities that the user belongs to.")
  private CommunityList userCommunities;

  public User() {
    // Default constructor
  }

  public CommunityList getUserCommunities() {
    return userCommunities;
  }

  public void setUserCommunities(CommunityList userCommunities) {
    this.userCommunities = userCommunities;
  }

  public Community getSelectedCommunity() {
    return selectedCommunity;
  }

  public void setSelectedCommunity(Community selectedCommunity) {
    this.selectedCommunity = selectedCommunity;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getUserType() {
    return userType;
  }

  public void setUserType(String userType) {
    this.userType = userType;
  }

  public List<LinkRef> getBookmarkedPages() {
    if (bookmarkedPages == null) {
      bookmarkedPages = new ArrayList<>();
    }
    return bookmarkedPages;
  }

  public void setBookmarkedPages(List<LinkRef> bookmarkedPages) {
    this.bookmarkedPages = bookmarkedPages;
  }

  public List<LinkRef> getRecentPages() {
    if (recentPages == null) {
      recentPages = new ArrayList<>();
    }
    return recentPages;
  }

  public void setRecentPages(List<LinkRef> recentPages) {
    this.recentPages = recentPages;
  }

  public List<LinkRef> getRecentAssetTypes() {
    if (recentAssetTypes == null) {
      recentAssetTypes = new ArrayList<>();
    }
    return recentAssetTypes;
  }

  public void setRecentAssetTypes(List<LinkRef> recentAssetTypes) {
    this.recentAssetTypes = recentAssetTypes;
  }

  public List<LinkRef> getRecentAssetFolders() {
    if (recentAssetFolders == null) {
      recentAssetFolders = new ArrayList<>();
    }
    return recentAssetFolders;
  }

  public void setRecentAssetFolders(List<LinkRef> recentAssetFolders) {
    this.recentAssetFolders = recentAssetFolders;
  }

  public List<LinkRef> getRecentSiteFolders() {
    if (recentSiteFolders == null) {
      recentSiteFolders = new ArrayList<>();
    }
    return recentSiteFolders;
  }

  public void setRecentSiteFolders(List<LinkRef> recentSiteFolders) {
    this.recentSiteFolders = recentSiteFolders;
  }

  public List<LinkRef> getRecentTemplates() {
    if (recentTemplates == null) {
      recentTemplates = new ArrayList<>();
    }
    return recentTemplates;
  }

  public void setRecentTemplates(List<LinkRef> recentTemplates) {
    this.recentTemplates = recentTemplates;
  }

  public LinkRef getPersonalPage() {
    return personalPage;
  }

  public void setPersonalPage(LinkRef personalPage) {
    this.personalPage = personalPage;
  }

  public List<LinkRef> getPersonAssets() {
    if (personAssets == null) {
      personAssets = new ArrayList<>();
    }
    return personAssets;
  }

  public void setPersonAssets(List<LinkRef> personAssets) {
    this.personAssets = personAssets;
  }

  public List<String> getRoles() {
    if (roles == null) {
      roles = new ArrayList<>();
    }
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
