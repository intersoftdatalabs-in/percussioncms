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

package com.percussion.rest.preferences;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a user preference. Sunny Sal: "User preference ka hero, customization ka zero!"
 *
 * <p>{@link JsonRootName} matches {@link XmlRootElement} so Jackson
 * {@code WRAP_ROOT_VALUE}/{@code UNWRAP_ROOT_VALUE} expects wire shape
 * {@code { "UserPreference": { ... } }} (WebUI #2708).
 */
@XmlRootElement(name = "UserPreference")
@JsonRootName("UserPreference")
@Schema(description = "UserPreference")
public class UserPreference {

  @Schema(description = "Property Name", required = true)
  private String name;

  @Schema(description = "Property Value", required = true)
  private String value;

  @Schema(
      description =
          "The category type, which is an arbitrary string used to group related "
              + "properties together. All categories beginning with sys_ are reserved by "
              + "the system. The category for session variables is sys_session.")
  private String category;

  @Schema(
      description =
          "The context to which the property belongs. May be null or empty. system or private")
  private String context;

  @Schema(description = "Extra Parameter / Action")
  private String extraParam;

  @Schema(description = "The user name of the user to whom this property belongs.", required = true)
  private String userName;

  public UserPreference() {
    // Default constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public String getExtraParam() {
    return extraParam;
  }

  public void setExtraParam(String extraParam) {
    this.extraParam = extraParam;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }
}
