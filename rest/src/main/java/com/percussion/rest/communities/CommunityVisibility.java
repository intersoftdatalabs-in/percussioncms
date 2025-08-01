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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import com.percussion.rest.ObjectSummary;
import com.percussion.rest.ObjectSummaryList;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the visibility of a community and its visible objects.
 */
@XmlRootElement
@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityVisibility {

    @Schema(description = "The id of the Community")
    private long id;

    @Schema(description = "The Guid of the community")
    private Guid guid;

    @ArraySchema(schema = @Schema(implementation = ObjectSummary.class))
    private ObjectSummaryList visibleObjects;

    public CommunityVisibility() {}

    public CommunityVisibility(long id, Guid guid) {
        this.id = id;
        this.guid = guid;
    }

    public CommunityVisibility(long id, Guid guid, ObjectSummaryList visibleObjects) {
        this.id = id;
        this.guid = guid;
        this.visibleObjects = visibleObjects;
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

    public Optional<ObjectSummaryList> getVisibleObjects() {
        return Optional.ofNullable(visibleObjects);
    }

    public void setVisibleObjects(ObjectSummaryList visibleObjects) {
        this.visibleObjects = visibleObjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunityVisibility)) return false;
        var that = (CommunityVisibility) o;
        return id == that.id &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(visibleObjects, that.visibleObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guid, visibleObjects);
    }

    @Override
    public String toString() {
        return "CommunityVisibility{" +
                "id=" + id +
                ", guid=" + guid +
                ", visibleObjects=" + visibleObjects +
                '}';
    }
}
