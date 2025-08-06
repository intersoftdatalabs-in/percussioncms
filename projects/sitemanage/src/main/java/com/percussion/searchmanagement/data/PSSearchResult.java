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
package com.percussion.searchmanagement.data;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single result from a full text search.
 */
@XmlRootElement(name = "SearchResult")
public class PSSearchResult {

    private String name;
    private String type;
    private String status;
    private Date lastModified;
    private Date lastPublished;
    private Date created;
    private String createdBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastModified() {
        return lastModified == null ? null : new Date(lastModified.getTime());
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified == null ? null : new Date(lastModified.getTime());
    }

    public Date getLastPublished() {
        return lastPublished == null ? null : new Date(lastPublished.getTime());
    }

    public void setLastPublished(Date lastPublished) {
        this.lastPublished = lastPublished == null ? null : new Date(lastPublished.getTime());
    }

    public Date getCreated() {
        return created == null ? null : new Date(created.getTime());
    }

    public void setCreated(Date created) {
        this.created = created == null ? null : new Date(created.getTime());
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
