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
import java.util.Collection;
import java.util.List;

/**
 * Admin GET envelope for SE-03 roles browse (community / workflow / unassigned grouping).
 */
@XmlRootElement(name = "RoleBrowseCatalog")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "RoleBrowseCatalog",
    description =
        "Admin roles browse catalog for Workbench Security Design SE-03. Optional group filter"
            + " limits entries to community, workflow, or unassigned.")
public class RoleBrowseCatalog {

  @Schema(
      description =
          "Optional filter that was applied (community, workflow, unassigned). Null/absent when"
              + " returning the full catalog.")
  private String group;

  @ArraySchema(schema = @Schema(implementation = RoleBrowseEntry.class))
  private List<RoleBrowseEntry> roles;

  public RoleBrowseCatalog() {
    // Default constructor
  }

  public RoleBrowseCatalog(Collection<? extends RoleBrowseEntry> roles) {
    this.roles = new ArrayList<>(roles);
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public List<RoleBrowseEntry> getRoles() {
    if (roles == null) {
      roles = new ArrayList<>();
    }
    return roles;
  }

  public void setRoles(List<RoleBrowseEntry> roles) {
    this.roles = roles;
  }
}
