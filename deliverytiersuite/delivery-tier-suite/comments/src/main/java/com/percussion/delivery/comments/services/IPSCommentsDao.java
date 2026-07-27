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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSPageInfo;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Data access interface for comments.
 *
 * @author erikserating
 */
public interface IPSCommentsDao {

  /**
   * Finds comments matching the supplied criteria.
   *
   * @param criteria the criteria to filter by, must not be {@code null}.
   * @return list of matching comments, never {@code null}, may be empty.
   * @throws Exception if an error occurs during the lookup.
   */
  List<IPSComment> find(PSCommentCriteria criteria) throws Exception;

  /**
   * Finds page summaries for all pages that have comments for the specified site.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @return list of page info, never {@code null}, may be empty.
   * @throws Exception if an error occurs during the lookup.
   */
  List<PSPageInfo> findPagesWithComments(String site) throws Exception;

  /**
   * Finds the distinct set of sites that own the specified comment ids.
   *
   * @param ids the comment ids to look up, must not be {@code null}.
   * @return set of site names, never {@code null}, may be empty.
   * @throws Exception if an error occurs during the lookup.
   */
  Set<String> findSitesForCommentIds(Collection<String> ids) throws Exception;

  /**
   * Finds the default moderation state configured for the specified site.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @return the default approval state for the site, never {@code null}.
   * @throws Exception if an error occurs during the lookup.
   */
  APPROVAL_STATE findDefaultModerationState(String site) throws Exception;

  /**
   * Persists the supplied comment.
   *
   * @param comment the comment to save, must not be {@code null}.
   * @throws Exception if an error occurs during the save.
   */
  void save(IPSComment comment) throws Exception;

  /**
   * Persists the default moderation state for the specified site.
   *
   * @param sitename the site name, must not be {@code null} or empty.
   * @param state the default approval state, must not be {@code null}.
   * @throws Exception if an error occurs during the save.
   */
  void saveDefaultModerationState(String sitename, APPROVAL_STATE state) throws Exception;

  /**
   * Deletes the comments with the specified ids.
   *
   * @param commentIds the comment ids to delete, must not be {@code null}.
   * @throws Exception if an error occurs during deletion.
   */
  void delete(Collection<String> commentIds) throws Exception;

  /**
   * Updates the approval state of the specified comments.
   *
   * @param commentIds the comment ids to moderate, must not be {@code null}.
   * @param newApprovalState the new approval state to apply, must not be {@code null}.
   * @throws Exception if an error occurs during the update.
   */
  void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState) throws Exception;
}
