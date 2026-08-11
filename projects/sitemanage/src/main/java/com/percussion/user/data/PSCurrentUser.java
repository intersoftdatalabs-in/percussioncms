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
package com.percussion.user.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/** Represents the current user, including role flags and session community summary. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@XmlRootElement(name = "CurrentUser")
@JsonRootName("CurrentUser")
public class PSCurrentUser extends PSUser {
  private static final long serialVersionUID = 1L;

  private boolean accessibilityUser = false;
  private boolean adminUser = false;
  private boolean designerUser = false;

  /** Role-resolved community names for the signed-in user (read-only summary). */
  private ArrayList<String> communities = new ArrayList<>();

  /** Active community name for the current session, or empty when unknown. */
  private String currentCommunity = "";

  public PSCurrentUser() {
    super();
  }

  public PSCurrentUser(PSUser user) {
    setName(user.getName());
    setPassword(user.getPassword());
    setEmail(user.getEmail());
    setProviderType(user.getProviderType());
    setRoles(user.getRoles());
  }

  public boolean isAccessibilityUser() {
    return accessibilityUser;
  }

  public void setAccessibilityUser(boolean accessibilityUser) {
    this.accessibilityUser = accessibilityUser;
  }

  public boolean isAdminUser() {
    return adminUser;
  }

  public void setAdminUser(boolean adminUser) {
    this.adminUser = adminUser;
  }

  public boolean isDesignerUser() {
    return designerUser;
  }

  public void setDesignerUser(boolean designerUser) {
    this.designerUser = designerUser;
  }

  public List<String> getCommunities() {
    return communities;
  }

  @SuppressWarnings("unchecked")
  public void setCommunities(List<String> communities) {
    if (communities == null) {
      this.communities = new ArrayList<>();
    } else if (communities instanceof ArrayList) {
      this.communities = (ArrayList<String>) communities;
    } else {
      this.communities = new ArrayList<>(communities);
    }
  }

  public String getCurrentCommunity() {
    return currentCommunity;
  }

  public void setCurrentCommunity(String currentCommunity) {
    this.currentCommunity = currentCommunity == null ? "" : currentCommunity;
  }
}
