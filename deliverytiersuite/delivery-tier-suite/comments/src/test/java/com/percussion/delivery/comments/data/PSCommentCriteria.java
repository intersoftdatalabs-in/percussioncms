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

package com.percussion.delivery.comments.data;

/** Data package criteria for filtering comments in queries. */
public class PSCommentCriteria {
  private String pagepath;
  private String site;
  private String username;
  private String tag;
  private Boolean viewed;
  private Boolean moderated;
  private IPSComment.APPROVAL_STATE state;
  private int maxResults;
  private int startIndex;
  private String lastCommentId;
  private PSCommentSort sort;

  public String getPagepath() {
    return pagepath;
  }

  public void setPagepath(String pagepath) {
    this.pagepath = pagepath;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public Boolean getViewed() {
    return viewed;
  }

  public Boolean isViewed() {
    return viewed;
  }

  public void setViewed(Boolean viewed) {
    this.viewed = viewed;
  }

  public Boolean getModerated() {
    return moderated;
  }

  public void setModerated(Boolean moderated) {
    this.moderated = moderated;
  }

  public Boolean isModerated() {
    return moderated;
  }

  public IPSComment.APPROVAL_STATE getState() {
    return state;
  }

  public void setState(IPSComment.APPROVAL_STATE state) {
    this.state = state;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public String getLastCommentId() {
    return lastCommentId;
  }

  public void setLastCommentId(String lastCommentId) {
    this.lastCommentId = lastCommentId;
  }

  public PSCommentSort getSort() {
    return sort;
  }

  public void setSort(PSCommentSort sort) {
    this.sort = sort;
  }
}
