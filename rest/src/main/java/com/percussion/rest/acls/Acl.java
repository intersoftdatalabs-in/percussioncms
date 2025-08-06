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

import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

@XmlRootElement
@Schema(description = "Acl")
public class Acl {

    private long id;
    private Guid guid;
    private String name;
    private long objectId;
    private String description;
    private AclEntryList aclEntries;
    private int objectType;
    private Guid objectGuid; // fixed typo

    public Acl() {}

    public Acl(long id, Guid guid, String name, long objectId, String description,
               AclEntryList aclEntries, int objectType, Guid objectGuid) {
        this.id = id;
        this.guid = guid;
        this.name = name;
        this.objectId = objectId;
        this.description = description;
        this.aclEntries = aclEntries;
        this.objectType = objectType;
        this.objectGuid = objectGuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Optional<Guid> getGuid() {
        return Optional.ofNullable(guid);
    }

    public void setGuid(Guid guid) {
        this.guid = guid;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<AclEntryList> getAclEntries() {
        return Optional.ofNullable(aclEntries);
    }

    public void setAclEntries(AclEntryList aclEntries) {
        this.aclEntries = aclEntries;
    }

    public int getObjectType() {
        return objectType;
    }

    public void setObjectType(int objectType) {
        this.objectType = objectType;
    }

    public Optional<Guid> getObjectGuid() {
        return Optional.ofNullable(objectGuid);
    }

    public void setObjectGuid(Guid objectGuid) {
        this.objectGuid = objectGuid;
    }

    @Override
    public String toString() {
        return "Acl{" +
                "id=" + id +
                ", guid=" + guid +
                ", name='" + name + '\'' +
                ", objectId=" + objectId +
                ", description='" + description + '\'' +
                ", aclEntries=" + aclEntries +
                ", objectType=" + objectType +
                ", objectGuid=" + objectGuid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Acl)) return false;
        var acl = (Acl) o;
        return id == acl.id &&
                objectId == acl.objectId &&
                objectType == acl.objectType &&
                Objects.equals(guid, acl.guid) &&
                Objects.equals(name, acl.name) &&
                Objects.equals(description, acl.description) &&
                Objects.equals(aclEntries, acl.aclEntries) &&
                Objects.equals(objectGuid, acl.objectGuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guid, name, objectId, description, aclEntries, objectType, objectGuid);
    }
}
