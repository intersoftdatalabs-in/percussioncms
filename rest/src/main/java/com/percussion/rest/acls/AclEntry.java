/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.rest.acls;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

@XmlRootElement
@Schema(description = "AclEntry")
public class AclEntry {

    private long id;
    private String name;
    private Principal principal;
    private TypedPrincipal type;
    private UserAccessLevelList permissions;
    private long aclId;

    public AclEntry() {}

    public AclEntry(long id, String name, Principal principal, TypedPrincipal type,
                    UserAccessLevelList permissions, long aclId) {
        this.id = id;
        this.name = name;
        this.principal = principal;
        this.type = type;
        this.permissions = permissions;
        this.aclId = aclId;
    }

    public long getAclId() {
        return aclId;
    }

    public void setAclId(long aclId) {
        this.aclId = aclId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<Principal> getPrincipal() {
        return Optional.ofNullable(principal);
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Optional<TypedPrincipal> getType() {
        return Optional.ofNullable(type);
    }

    public void setType(TypedPrincipal type) {
        this.type = type;
    }

    public Optional<UserAccessLevelList> getPermissions() {
        return Optional.ofNullable(permissions);
    }

    public void setPermissions(UserAccessLevelList permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "AclEntry{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", principal=" + principal +
                ", type=" + type +
                ", permissions=" + permissions +
                ", aclId=" + aclId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AclEntry)) return false;
        var aclEntry = (AclEntry) o;
        return id == aclEntry.id &&
                aclId == aclEntry.aclId &&
                Objects.equals(name, aclEntry.name) &&
                Objects.equals(principal, aclEntry.principal) &&
                Objects.equals(type, aclEntry.type) &&
                Objects.equals(permissions, aclEntry.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, principal, type, permissions, aclId);
    }
}
