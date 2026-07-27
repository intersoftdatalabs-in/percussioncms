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
 * Lightweight value object that summarizes a single page that has comments. Used in REST responses
 * listing pages with comments.
 */
public class PSPageInfo {

  private String pagePath;
  private String approvalState;
  private long commentCount;
  private boolean viewed;

  /**
   * Creates a new page summary.
   *
   * @param pagePath the relative page path, must not be {@code null}.
   * @param approvalState the approval state value as a string, may be {@code null}.
   * @param commentCount the total number of comments on the page.
   * @param viewed whether the page has been viewed by an admin.
   */
  public PSPageInfo(String pagePath, String approvalState, long commentCount, boolean viewed) {
    this.pagePath = pagePath;
    this.approvalState = approvalState;
    this.commentCount = commentCount;
    this.viewed = viewed;
  }

  /**
   * Gets the relative page path.
   *
   * @return the page path.
   */
  public String getPagePath() {
    return pagePath;
  }

  /**
   * Gets the approval state of the page's comments.
   *
   * @return the approval state.
   */
  public String getApprovalState() {
    return approvalState;
  }

  /**
   * Gets the number of comments on the page.
   *
   * @return the comment count.
   */
  public long getCommentCount() {
    return commentCount;
  }

  /**
   * Checks whether an admin has viewed this page's comments.
   *
   * @return {@code true} if the page has been viewed.
   */
  public boolean isViewed() {
    return viewed;
  }
}
