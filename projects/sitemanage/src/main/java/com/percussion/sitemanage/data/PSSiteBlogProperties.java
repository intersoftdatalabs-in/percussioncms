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
package com.percussion.sitemanage.data;

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/** This class contains information for a blog of a site. */
@XmlRootElement(name = "SiteBlogProperties")
public class PSSiteBlogProperties extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private String id;
  private String pageId;
  private String blogPostTemplateId;
  private String title;
  private String description;
  private int blogPostcount;
  private String lastPublishDate;
  private String path;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBlogPostTemplateId() {
    return blogPostTemplateId;
  }

  public void setBlogPostTemplateId(String blogPostTemplateId) {
    this.blogPostTemplateId = blogPostTemplateId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getBlogPostcount() {
    return blogPostcount;
  }

  public void setBlogPostcount(int blogPostcount) {
    this.blogPostcount = blogPostcount;
  }

  public Optional<String> getLastPublishDate() {
    return Optional.ofNullable(lastPublishDate);
  }

  public void setLastPublishDate(String lastPublishDate) {
    this.lastPublishDate = lastPublishDate;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }
}
