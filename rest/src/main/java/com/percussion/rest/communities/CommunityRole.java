// REFACTORED: CP-JAVA21
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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Represents a Community Role association.
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits scalars
 * rather than Optional beans ({@code empty}/{@code present}).
 */
@XmlRootElement(name = "CommunityRole")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a Community Role association")
public class CommunityRole {

  @Schema(description = "The long id of the community")
  private long communityId;

  @Schema(description = "The long id of the Role")
  private long roleId;

  @Schema(description = "The name of the role")
  private String roleName;

  @Schema(description = "Guid of the community", required = true)
  private Guid communityGuid;

  @Schema(description = "Guid of the Role", required = true)
  private Guid roleGuid;

  public CommunityRole() {}

  public CommunityRole(
      long communityId, long roleId, String roleName, Guid communityGuid, Guid roleGuid) {
    this.communityId = communityId;
    this.roleId = roleId;
    this.roleName = roleName;
    this.communityGuid = communityGuid;
    this.roleGuid = roleGuid;
  }

  public Guid getCommunityGuid() {
    return communityGuid;
  }

  public void setCommunityGuid(Guid communityGuid) {
    this.communityGuid = communityGuid;
  }

  public Guid getRoleGuid() {
    return roleGuid;
  }

  public void setRoleGuid(Guid roleGuid) {
    this.roleGuid = roleGuid;
  }

  public long getCommunityId() {
    return communityId;
  }

  public void setCommunityId(long communityId) {
    this.communityId = communityId;
  }

  public long getRoleId() {
    return roleId;
  }

  public void setRoleId(long roleId) {
    this.roleId = roleId;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CommunityRole)) return false;
    CommunityRole that = (CommunityRole) o;
    return communityId == that.communityId
        && roleId == that.roleId
        && Objects.equals(roleName, that.roleName)
        && Objects.equals(communityGuid, that.communityGuid)
        && Objects.equals(roleGuid, that.roleGuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(communityId, roleId, roleName, communityGuid, roleGuid);
  }

  @Override
  public String toString() {
    return "CommunityRole{"
        + "communityId="
        + communityId
        + ", roleId="
        + roleId
        + ", roleName='"
        + roleName
        + '\''
        + ", communityGuid="
        + communityGuid
        + ", roleGuid="
        + roleGuid
        + '}';
  }
}
