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
package com.percussion.services.aaclient;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Node type definitions for the nodes used in the Active Assembly (AA) widgets.
 * Each node type has an associated ordinal value and icon URL for UI representation.
 *
 * @author Percussion Software
 */
public enum PSWidgetNodeType {

    /** AA parent page node type */
    WIDGET_NODE_TYPE_PAGE(0, "/Rhythmyx/sys_resources/images/page.gif"),

    /** AA slot node type */
    WIDGET_NODE_TYPE_SLOT(1, "/Rhythmyx/sys_resources/images/slot.gif"),

    /** AA snippet node type */
    WIDGET_NODE_TYPE_SNIPPET(2, "/Rhythmyx/sys_resources/images/snippet.gif"),

    /** Content Editor field node type */
    WIDGET_NODE_TYPE_FIELD(3, "/Rhythmyx/sys_resources/images/field.gif");

    /** Maximum allowed ordinal value */
    private static final int MAX_ORDINAL = Short.MAX_VALUE;

    /** Ordinal value for this node type */
    private final short ordinal;

    /** Icon URL for UI representation */
    private final String iconUrl;

    /**
     * Constructs a new widget node type with the specified ordinal and icon URL.
     *
     * @param ordinal the ordinal value for this node type, must be non-negative and within short range
     * @param iconUrl the icon URL for UI representation, must not be null
     * @throws IllegalArgumentException if ordinal is negative or exceeds maximum value, or if iconUrl is null
     */
    PSWidgetNodeType(int ordinal, String iconUrl) {
        if (ordinal < 0 || ordinal > MAX_ORDINAL) {
            throw new IllegalArgumentException(
                "Ordinal must be between 0 and " + MAX_ORDINAL + ", got: " + ordinal);
        }
        this.ordinal = (short) ordinal;
        this.iconUrl = Objects.requireNonNull(iconUrl, "Icon URL cannot be null");
    }

    /**
     * Gets the ordinal value for this node type.
     *
     * @return the ordinal value as a short
     */
    public short getOrdinal() {
        return ordinal;
    }

    /**
     * Gets the icon URL for this node type.
     *
     * @return the icon URL, never null
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Creates a PSWidgetNodeType from the given ordinal value.
     *
     * @param ordinal the ordinal value to convert
     * @return Optional containing the matching PSWidgetNodeType, or empty if no match found
     */
    public static Optional<PSWidgetNodeType> fromOrdinal(short ordinal) {
        return Arrays.stream(values())
            .filter(type -> type.ordinal == ordinal)
            .findFirst();
    }

    /**
     * Creates a PSWidgetNodeType from the given ordinal value.
     *
     * @param ordinal the ordinal value to convert
     * @return the matching PSWidgetNodeType
     * @throws IllegalArgumentException if no matching node type exists
     * @deprecated Use {@link #fromOrdinal(short)} instead for better error handling
     */
    @Deprecated
    public static PSWidgetNodeType valueOf(int ordinal) {
        return fromOrdinal((short) ordinal)
            .orElseThrow(() -> new IllegalArgumentException("No widget node type with ordinal: " + ordinal));
    }

    /**
     * Gets a human-readable description of this node type.
     *
     * @return description string, never null
     */
    public String getDescription() {
        switch (this) {
            case WIDGET_NODE_TYPE_PAGE:
                return "Active Assembly Page";
            case WIDGET_NODE_TYPE_SLOT:
                return "Active Assembly Slot";
            case WIDGET_NODE_TYPE_SNIPPET:
                return "Active Assembly Snippet";
            case WIDGET_NODE_TYPE_FIELD:
                return "Content Editor Field";
            default:
                throw new IllegalStateException("Unknown widget node type: " + this);
        }
    }

    @Override
    public String toString() {
        return String.format("%s(ordinal=%d, iconUrl='%s')", name(), ordinal, iconUrl);
    }
}
