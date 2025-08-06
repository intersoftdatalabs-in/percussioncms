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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Criteria for deleting a folder via REST.
 * Contains the relative path of the folder to delete, a flag for skipping in-use assets/resources,
 * and a flag for purging or recycling the folder.
 *
 * @author peterfrontiero
 */
@XmlRootElement(name = "DeleteFolderCriteria")
@JsonRootName("DeleteFolderCriteria")
public class PSDeleteFolderCriteria {

    /**
     * The path of the folder to delete. Never null or empty.
     */
    @NotNull
    @NotBlank
    private String path;

    /**
     * Whether to skip in-use assets/resources during deletion.
     */
    private SkipItemsType skipItems;

    /**
     * Whether the folder should be purged (true) or recycled (false).
     */
    private boolean shouldPurge;

    /**
     * Optional GUID for the folder.
     */
    private String guid;

    public String getPath() {
        return path;
    }

    /**
     * Sets the path of the folder to delete.
     *
     * @param path the folder path, not null or empty
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the skip items type.
     *
     * @return the skip items type, may be null
     */
    public SkipItemsType getSkipItems() {
        return skipItems;
    }

    /**
     * Sets the skip items type.
     *
     * @param skipItems the skip items type
     */
    public void setSkipItems(SkipItemsType skipItems) {
        this.skipItems = skipItems;
    }

    /**
     * Sets whether the folder should be purged.
     *
     * @param shouldPurge true to purge, false to recycle
     */
    public void setShouldPurge(boolean shouldPurge) {
        this.shouldPurge = shouldPurge;
    }

    /**
     * @return true if the folder should be purged, false if recycled
     */
    public boolean getShouldPurge() {
        return this.shouldPurge;
    }

    /**
     * Gets the GUID for the folder.
     *
     * @return the GUID, may be null
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Sets the GUID for the folder.
     *
     * @param guid the GUID
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    /**
     * The type used to determine if in-use folder items should be skipped during deletion.
     */
    public enum SkipItemsType {
        /**
         * Skip in-use folder items.
         */
        YES,
        /**
         * Delete in-use folder items.
         */
        NO,
        /**
         * Skip in-use folder items and return their paths.
         */
        EMPTY
    }
}
