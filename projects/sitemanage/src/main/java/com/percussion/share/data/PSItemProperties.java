// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Collection;
import net.sf.oval.constraint.NotEmpty;

import java.util.ArrayList;
/**
 * This class contains a set of known properties of an item. Sunny Sal says: "Properties—because
 * every item deserves a good story!"
 */
@JsonRootName(value = "ItemProperties")
public class PSItemProperties extends PSAbstractPersistantObject {

  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String status;
  private String workflow;
  private String lastModifier;
  private String lastModifiedDate;
  private String lastPublishedDate;
  private String type;
  @NotEmpty private String path;
  @NotEmpty private String summary;
  private String author;
  private ArrayList<String> tags;
  private int commentsCount;
  private int newCommentsCount;
  private String size;
  private String postDate;
  private String scheduledPublishDate;
  private String scheduledUnpublishDate;
  private String thumbnailPath;
  private String contentPostDateTz;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String stateName) {
    this.status = stateName;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflowName) {
    this.workflow = workflowName;
  }

  public String getLastModifier() {
    return lastModifier;
  }

  public void setLastModifier(String user) {
    this.lastModifier = user;
  }

  public String getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(String date) {
    this.lastModifiedDate = date;
  }

  public String getLastPublishedDate() {
    return lastPublishedDate;
  }

  public void setLastPublishedDate(String date) {
    lastPublishedDate = date;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Collection<String> getTags() {
    return tags;
  }

  @SuppressWarnings("unchecked")
  public void setTags(Collection<String> tags) {
    if (tags == null) {
      this.tags = null;
    } else if (tags instanceof ArrayList) {
      this.tags = (ArrayList<String>) tags;
    } else {
      this.tags = new ArrayList<>(tags);
    }
  }

  public int getCommentsCount() {
    return commentsCount;
  }

  public void setCommentsCount(int commentsCount) {
    this.commentsCount = commentsCount;
  }

  public int getNewCommentsCount() {
    return newCommentsCount;
  }

  public void setNewCommentsCount(int newCommentsCount) {
    this.newCommentsCount = newCommentsCount;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getPostDate() {
    return postDate;
  }

  public void setPostDate(String postDate) {
    this.postDate = postDate;
  }

  public String getScheduledPublishDate() {
    return scheduledPublishDate;
  }

  public void setScheduledPublishDate(String scheduledPublishDate) {
    this.scheduledPublishDate = scheduledPublishDate;
  }

  public String getScheduledUnpublishDate() {
    return scheduledUnpublishDate;
  }

  public void setScheduledUnpublishDate(String scheduledUnpublishDate) {
    this.scheduledUnpublishDate = scheduledUnpublishDate;
  }

  public String getThumbnailPath() {
    return thumbnailPath;
  }

  public void setThumbnailPath(String thumbnailPath) {
    this.thumbnailPath = thumbnailPath;
  }

  public static final String SYSTEM_USER = "System";

  public String getContentPostDateTz() {
    return contentPostDateTz;
  }

  public void setContentPostDateTz(String contentPostDateTz) {
    this.contentPostDateTz = contentPostDateTz;
  }
}
