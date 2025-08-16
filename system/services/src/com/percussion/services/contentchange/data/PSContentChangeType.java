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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of content change types for incremental publishing and change tracking.
 *
 * <p>This enum provides modern Java 11 capabilities for content change type management:
 * <ul>
 *   <li>Stream-based operations for bulk processing</li>
 *   <li>Optional-based safe lookups</li>
 *   <li>Enhanced validation and error handling</li>
 *   <li>Functional programming support</li>
 * </ul>
 *
 * <p>The enum represents the different states of content changes that can occur
 * during the publishing workflow, enabling fine-grained tracking of content
 * modifications for incremental publishing scenarios.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */
public enum PSContentChangeType {

    /**
     * Content changes that are pending to go live (production).
     *
     * <p>This type represents content modifications that have been made
     * but are waiting to be published to the live/production environment.
     */
    PENDING_LIVE("pending-live", "Changes pending for live publication"),

    /**
     * Content changes that are pending to be staged.
     *
     * <p>This type represents content modifications that are queued
     * for staging environment deployment before final publication.
     */
    PENDING_STAGED("pending-staged", "Changes pending for staging publication");

    private final String displayName;
    private final String description;

    /**
     * Cached lookup map for efficient string-to-enum conversion.
     */
    private static final Map<String, PSContentChangeType> NAME_LOOKUP = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(
            type -> type.name().toLowerCase(),
            Function.identity()
        ));

    /**
     * Cached lookup map for display name to enum conversion.
     */
    private static final Map<String, PSContentChangeType> DISPLAY_NAME_LOOKUP = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(
            PSContentChangeType::getDisplayName,
            Function.identity()
        ));

    /**
     * Creates a new content change type with display information.
     *
     * @param displayName the human-readable display name
     * @param description the detailed description
     */
    PSContentChangeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this change type.
     *
     * @return the display name, never null
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the detailed description of this change type.
     *
     * @return the description, never null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this change type is related to live publishing.
     *
     * @return true if this type involves live publishing
     */
    public boolean isLivePublishing() {
        return this == PENDING_LIVE;
    }

    /**
     * Checks if this change type is related to staged publishing.
     *
     * @return true if this type involves staged publishing
     */
    public boolean isStagedPublishing() {
        return this == PENDING_STAGED;
    }

    /**
     * Safely converts a string to a PSContentChangeType enum value.
     *
     * <p>This method performs case-insensitive lookup and returns an Optional
     * to handle invalid input gracefully.
     *
     * @param name the string name to convert, may be null
     * @return an Optional containing the enum value, or empty if not found
     */
    public static Optional<PSContentChangeType> fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(NAME_LOOKUP.get(name.trim().toLowerCase()));
    }

    /**
     * Safely converts a display name to a PSContentChangeType enum value.
     *
     * @param displayName the display name to convert, may be null
     * @return an Optional containing the enum value, or empty if not found
     */
    public static Optional<PSContentChangeType> fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(DISPLAY_NAME_LOOKUP.get(displayName.trim()));
    }

    /**
     * Gets all content change types as an immutable set.
     *
     * @return an immutable set of all enum values
     */
    public static Set<PSContentChangeType> getAllTypes() {
        return Set.of(values());
    }

    /**
     * Gets all content change types related to live publishing.
     *
     * @return a set of live publishing related types
     */
    public static Set<PSContentChangeType> getLivePublishingTypes() {
        return Arrays.stream(values())
            .filter(PSContentChangeType::isLivePublishing)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets all content change types related to staged publishing.
     *
     * @return a set of staged publishing related types
     */
    public static Set<PSContentChangeType> getStagedPublishingTypes() {
        return Arrays.stream(values())
            .filter(PSContentChangeType::isStagedPublishing)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Validates that a string represents a valid content change type.
     *
     * @param name the string to validate, may be null
     * @return true if the string represents a valid enum value
     */
    public static boolean isValidType(String name) {
        return fromString(name).isPresent();
    }

    /**
     * Converts a string to a PSContentChangeType, throwing an exception if invalid.
     *
     * @param name the string name to convert, must not be null or blank
     * @return the corresponding enum value
     * @throws IllegalArgumentException if the name is invalid
     */
    public static PSContentChangeType requireValidType(String name) {
        return fromString(name)
            .orElseThrow(() -> new IllegalArgumentException("Invalid content change type: " + name));
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, name());
    }
}
