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
package com.percussion.delivery.comments.services;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;

/**
 * Service interface for managing comments in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public interface IPSCommentsService {

    /**
     * Retrieves comments based on criteria.
     *
     * @param criteria comment search criteria, must not be null
     * @param isModerator if true, marks returned comments as viewed by moderator
     * @return matching comments, never null
     */
    PSComments getComments(PSCommentCriteria criteria, boolean isModerator);

    /**
     * Retrieves page summaries for all pages with comments.
     *
     * @param site site name to filter by, must not be blank
     * @param maxResults maximum results to return (0 for unlimited)
     * @param startIndex starting index for pagination (0 for first page)
     * @return page summaries, never null
     */
    PSPageSummaries getPagesWithComments(String site, int maxResults, int startIndex);

    /**
     * Gets a list of unique tags across all comments.
     *
     * @param maxResults maximum results to return (0 for unlimited)
     * @param startIndex starting index for pagination (0 for first page)
     * @return immutable list of tags, never null
     */
    List<String> getTags(int maxResults, int startIndex);

    /**
     * Adds a new comment to the system.
     *
     * @param comment comment to add, must not be null
     * @return saved comment with generated ID
     */
    IPSComment addComment(IPSComment comment);

    /**
     * Adds tags to an existing comment.
     *
     * @param commentId comment ID
     * @param tags tags to add, must not be null
     */
    void addCommentTags(String commentId, Set<String> tags);

    /**
     * Gets a specific comment by ID.
     *
     * @param id comment ID to find
     * @return the comment if found
     */
    Optional<IPSComment> getComment(String id);

    /**
     * Gets summaries of pages matching the criteria.
     *
     * @param criteria search criteria, must not be null
     * @return page summaries, never null
     */
    PSPageSummaries getPageSummaries(PSCommentCriteria criteria);

    /**
     * Gets the default moderation state for a site.
     *
     * @param site site name to check
     * @return default moderation state
     */
    APPROVAL_STATE getDefaultModerationState(String site);

    /**
     * Sets the default moderation state for a site.
     *
     * @param site site name to update
     * @param state new default state
     */
    void setDefaultModerationState(String site, APPROVAL_STATE state);

    /**
     * Updates all comments when a site is renamed.
     *
     * @param oldSiteName current site name
     * @param newSiteName new site name
     * @return true if all comments were updated successfully
     */
    boolean updateCommentsForRenameSite(String oldSiteName, String newSiteName);

    /**
     * Adds a listener for comment data changes.
     *
     * @param listener listener to add, must not be null
     */
    void addListener(IPSServiceDataChangeListener listener);

    /**
     * Removes a data change listener.
     *
     * @param listener listener to remove, must not be null
     */
    void removeListener(IPSServiceDataChangeListener listener);

    /**
     * Finds comments matching the given criteria.
     *
     * @param criteria search criteria, must not be null
     * @return immutable list of matching comments
     */
    List<IPSComment> findComments(PSCommentCriteria criteria);
}
