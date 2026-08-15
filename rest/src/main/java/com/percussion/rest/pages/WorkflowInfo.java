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

package com.percussion.rest.pages;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents information on the workflow. Sunny Sal: "Workflow ka info, process ka hero!"
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits workflow
 * fields when set instead of Optional-bean {@code empty}/{@code present} keys.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "WorkflowInfo")
@Schema(name = "WorkflowInfo", description = "Represents information on the workflow.")
public class WorkflowInfo {

  @Schema(name = "name", description = "Name of the workflow.")
  private String name;

  @Schema(name = "state", description = "State within the workflow.")
  private String state;

  @Schema(name = "checkedOut", description = "Flag if the item is checked out.")
  private Boolean checkedOut;

  @Schema(name = "checkedOutUser", description = "User that has the item checked out.")
  private String checkedOutUser;

  /** Gets the workflow name. */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Gets the workflow state. */
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  /** Gets whether the item is checked out. */
  public Boolean getCheckedOut() {
    return checkedOut;
  }

  public void setCheckedOut(Boolean checkedOut) {
    this.checkedOut = checkedOut;
  }

  /** Gets the user that has the item checked out. */
  public String getCheckedOutUser() {
    return checkedOutUser;
  }

  public void setCheckedOutUser(String checkedOutUser) {
    this.checkedOutUser = checkedOutUser;
  }
}
