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

package com.percussion.delivery.comments.bean;

/** Summary of comments for a specific page. */
public class PSPageSummary {
  private String pagePath;
  private Integer commentCount;
  private Integer approvedCount;
  private Integer newCommentCount;

  public String getPagePath() {
    return pagePath;
  }

  public void setPagePath(String pagePath) {
    this.pagePath = pagePath;
  }

  public Integer getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(Integer commentCount) {
    this.commentCount = commentCount;
  }

  public Integer getApprovedCount() {
    return approvedCount;
  }

  public void setApprovedCount(Integer approvedCount) {
    this.approvedCount = approvedCount;
  }

  public Integer getNewCommentCount() {
    return newCommentCount;
  }

  public void setNewCommentCount(Integer newCommentCount) {
    this.newCommentCount = newCommentCount;
  }
}
