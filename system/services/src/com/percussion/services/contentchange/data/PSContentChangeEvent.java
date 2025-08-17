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
package com.percussion.services.contentchange.data;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA entity representing a content change event in the system.
 *
 * <p>This entity tracks changes to content items for incremental publishing
 * and change notification purposes. It uses modern Java 11 features and
 * enhanced validation for improved type safety and data integrity.
 *
 * <p>The entity uses a composite primary key consisting of content ID,
 * change type, and site ID to ensure unique tracking of changes per site.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSContentChangeEvent")
@Table(name = "PSX_CONTENTCHANGEEVENT")
@IdClass(PSContentChangePK.class)
public final class PSContentChangeEvent {

    @Id
    @Column(name = "CONTENTID", nullable = false)
    @Positive(message = "Content ID must be positive")
    private int contentId;

    @Id
    @Column(name = "CHANGE_TYPE", nullable = false)
    @NotBlank(message = "Change type cannot be blank")
    private String changeType;

    @Id
    @Column(name = "SITEID", nullable = false)
    @NotNull(message = "Site ID cannot be null")
    private long siteId;

    /**
     * Default constructor for JPA.
     */
    public PSContentChangeEvent() {
        // JPA requires default constructor
    }

    /**
     * Creates a new content change event with all required fields.
     *
     * @param contentId the content identifier, must be positive
     * @param changeType the type of change, must not be null
     * @param siteId the site identifier
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public PSContentChangeEvent(int contentId, PSContentChangeType changeType, long siteId) {
        setContentId(contentId);
        setChangeType(changeType);
        setSiteId(siteId);
    }

    /**
     * Creates a new content change event with string change type.
     *
     * @param contentId the content identifier, must be positive
     * @param changeType the type of change as string, must not be blank
     * @param siteId the site identifier
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public PSContentChangeEvent(int contentId, String changeType, long siteId) {
        setContentId(contentId);
        setChangeType(changeType);
        setSiteId(siteId);
    }

    /**
     * Gets the content identifier.
     *
     * @return the content ID
     */
    public int getContentId() {
        return contentId;
    }

    /**
     * Sets the content identifier with validation.
     *
     * @param contentId the content ID to set, must be positive
     * @throws IllegalArgumentException if contentId is not positive
     */
    public void setContentId(int contentId) {
        if (contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive: " + contentId);
        }
        this.contentId = contentId;
    }

    /**
     * Gets the change type as an enum.
     *
     * @return the change type enum
     * @throws IllegalArgumentException if the stored change type is invalid
     */
    public PSContentChangeType getChangeType() {
        return PSContentChangeType.valueOf(changeType);
    }

    /**
     * Safely gets the change type as an Optional enum.
     *
     * @return an Optional containing the change type, or empty if invalid
     */
    public Optional<PSContentChangeType> getChangeTypeSafely() {
        try {
            return Optional.of(PSContentChangeType.valueOf(changeType));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the raw change type string.
     *
     * @return the change type string
     */
    public String getChangeTypeString() {
        return changeType;
    }

    /**
     * Sets the change type using the enum.
     *
     * @param changeType the change type to set, must not be null
     * @throws IllegalArgumentException if changeType is null
     */
    public void setChangeType(PSContentChangeType changeType) {
        Objects.requireNonNull(changeType, "Change type cannot be null");
        this.changeType = changeType.name();
    }

    /**
     * Sets the change type using a string with validation.
     *
     * @param changeType the change type string to set, must not be blank
     * @throws IllegalArgumentException if changeType is blank or invalid
     */
    public void setChangeType(String changeType) {
        if (Objects.isNull(changeType) || changeType.trim().isEmpty()) {
            throw new IllegalArgumentException("Change type cannot be null or blank");
        }

        // Validate that it's a valid enum value
        try {
            PSContentChangeType.valueOf(changeType.trim());
            this.changeType = changeType.trim();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid change type: " + changeType, e);
        }
    }

    /**
     * Gets the site identifier.
     *
     * @return the site ID
     */
    public long getSiteId() {
        return siteId;
    }

    /**
     * Sets the site identifier.
     *
     * @param siteId the site ID to set
     */
    public void setSiteId(long siteId) {
        this.siteId = siteId;
    }

    /**
     * Creates a copy of this content change event.
     *
     * @return a new instance with the same values
     */
    public PSContentChangeEvent copy() {
        return new PSContentChangeEvent(contentId, changeType, siteId);
    }

    /**
     * Checks if this change event matches the specified criteria.
     *
     * @param contentId the content ID to match, or -1 to ignore
     * @param changeType the change type to match, or null to ignore
     * @param siteId the site ID to match, or -1 to ignore
     * @return true if all specified criteria match
     */
    public boolean matches(int contentId, PSContentChangeType changeType, long siteId) {
        return (contentId == -1 || this.contentId == contentId) &&
               (changeType == null || this.getChangeType() == changeType) &&
               (siteId == -1 || this.siteId == siteId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var other = (PSContentChangeEvent) obj;
        return contentId == other.contentId &&
               siteId == other.siteId &&
               Objects.equals(changeType, other.changeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, changeType, siteId);
    }

    @Override
    public String toString() {
        return String.format("PSContentChangeEvent{contentId=%d, changeType='%s', siteId=%d}",
                           contentId, changeType, siteId);
    }
}
