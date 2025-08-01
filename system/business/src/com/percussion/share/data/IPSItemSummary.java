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
        ASSET,
        RESOURCE,
        UNKNOWN
    }

    /**
     * Gets the item name.
     *
     * @return the item name, never {@code null} or empty
     */
    String getName();

    /**
     * Gets the item category.
     *
     * @return the category, never {@code null}
     */
    Category getCategory();

    /**
     * Gets the list of tags for the item.
     *
     * @return a non-null, possibly empty list of tags
     */
    List<String> getTags();
}
