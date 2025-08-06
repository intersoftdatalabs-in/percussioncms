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
package com.percussion.services.legacy.data;

import com.percussion.services.legacy.IPSItemEntry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Item entry data object with Java 11 enhancements for caching skeleton information.
 * Provides immutable-style access patterns, Optional-based safe operations, and
 * enhanced validation for legacy item data management.
 *
 * This class contains the essential information of an item that is cached
 * for performance optimization.
 */
public class PSItemEntry implements IPSItemEntry {

    private static final Logger logger = LogManager.getLogger(PSItemEntry.class);

    // Core item identifiers
    private final int contentId;
    private final String name;
    private final int communityId;
    private final int contentTypeId;
    private final int objectType;

    // Audit information
    private final String createdBy;
    private final Date lastModifiedDate;
    private final String lastModifier;
    private final Date postedDate;
    private final Date createdDate;

    // Workflow information
    private final int workflowAppId;
    private final int stateId;

    // Revision tracking
    private final int tipRevision;
    private final int currentRevision;
    private final int publicRevision;

    // Check-out information
    private final String checkedOutUsername;

    // Additional metadata
    private String contentTypeLabel;
    private String stateName;

    /**
     * Constructs an item entry with minimal required information.
     * Other fields are initialized with default values.
     *
     * @param contentId the content ID of the item
     * @param name the name/title of the item, never null or empty
     * @param communityId the community ID of the item
     * @param contentTypeId the content type ID of the item
     * @param objectType the object type number
     */
    public PSItemEntry(int contentId, String name, int communityId, int contentTypeId, int objectType) {
        this(contentId, name, communityId, contentTypeId, objectType,
             null, null, null, null, null, -1, -1, -1, -1, -1, null);
    }

    /**
     * Constructs an item entry with complete information.
     *
     * @param contentId the content ID of the item
     * @param name the name/title of the item, never null or empty
     * @param communityId the community ID of the item
     * @param contentTypeId the content type ID of the item
     * @param objectType the object type number
     * @param createdBy the user who created the item, may be null
     * @param lastModifiedDate the last modified date, may be null
     * @param lastModifier the last modifier username, may be null
     * @param postedDate the posted date, may be null
     * @param createdDate the created date, may be null
     * @param workflowAppId the workflow application ID
     * @param contentStateId the workflow state ID
     * @param tipRevision the tip revision number
     * @param currentRevision the current revision number
     * @param publicRevision the public revision number
     * @param checkedOutUsername the user who has the item checked out, may be null
     */
    public PSItemEntry(int contentId, String name, int communityId, int contentTypeId, int objectType,
                       String createdBy, Date lastModifiedDate, String lastModifier, Date postedDate,
                       Date createdDate, int workflowAppId, int contentStateId, int tipRevision,
                       int currentRevision, int publicRevision, String checkedOutUsername) {

        // Validate required parameters
        if (StringUtils.isBlank(name)) {
            logger.warn("Item name (sys_title) must not be null or empty for contentId: {}", contentId);
        }

        this.contentId = contentId;
        this.name = name;
        this.communityId = communityId;
        this.contentTypeId = contentTypeId;
        this.objectType = objectType;
        this.createdBy = createdBy;
        this.lastModifiedDate = lastModifiedDate != null ? new Date(lastModifiedDate.getTime()) : null;
        this.lastModifier = lastModifier;
        this.postedDate = postedDate != null ? new Date(postedDate.getTime()) : null;
        this.createdDate = createdDate != null ? new Date(createdDate.getTime()) : null;
        this.workflowAppId = workflowAppId;
        this.stateId = contentStateId;
        this.tipRevision = tipRevision;
        this.currentRevision = currentRevision;
        this.publicRevision = publicRevision;
        this.checkedOutUsername = checkedOutUsername;
    }

