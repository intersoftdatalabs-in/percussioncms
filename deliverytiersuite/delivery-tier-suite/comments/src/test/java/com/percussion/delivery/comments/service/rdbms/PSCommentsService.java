/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.dao.IPSCommentsDao;
// Import only one IPSComment - use fully qualified names where needed to avoid ambiguity
import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.comments.services.IPSCommentsService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the IPSCommentsService interface for RDBMS storage. Used for testing purposes.
 *
 * @since 8.0
 */
public class PSCommentsService implements IPSCommentsService {

  private static final Logger LOGGER = LogManager.getLogger(PSCommentsService.class);
  private final IPSCommentsDao commentsDao;

  /**
   * Creates a new PSCommentsService.
   *
   * @param commentsDao The DAO used for persisting and retrieving comments
   */
  public PSCommentsService(IPSCommentsDao commentsDao) {
    this.commentsDao = Objects.requireNonNull(commentsDao, "Comments DAO cannot be null");
  }

  @Override
  public IPSComment addComment(IPSComment comment) throws Exception {
    // For test implementation, just return the comment as is
    // In a real implementation, this would call the DAO
    return comment;
  }

  /**
   * Adds tags to a comment.
   *
   * @param commentId The ID of the comment to add tags to
   * @param tags The set of tags to add
   * @return true if the tags were added successfully, false otherwise
   */
  @Override
  public void addCommentTags(Long id, Set<String> tags) {
    if (id == null) {
      throw new IllegalArgumentException("Comment ID cannot be null");
    }
    if (tags == null || tags.isEmpty()) {
      return; // Nothing to do
    }

    try {
      // Implementation would typically call a DAO method
      // For this test stub, just log
      LOGGER.debug("Adding tags {} to comment {}", tags, id);
    } catch (Exception e) {
      LOGGER.error("Failed to add tags to comment {}: {}", id, e.getMessage());
    }
  }

  @Override
  public PSComments getComments(PSCommentCriteria criteria, boolean isModerator) throws Exception {
    // For test implementation, just return an empty PSComments object
    // In a real implementation, this would call the DAO
    return new PSComments();
  }

  @Override
  public PSPageSummaries getPagesWithComments(String site, int maxResults, int startIndex)
      throws Exception {
    if (site == null || site.isEmpty()) {
      throw new IllegalArgumentException("Site cannot be null or empty");
    }
    // For test implementation, just return an empty PSPageSummaries object
    // In a real implementation, this would call the DAO
    return new PSPageSummaries(new ArrayList<>());
  }

  /**
   * Approves the comments with the given IDs.
   *
   * @param commentIds the IDs of the comments to approve
   */
  @Override
  public void approveComments(Collection<String> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("Comment IDs collection cannot be null");
    }
    if (!ids.isEmpty()) {
      commentsDao.approveComments(new ArrayList<>(ids));
    }
  }

  /**
   * Rejects the comments with the given IDs.
   *
   * @param commentIds the IDs of the comments to reject
   */
  @Override
  public void rejectComments(Collection<String> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("Comment IDs collection cannot be null");
    }
    if (!ids.isEmpty()) {
      commentsDao.rejectComments(new ArrayList<>(ids));
    }
  }

  /**
   * Deletes the comments with the given IDs.
   *
   * @param commentIds the IDs of the comments to delete
   */
  @Override
  public void deleteComments(Collection<String> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("Comment IDs collection cannot be null");
    }
    if (!ids.isEmpty()) {
      commentsDao.deleteComments(new ArrayList<>(ids));
    }
  }

  /**
   * Gets the default moderation state for comments on the specified site.
   *
   * @param site the site to get the default moderation state for
   * @return the default moderation state
   */
  @Override
  public IPSComment.APPROVAL_STATE getDefaultModerationState(String sitename) {
    // For test implementation, just return APPROVED
    // In a real implementation, this would call the DAO
    return IPSComment.APPROVAL_STATE.APPROVED;
  }

  /**
   * Sets the default moderation state for comments on the specified site.
   *
   * @param site the site to set the default moderation state for
   * @param state the default moderation state
   */
  @Override
  public void setDefaultModerationState(String sitename, IPSComment.APPROVAL_STATE dflt) {
    if (sitename == null || sitename.isEmpty()) {
      throw new IllegalArgumentException("Site cannot be null or empty");
    }
    if (dflt == null) {
      throw new IllegalArgumentException("State cannot be null");
    }
    // For test implementation, do nothing
    // In a real implementation, this would call the DAO
  }

  @Override
  public boolean updateCommentsForRenameSite(String prevSiteName, String newSiteName) {
    if (prevSiteName == null
        || newSiteName == null
        || prevSiteName.isEmpty()
        || newSiteName.isEmpty()) {
      throw new IllegalArgumentException("Site names cannot be null or empty");
    }
    try {
      // For now, just delegate to DAO if available or return false
      // In a real implementation, this would perform the site rename operation
      return false;
    } catch (Exception e) {
      LOGGER.error("Failed to update comments for renamed site", e);
      return false;
    }
  }

  @Override
  public List<String> getTags(int maxResults, int startIndex) {
    // This is a test implementation that returns an empty list
    // In a real implementation, this would call the DAO to retrieve tags
    return new ArrayList<>();
  }

  @Override
  public PSComments getComments(PSCommentCriteria criteria) throws Exception {
    // Delegate to the existing method with isModerator = false as default
    return getComments(criteria, false);
  }
}
