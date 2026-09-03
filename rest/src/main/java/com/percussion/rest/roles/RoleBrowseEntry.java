/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.roles;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * One role in the Admin SE-03 browse catalog, with Workbench grouping metadata.
 *
 * <p>{@code groups} lists every navigator folder the role appears under ({@code community} and/or
 * {@code workflow}, or solely {@code unassigned}). A role that is both community- and
 * workflow-assigned appears under both; unassigned is exclusive.
 */
@XmlRootElement(name = "RoleBrowseEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "RoleBrowseEntry",
    description =
        "Role summary for Developer Security Roles browse, including community/workflow/unassigned"
            + " grouping metadata.")
public class RoleBrowseEntry {

  @Schema(description = "Unique role name", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "Role description when known")
  private String description;

  @ArraySchema(
      schema =
          @Schema(
              implementation = String.class,
              description = "Grouping keys: community, workflow, and/or unassigned"))
  private List<String> groups;

  @ArraySchema(
      schema =
          @Schema(
              implementation = String.class,
              description = "Community names that include this role (sorted)"))
  private List<String> communities;

  @ArraySchema(
      schema =
          @Schema(
              implementation = String.class,
              description = "Workflow names that include this role (sorted)"))
  private List<String> workflows;

  public RoleBrowseEntry() {
    // Default constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getGroups() {
    if (groups == null) {
      groups = new ArrayList<>();
    }
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public List<String> getCommunities() {
    if (communities == null) {
      communities = new ArrayList<>();
    }
    return communities;
  }

  public void setCommunities(List<String> communities) {
    this.communities = communities;
  }

  public List<String> getWorkflows() {
    if (workflows == null) {
      workflows = new ArrayList<>();
    }
    return workflows;
  }

  public void setWorkflows(List<String> workflows) {
    this.workflows = workflows;
  }
}
