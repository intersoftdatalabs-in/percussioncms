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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Community in Percussion CMS.
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class Community {

    private long id;
    @Schema
    private Guid guid;
    @Schema
    private String name;
    @Schema
    private String description;
    private String label;
    private CommunityRoleList roleList;

    public Community() {}

    public Community(long id, Guid guid, String name, String description, String label) {
        this.id = id;
        this.guid = guid;
        this.name = name;
        this.description = description;
        this.label = label;
    }

    public Community(long id, Guid guid, String name, String description, String label, CommunityRoleList roleList) {
        this(id, guid, name, description, label);
        this.roleList = roleList;
    }

    /**
     * Gets the community ID.
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the community ID.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Gets the community GUID.
     */
    public Optional<Guid> getGuid() {
        return Optional.ofNullable(guid);
    }

    public void setGuid(Guid guid) {
        this.guid = guid;
    }

    /**
     * Gets the community name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the community description.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the community label.
     */
    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the list of roles associated with this community.
     */
    public Optional<CommunityRoleList> getRoleList() {
        return Optional.ofNullable(roleList);
    }

    public void setRoleList(CommunityRoleList roleList) {
        this.roleList = roleList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Community)) return false;
        var that = (Community) o;
        return id == that.id &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(label, that.label) &&
                Objects.equals(roleList, that.roleList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guid, name, description, label, roleList);
    }

    @Override
    public String toString() {
        return "Community{" +
                "id=" + id +
                ", guid=" + guid +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", label='" + label + '\'' +
                ", roleList=" + roleList +
                '}';
    }
}
