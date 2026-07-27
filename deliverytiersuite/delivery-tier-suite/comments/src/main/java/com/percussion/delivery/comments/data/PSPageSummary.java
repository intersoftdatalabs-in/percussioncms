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

/**
 * A simple bean class to hold basic page/comment summary info.
 *
 * @author erikserating
 */
public class PSPageSummary {
  private String pagePath;

  private long commentCount;

  private long approvedCount;

  private long newCommentCount;

  /** Default no-arg constructor required by JAXB. */
  public PSPageSummary() {}

  /**
   * Creates a new page summary without a new-comment count.
   *
   * @param pagePath the relative path of the page.
   * @param commentCount the total number of comments on the page.
   * @param approvedCount the number of approved comments on the page.
   */
  public PSPageSummary(String pagePath, long commentCount, long approvedCount) {
    this.pagePath = pagePath;
    this.commentCount = commentCount;
    this.approvedCount = approvedCount;
  }

  /**
   * Creates a new page summary including the new-comment count.
   *
   * @param pagePath the relative path of the page.
   * @param commentCount the total number of comments on the page.
   * @param approvedCount the number of approved comments on the page.
   * @param newCommentCount the number of comments that have not yet been viewed.
   */
  public PSPageSummary(
      String pagePath, long commentCount, long approvedCount, long newCommentCount) {
    this.pagePath = pagePath;
    this.commentCount = commentCount;
    this.approvedCount = approvedCount;
    this.newCommentCount = newCommentCount;
  }

  /**
   * Gets the relative page path.
   *
   * @return the pagePath
   */
  public String getPagePath() {
    return pagePath;
  }

  /**
   * Sets the relative page path.
   *
   * @param pagePath the pagePath to set
   */
  public void setPagePath(String pagePath) {
    this.pagePath = pagePath;
  }

  /**
   * Gets the total comment count for the page.
   *
   * @return the commentCount
   */
  public long getCommentCount() {
    return commentCount;
  }

  /**
   * Sets the total comment count for the page.
   *
   * @param commentCount the commentCount to set
   */
  public void setCommentCount(long commentCount) {
    this.commentCount = commentCount;
  }

  /**
   * Gets the number of approved comments for the page.
   *
   * @return the approvedCount
   */
  public long getApprovedCount() {
    return approvedCount;
  }

  /**
   * Sets the number of approved comments for the page.
   *
   * @param approvedCount the approvedCount to set
   */
  public void setApprovedCount(long approvedCount) {
    this.approvedCount = approvedCount;
  }

  /**
   * Gets the number of unviewed (new) comments for the page.
   *
   * @return the newCommentCount
   */
  public long getNewCommentCount() {
    return newCommentCount;
  }

  /**
   * Sets the number of unviewed (new) comments for the page.
   *
   * @param newCommentCount the newCommentCount to set
   */
  public void setNewCommentCount(long newCommentCount) {
    this.newCommentCount = newCommentCount;
  }
}
