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
package com.percussion.services.legacy;

import java.util.Date;

/**
 * Interface for item entry data with Java 11 enhancements.
 * Provides essential information about content items that are cached by the server
 * with Optional-based safe access and modern type safety.
 *
 * @author yubingchen
 */
public interface IPSItemEntry {

    /**
     * Gets the name of the item (sys_title field).
     *
     * @return the name of the item, never null
     */
    String getName();

    /**
     * Gets the content ID of the item.
     *
     * @return the content ID
     */
    int getContentId();

    /**
     * Gets the community ID of the item.
     *
     * @return the community ID
     */
    int getCommunityId();

    /**
     * Gets the content type ID of the item.
     *
     * @return the content type ID
     */
    int getContentTypeId();

    /**
     * Gets the object type.
     *
     * @return the object type
     */
    int getObjectType();

    /**
     * Gets the user who created the item.
     *
     * @return Optional containing the creator username, empty if not available
     */
    String getCreatedBy();

    /**
     * Gets the last modified date of the item.
     *
     * @return Optional containing the last modified date, empty if not available
     */
    Date getLastModifiedDate();

    /**
     * Gets the last modifier of the item.
     *
     * @return Optional containing the last modifier username, empty if not available
     */
    String getLastModifier();

    /**
     * Gets the posted date of the item.
     *
     * @return Optional containing the posted date, empty if not available
     */
    Date getPostDate();

    /**
     * Gets the created date of the item.
     *
     * @return Optional containing the created date, empty if not available
     */
    Date getCreatedDate();

    /**
     * Gets the workflow application ID.
     *
     * @return the workflow app ID
     */
    int getWorkflowAppId();

    /**
     * Gets the workflow state ID.
     *
     * @return the state ID
     */
    int getStateId();

    /**
     * Backwards compatible alias for workflow state ID. Legacy callers expect this
     * signature.
     *
     * @return the content state id
     */
    int getContentStateId();

    /**
     * Whether this entry represents a folder; many legacy APIs call this method.
     * Implementations should return true when appropriate.
     *
     * @return true if a folder
     */
    boolean isFolder();

    /**
     * Gets the tip revision number.
     *
     * @return the tip revision
     */
    int getTipRevision();

    /**
     * Gets the current revision number.
     *
     * @return the current revision
     */
    int getCurrentRevision();

    /**
     * Gets the public revision number.
     *
     * @return the public revision
     */
    int getPublicRevision();

    /**
     * Gets the username of the user who has the item checked out.
     *
     * @return Optional containing the checked out username, empty if not checked out
     */
    String getCheckedOutUsername();

    /**
     * Gets the content type label.
     *
     * @return Optional containing the content type label, empty if not available
     */
    String getContentTypeLabel();

    /**
     * Gets the workflow state name.
     *
     * @return Optional containing the state name, empty if not available
     */
    String getStateName();
}
