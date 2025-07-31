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

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSPageInfo;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * DAO interface for comment persistence in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public interface IPSCommentsDao {

    /**
     * Finds comments matching the given criteria.
     *
     * @param criteria comment search criteria
     * @return list of matching comments
     * @throws Exception if query fails
     */
    List<IPSComment> find(PSCommentCriteria criteria) throws Exception;

    /**
     * Finds pages with comments for the given site.
     *
     * @param site site name
     * @return list of page info
     * @throws Exception if query fails
     */
    List<PSPageInfo> findPagesWithComments(String site) throws Exception;

    /**
     * Finds sites for the given comment IDs.
     *
     * @param ids comment IDs
     * @return set of site names
     * @throws Exception if query fails
     */
    Set<String> findSitesForCommentIds(Collection<String> ids) throws Exception;

    /**
     * Finds the default moderation state for a site.
     *
     * @param site site name
     * @return approval state
     * @throws Exception if query fails
     */
    APPROVAL_STATE findDefaultModerationState(String site) throws Exception;

    /**
     * Saves a comment.
     *
     * @param comment comment to save
     * @throws Exception if save fails
     */
    void save(IPSComment comment) throws Exception;

    /**
     * Saves the default moderation state for a site.
     *
     * @param sitename site name
     * @param state approval state
     * @throws Exception if save fails
     */
    void saveDefaultModerationState(String sitename, APPROVAL_STATE state) throws Exception;

    /**
     * Deletes comments by IDs.
     *
     * @param commentIds comment IDs
     * @throws Exception if delete fails
     */
    void delete(Collection<String> commentIds) throws Exception;

    /**
     * Moderates comments by IDs.
     *
     * @param commentIds comment IDs
     * @param newApprovalState new approval state
     * @throws Exception if update fails
     */
    void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState) throws Exception;
}
