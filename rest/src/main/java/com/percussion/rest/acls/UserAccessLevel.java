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

package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.PermissionList;
import com.percussion.rest.Permissions;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Represents a user's access level for a given ACL.
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson/CXF JSON emits
 * permission scalars, not Optional beans. Matches {@link Acl} getter style (issue #3388 slice 8 /
 * #3420).
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User Access Level")
public class UserAccessLevel {

  @Schema(description = "Unique id of this access level")
  private long id;

  @Schema(description = "The permissions defined for this ACL")
  private Permissions permission;

  private PermissionList permissions;

  public UserAccessLevel() {}

  public UserAccessLevel(long id, Permissions permission, PermissionList permissions) {
    this.id = id;
    this.permission = permission;
    this.permissions = permissions;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Permissions getPermission() {
    return permission;
  }

  public void setPermission(Permissions permission) {
    this.permission = permission;
  }

  public PermissionList getPermissions() {
    return permissions;
  }

  public void setPermissions(PermissionList permissions) {
    this.permissions = permissions;
  }

  @Override
  public String toString() {
    return "UserAccessLevel{"
        + "id="
        + id
        + ", permission="
        + permission
        + ", permissions="
        + permissions
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserAccessLevel)) return false;
    var that = (UserAccessLevel) o;
    return id == that.id
        && Objects.equals(permission, that.permission)
        && Objects.equals(permissions, that.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, permission, permissions);
  }
}
