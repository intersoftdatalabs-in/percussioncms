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

import com.percussion.delivery.comments.data.IPSComment;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the IPSComment interface for use in tests. Represents a comment entity with all
 * its properties.
 */
public class PSComment implements IPSComment {
  private String id;
  private String pagePath;
  private String site;
  private String username;
  private String email;
  private String url;
  private String title;
  private String text;
  private Date createdDate = new Date();
  private boolean viewed;
  private boolean moderated;
  private IPSComment.APPROVAL_STATE approvalState = IPSComment.APPROVAL_STATE.APPROVED;
  private Set<String> tags = new HashSet<>();
  private String commentCreatedDate;
  private String parent;

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getPagePath() {
    return pagePath;
  }

  public void setPagePath(String pagePath) {
    this.pagePath = pagePath;
  }

  @Override
  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public boolean isViewed() {
    return viewed;
  }

  public void setViewed(boolean viewed) {
    this.viewed = viewed;
  }

  @Override
  public boolean isModerated() {
    return moderated;
  }

  public void setModerated(boolean moderated) {
    this.moderated = moderated;
  }

  @Override
  public IPSComment.APPROVAL_STATE getApprovalState() {
    return approvalState;
  }

  public void setApprovalState(IPSComment.APPROVAL_STATE approvalState) {
    this.approvalState = approvalState;
  }

  @Override
  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  @Override
  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  @Override
  public String getCommentCreatedDate() {
    return commentCreatedDate;
  }

  @Override
  public void setCommentCreatedDate(String commentCreatedDate) {
    this.commentCreatedDate = commentCreatedDate;
  }
}
