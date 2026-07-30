// REFACTORED: CP-JAVA11
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import java.util.Set;

/**
 * Represents a comment stored by the delivery tier comment service.
 *
 * @author erikserating
 */
public interface IPSComment {
  /**
   * Gets the unique id assigned to this comment by the persistence layer.
   *
   * @return the id for this comment, this is assigned by the persistence layer.
   */
  String getId();

  /**
   * Gets the id of the parent comment.
   *
   * @return the id of the parent comment. Used for comment threading.
   */
  String getParent();

  /**
   * Gets the body text of the comment.
   *
   * @return the comment text, never <code>null</code>, may be empty.
   */
  String getText();

  /**
   * Gets the title of the comment.
   *
   * @return the comment title, may be <code>null</code> or empty.
   */
  String getTitle();

  /**
   * Gets the site this comment belongs to.
   *
   * @return the sitename of the site the comment is in.
   */
  String getSite();

  /**
   * Gets the relative path of the page the comment is on.
   *
   * @return the page path, the relative path to the page that this comment is on, not including the
   *     site. Never <code>null</code> or empty.
   */
  String getPagePath();

  /**
   * Gets the user name of the comment author.
   *
   * @return the user name of the person who wrote the comment. May be <code>null</code> or empty.
   */
  String getUsername();

  /**
   * Gets the URL entered by the comment author.
   *
   * @return the url that the comment author entered. May be <code>null</code> or empty.
   */
  String getUrl();

  /**
   * Gets the email of the comment author.
   *
   * @return the email for the user who wrote the comment. May be <code>null</code> or empty.
   */
  String getEmail();

  /**
   * Gets the date this comment was created.
   *
   * @return the created date for this comment. Never <code>null</code> or empty.
   */
  @JsonSerialize(using = com.fasterxml.jackson.databind.JsonSerializer.class)
  Date getCreatedDate();

  /**
   * Gets the unique tags assigned to this comment.
   *
   * @return set of all unique tag strings for this comment. Never <code>null</code>, may be empty.
   */
  Set<String> getTags();

  /**
   * Gets the current approval state of this comment.
   *
   * @return the current approval state for this comment. Never <code>null</code>. Defaults to
   *     <code>APPROVAL_STATE.PENDING</code>.
   */
  APPROVAL_STATE getApprovalState();

  /**
   * Flag indicating that this comment has been moderated. This should only be <code>true</code> if
   * this comment was put into a state by a user action and not programmatically.
   *
   * @return <code>true</code> if the the comment was moderated.
   */
  boolean isModerated();

  /**
   * Flag indicating that this comment was viewed once by a CM1 user and is no longer considered a
   * new comment.
   *
   * @return <code>true</code> if this comment was viewed.
   */
  boolean isViewed();

  /**
   * Set the viewed flag to indicate the comment has been viewed once by a moderator.
   *
   * @param viewed the new viewed flag value.
   */
  void setViewed(boolean viewed);

  /**
   * Sets the date this comment was created.
   *
   * @param createdDate the createdDate to set
   */
  void setCreatedDate(Date createdDate);

  /**
   * Sets the unique id for this comment.
   *
   * @param id the id to set
   */
  void setId(String id);

  /**
   * Sets the relative path of the page this comment is on.
   *
   * @param pagePath the pagePath to set
   */
  void setPagePath(String pagePath);

  /**
   * Sets the email of the comment author.
   *
   * @param email the email to set
   */
  void setEmail(String email);

  /**
   * Sets the user name of the comment author.
   *
   * @param username the username to set
   */
  void setUsername(String username);

  /**
   * Sets the body text of this comment.
   *
   * @param text the text to set
   */
  void setText(String text);

  /**
   * Sets the id of the parent comment.
   *
   * @param parent the parent to set
   */
  void setParent(String parent);

  /**
   * Sets the approval state of this comment.
   *
   * @param approvalState the approvalState to set
   */
  void setApprovalState(APPROVAL_STATE approvalState);

  /**
   * Sets the moderated flag of this comment.
   *
   * @param moderated the moderated to set
   */
  void setModerated(boolean moderated);

  /**
   * Sets the site this comment belongs to.
   *
   * @param site the site to set
   */
  void setSite(String site);

  /**
   * Sets the URL entered by the comment author.
   *
   * @param url the url to set
   */
  void setUrl(String url);

  /**
   * Sets the title of this comment.
   *
   * @param title the title to set
   */
  void setTitle(String title);

  /**
   * Gets the comment created date as a string. Used for legacy clients that expect a string value.
   *
   * @return the comment created date as a string.
   */
  String getCommentCreatedDate();

  /**
   * Sets the comment created date from a string. Used for legacy clients that supply a string.
   *
   * @param commentCreatedDate the comment created date as a string.
   */
  void setCommentCreatedDate(String commentCreatedDate);

  /**
   * Utility method to safely convert APPROVAL_STATE to String. Returns "PENDING" if the state is
   * null.
   *
   * @param state the approval state
   * @return the string representation or "PENDING" if null
   */
  static String approvalStateToString(final APPROVAL_STATE state) {
    return state != null ? state.toString() : "PENDING";
  }

  /** Comment approval states. */
  public enum APPROVAL_STATE {
    /** The comment has been approved by a moderator. */
    APPROVED,
    /** The comment has been rejected by a moderator. */
    REJECTED
  }
}
