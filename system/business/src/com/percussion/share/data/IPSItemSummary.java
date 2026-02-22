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
// REFACTORED: CP-JAVA11
package com.percussion.share.data;

import java.util.List;

/**
 * Java 11 refactored: Low-level summary of an item in the system.
 * <p>
 * Implementations must be immutable and thread-safe.
 * <p>
 * <b>Contract:</b> All returned lists must be non-null (may be empty).
 *
 * @author adamgent
 */
public interface IPSItemSummary {
    /**
     * All possible values of the category property.
     */
    enum Category {
        SITE,
        PAGE,
        FOLDER,
        SECTION_FOLDER,
        EXTERNAL_SECTION_FOLDER,
        ASSET,
        RESOURCE,
        UNKNOWN
    }

    /**
     * Gets the item name.
     *
     * @return the item name, never {@code null} or empty
     */
    String getId();

    String getName();

    /**
     * Gets the item category.
     *
     * @return the category, never {@code null}
     */
    Category getCategory();

    /**
     * Gets the content type of the item (eg. "percPage", "Folder", etc).
     *
     * @return the type string, may be {@code null} for incomplete summaries.
     */
    String getType();

    /**
     * All folder paths associated with the item. May be empty but never null.
     */
    List<String> getFolderPaths();

    /**
     * Gets the list of tags for the item.
     *
     * @return a non-null, possibly empty list of tags
     */
    List<String> getTags();

    /**
     * Helper to determine if this summary represents a folder.
     *
     * @return {@code true} when the type is known to be a folder.
     */
    default boolean isFolder() {
        String t = getType();
        return "Folder".equals(t) || "FSFolder".equals(t);
    }

    /**
     * Convenience helper to identify a page item.
     *
     * @return {@code true} when the type is "percPage".
     */
    default boolean isPage() {
        return "percPage".equals(getType());
    }
    /**
     * Sets the item ID. Default implementation does nothing.
     *
     * @param id the item ID
     */
    default void setId(String id) {
        // Default no-op implementation
    }

    /**
     * Sets the item type. Default implementation does nothing.
     *
     * @param type the item type
     */
    default void setType(String type) {
        // Default no-op implementation
    }

    /**
     * Sets the folder paths. Default implementation does nothing.
     *
     * @param folderPaths the list of folder paths
     */
    default void setFolderPaths(List<String> folderPaths) {
        // Default no-op implementation
    }

    /**
     * Sets the item category. Default implementation does nothing.
     *
     * @param category the item category
     */
    default void setCategory(Category category) {
        // Default no-op implementation
    }}