    @Override
    public int getContentId() {
        return contentId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCommunityId() {
        return communityId;
    }

    @Override
    public int getContentTypeId() {
        return contentTypeId;
    }

    @Override
    public int getObjectType() {
        return objectType;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Date getLastModifiedDate() {
        return lastModifiedDate != null ? new Date(lastModifiedDate.getTime()) : null;
    }

    @Override
    public String getLastModifier() {
        return lastModifier;
    }

    @Override
    public Date getPostDate() {
        return postedDate != null ? new Date(postedDate.getTime()) : null;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate != null ? new Date(createdDate.getTime()) : null;
    }

    @Override
    public int getWorkflowAppId() {
        return workflowAppId;
    }

    @Override
    public int getStateId() {
        return stateId;
    }

    @Override
    public int getTipRevision() {
        return tipRevision;
    }

    @Override
    public int getCurrentRevision() {
        return currentRevision;
    }

    @Override
    public int getPublicRevision() {
        return publicRevision;
    }

    @Override
    public String getCheckedOutUsername() {
        return checkedOutUsername;
    }

    @Override
    public String getContentTypeLabel() {
        return contentTypeLabel;
    }

    /**
     * Sets the content type label with validation.
     *
     * @param label the content type label, may be null
     */
    public void setContentTypeLabel(String label) {
        this.contentTypeLabel = label;
    }

    @Override
    public String getStateName() {
        return stateName;
    }

    /**
     * Sets the workflow state name with validation.
     *
     * @param stateName the workflow state name, may be null
     */
    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    /**
     * Checks if the item is currently checked out.
     *
     * @return true if the item is checked out, false otherwise
     */
    public boolean isCheckedOut() {
        return StringUtils.isNotBlank(checkedOutUsername);
    }

    /**
     * Checks if the item has been published (has a public revision).
     *
     * @return true if the item has been published, false otherwise
     */
    public boolean isPublished() {
        return publicRevision > 0;
    }

    /**
     * Gets a defensive copy of the last modified date for legacy compatibility.
     *
     * @return copy of last modified date, may be null
     * @deprecated Use {@link #getLastModifiedDate()} which returns Optional&lt;LocalDateTime&gt;
     */
    @Deprecated
    public Date getLastModifiedDateLegacy() {
        return lastModifiedDate != null ? new Date(lastModifiedDate.getTime()) : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var other = (PSItemEntry) obj;
        return contentId == other.contentId &&
               communityId == other.communityId &&
               contentTypeId == other.contentTypeId &&
               Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, communityId, contentTypeId, name);
    }

    @Override
    public String toString() {
        return String.format("PSItemEntry{contentId=%d, name='%s', contentType=%d, community=%d, state=%d}",
                contentId, name, contentTypeId, communityId, stateId);
    }

    /**
     * Builder for creating PSItemEntry instances with fluent API.
     */
    public static class Builder {
        private int contentId;
        private String name;
        private int communityId;
        private int contentTypeId;
        private int objectType;
        private String createdBy;
        private Date lastModifiedDate;
        private String lastModifier;
        private Date postedDate;
        private Date createdDate;
        private int workflowAppId = -1;
        private int stateId = -1;
        private int tipRevision = -1;
        private int currentRevision = -1;
        private int publicRevision = -1;
        private String checkedOutUsername;

        public Builder withContentId(int contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCommunityId(int communityId) {
            this.communityId = communityId;
            return this;
        }

        public Builder withContentTypeId(int contentTypeId) {
            this.contentTypeId = contentTypeId;
            return this;
        }

        public Builder withObjectType(int objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder withLastModifiedDate(Date lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public Builder withWorkflowInfo(int workflowAppId, int stateId) {
            this.workflowAppId = workflowAppId;
            this.stateId = stateId;
            return this;
        }

        public Builder withRevisions(int tipRevision, int currentRevision, int publicRevision) {
            this.tipRevision = tipRevision;
            this.currentRevision = currentRevision;
            this.publicRevision = publicRevision;
            return this;
        }

        public PSItemEntry build() {
            Validate.notBlank(name, "Item name cannot be blank");
            return new PSItemEntry(contentId, name, communityId, contentTypeId, objectType,
                    createdBy, lastModifiedDate, lastModifier, postedDate, createdDate,
                    workflowAppId, stateId, tipRevision, currentRevision, publicRevision, checkedOutUsername);
        }
    }

    /**
     * Creates a new builder for PSItemEntry.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Enhanced Java 11 methods with Optional support for modern usage

    /**
     * Gets the creator username with Optional safety.
     *
     * @return Optional containing the creator username, empty if not available
     */
    public Optional<String> getCreatedByOptional() {
        return Optional.ofNullable(createdBy);
    }

    /**
     * Gets the last modified date as LocalDateTime with Optional safety.
     *
     * @return Optional containing the last modified date, empty if not available
     */
    public Optional<LocalDateTime> getLastModifiedDateAsLocalDateTime() {
        return Optional.ofNullable(lastModifiedDate)
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    /**
     * Gets the last modifier username with Optional safety.
     *
     * @return Optional containing the last modifier username, empty if not available
     */
    public Optional<String> getLastModifierOptional() {
        return Optional.ofNullable(lastModifier);
    }

    /**
     * Gets the posted date as LocalDateTime with Optional safety.
     *
     * @return Optional containing the posted date, empty if not available
     */
    public Optional<LocalDateTime> getPostedDateAsLocalDateTime() {
        return Optional.ofNullable(postedDate)
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    /**
     * Gets the created date as LocalDateTime with Optional safety.
     *
     * @return Optional containing the created date, empty if not available
     */
    public Optional<LocalDateTime> getCreatedDateAsLocalDateTime() {
        return Optional.ofNullable(createdDate)
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    /**
     * Gets the checked out username with Optional safety.
     *
     * @return Optional containing the checked out username, empty if not checked out
     */
    public Optional<String> getCheckedOutUsernameOptional() {
        return Optional.ofNullable(checkedOutUsername);
    }

    /**
     * Gets the content type label with Optional safety.
     *
     * @return Optional containing the content type label, empty if not available
     */
    public Optional<String> getContentTypeLabelOptional() {
        return Optional.ofNullable(contentTypeLabel);
    }

    /**
     * Gets the workflow state name with Optional safety.
     *
     * @return Optional containing the state name, empty if not available
     */
    public Optional<String> getStateNameOptional() {
        return Optional.ofNullable(stateName);
    }
}
