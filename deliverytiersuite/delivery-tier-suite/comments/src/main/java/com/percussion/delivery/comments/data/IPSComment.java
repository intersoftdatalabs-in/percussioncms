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
package com.percussion.delivery.comments.data;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.percussion.delivery.services.PSCustomDateSerializer;

/**
 * Interface defining the core functionality for comments in the system.
 * All implementing classes should ensure thread-safety.
 */
public interface IPSComment {
    /**
     * @return the id for this comment, assigned by the persistence layer.
     */
    String getId();

    /**
     * @return the id of the parent comment for threading support
     */
    Optional<String> getParent();

    /**
     * @return the comment text, never empty
     */
    String getText();

    /**
     * @return the optional comment title
     */
    Optional<String> getTitle();

    /**
     * @return the sitename of the site the comment is in
     */
    String getSite();

    /**
     * @return the page path, relative path to the page excluding site
     */
    String getPagePath();

    /**
     * @return the optional username of the comment author
     */
    Optional<String> getUsername();

    /**
     * @return the optional URL provided by the comment author
     */
    Optional<String> getUrl();

    /**
     * @return the optional email of the comment author
     */
    Optional<String> getEmail();

    /**
     * @return the creation timestamp of this comment, never null
     */
    @JsonSerialize(using = PSCustomDateSerializer.class)
    Date getCreatedDate();

    /**
     * @return unmodifiable set of tags for this comment, never null
     */
    Set<String> getTags();

    /**
     * @return the current approval state, defaults to APPROVAL_STATE.PENDING
     */
    APPROVAL_STATE getApprovalState();

    /**
     * @return true if the comment was moderated by user action
     */
    boolean isModerated();
}
