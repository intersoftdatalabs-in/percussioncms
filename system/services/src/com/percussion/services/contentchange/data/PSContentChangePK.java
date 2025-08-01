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
package com.percussion.services.contentchange.data;

import javax.persistence.Column;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the content change service entities.
 *
 * <p>This class enables quick lookups and Hibernate caching of content change entries
 * by providing a compound primary key consisting of content ID, site ID, and change type.
 *
 * <p>Implements proper equals/hashCode semantics required for JPA composite keys
 * using modern Java 11 practices and enhanced validation.
 *
 * @author stephenbolton
 * @since Java 11 Modernization
 */
@IdClass(PSContentChangePK.class)
public final class PSContentChangePK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "CONTENTID")
    private int contentId;

    @Column(name = "CHANGE_TYPE")
    private String changeType;

    @Column(name = "SITEID")
    private long siteId;

    /**
     * Default constructor required by JPA.
     */
    public PSContentChangePK() {
        // JPA requires default constructor
    }

    /**
     * Creates a new composite primary key with all components.
     *
     * @param contentId the content identifier, must be positive
     * @param siteId the site identifier
     * @param changeType the change type string, must not be blank
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public PSContentChangePK(int contentId, long siteId, String changeType) {
        setContentId(contentId);
        setSiteId(siteId);
        setChangeType(changeType);
    }

    /**
     * Creates a new composite primary key using enum change type.
     *
     * @param contentId the content identifier, must be positive
     * @param siteId the site identifier
     * @param changeType the change type enum, must not be null
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public PSContentChangePK(int contentId, long siteId, PSContentChangeType changeType) {
        this(contentId, siteId, Objects.requireNonNull(changeType, "Change type cannot be null").name());
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
     * Gets the change type string.
     *
     * @return the change type
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * Sets the change type with validation.
     *
     * @param changeType the change type to set, must not be blank
     * @throws IllegalArgumentException if changeType is blank
     */
    public void setChangeType(String changeType) {
        if (Objects.isNull(changeType) || changeType.trim().isEmpty()) {
            throw new IllegalArgumentException("Change type cannot be null or blank");
        }
        this.changeType = changeType.trim();
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
     * Creates a copy of this primary key.
     *
     * @return a new instance with the same values
     */
    public PSContentChangePK copy() {
        return new PSContentChangePK(contentId, siteId, changeType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var other = (PSContentChangePK) obj;
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
        return String.format("PSContentChangePK{contentId=%d, changeType='%s', siteId=%d}",
                           contentId, changeType, siteId);
    }
}
