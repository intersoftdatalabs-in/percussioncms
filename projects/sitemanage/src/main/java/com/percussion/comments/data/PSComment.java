/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.comments.data;

import static com.percussion.share.dao.PSDateUtils.getDateFromString;
import static com.percussion.share.dao.PSDateUtils.getDateToString;

import com.percussion.itemmanagement.data.IPSEditableItem;
import com.percussion.share.data.PSAbstractDataObject;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import java.util.HashSet;
/**
 * Represents a comment in Percussion CMS. Provides all comment metadata and supports XML
 * serialization.
 */
@XmlRootElement(name = "comments")
public class PSComment extends PSAbstractDataObject implements IPSEditableItem {

  private static final long serialVersionUID = -6525483335618861315L;

  private String id;
  private String commentId;
  private String commentTitle;
  private String commentText;
  private Date commentCreateDate;
  private String commentApprovalState;
  private Boolean commentModerated;
  private Boolean commentViewed;
  private HashSet<String> commentTags;
  private Integer commentParentId;
  private String siteName;
  private String pagePath;
  private HashSet<String> pageTags;
  private String userName;
  private String userLinkUrl;
  private String userEmail;

  @XmlElement(name = "_id")
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @XmlElement(name = "id")
  public String getCommentId() {
    return commentId;
  }

  public void setCommentId(String commentId) {
    this.commentId = commentId;
  }

  @XmlElement(name = "title")
  public String getCommentTitle() {
    return commentTitle;
  }

  public void setCommentTitle(String commentTitle) {
    this.commentTitle = commentTitle;
  }

  @XmlElement(name = "text")
  public String getCommentText() {
    return commentText;
  }

  public void setCommentText(String commentText) {
    this.commentText = commentText;
  }

  @XmlElement(name = "createdDate")
  public String getCommentCreateDate() {
    return getDateToString(this.commentCreateDate);
  }

  public void setCommentCreateDate(String commentCreateDate) throws DataServiceLoadException {
    try {
      this.commentCreateDate = getDateFromString(commentCreateDate);
    } catch (ParseException e) {
      throw new DataServiceLoadException(
          "Error parsing date in setCommentCreateDate(String commentCreateDate)"
              + " in com.percussion.comments.data.PSComment",
          e);
    }
  }

  public void setCommentCreateDate(Date commentCreateDate) {
    this.commentCreateDate = commentCreateDate;
  }

  @XmlElement(name = "approvalState")
  public String getCommentApprovalState() {
    return commentApprovalState;
  }

  public void setCommentApprovalState(String commentApprovalState) {
    this.commentApprovalState = commentApprovalState;
  }

  @XmlElement(name = "moderated")
  public Boolean getCommentModerated() {
    return commentModerated;
  }

  public void setCommentModerated(Boolean commentModerated) {
    this.commentModerated = commentModerated;
  }

  @XmlElement(name = "viewed")
  public Boolean getCommentViewed() {
    return commentViewed;
  }

  public void setCommentViewed(Boolean commentViewed) {
    this.commentViewed = commentViewed;
  }

  @XmlElement(name = "commentTags")
  public Set<String> getCommentTags() {
    return commentTags;
  }

  @SuppressWarnings("unchecked")
  public void setCommentTags(Set<String> commentTags) {
    if (commentTags == null) {
      this.commentTags = null;
    } else if (commentTags instanceof HashSet) {
      this.commentTags = (HashSet<String>) commentTags;
    } else {
      this.commentTags = new HashSet<>(commentTags);
    }
  }

  @XmlElement(name = "site")
  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  @XmlElement(name = "pagePath")
  public String getPagePath() {
    return pagePath;
  }

  public void setPagePath(String pagePath) {
    this.pagePath = pagePath;
  }

  @XmlElement(name = "tags")
  public Set<String> getPageTags() {
    return pageTags;
  }

  @SuppressWarnings("unchecked")
  public void setPageTags(Set<String> pageTags) {
    if (pageTags == null) {
      this.pageTags = null;
    } else if (pageTags instanceof HashSet) {
      this.pageTags = (HashSet<String>) pageTags;
    } else {
      this.pageTags = new HashSet<>(pageTags);
    }
  }

  @XmlElement(name = "parent")
  public Integer getCommentParentId() {
    return commentParentId;
  }

  public void setCommentParentId(Integer commentParentId) {
    this.commentParentId = commentParentId;
  }

  @XmlElement(name = "username")
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @XmlElement(name = "url")
  public String getUserLinkUrl() {
    return userLinkUrl;
  }

  public void setUserLinkUrl(String userLinkUrl) {
    this.userLinkUrl = userLinkUrl;
  }

  @XmlElement(name = "email")
  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  @Override
  public String getType() {
    // TODO: ASSET_TYPE is likely incorrect. Determine the correct type.
    return IPSEditableItem.ASSET_TYPE;
  }
}
