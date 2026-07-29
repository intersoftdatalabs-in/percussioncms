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
package com.percussion.delivery.data;

import java.util.Date;

/**
 * Transfer object representing a single feed item (entry) within an RSS/Atom feed.
 *
 * @author erikserating
 */
public class PSFeedItem {
  private String title;
  private String description;
  private Date publishDate;
  private String link;

  /** Default no-arg constructor required by Jackson serialization. */
  public PSFeedItem() {
    super();
  }

  /**
   * Gets the title of the feed item.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the feed item.
   *
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the description of the feed item.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the feed item.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the publish date of the feed item.
   *
   * @return the publishDate
   */
  public Date getPublishDate() {
    return publishDate;
  }

  /**
   * Sets the publish date of the feed item.
   *
   * @param publishDate the publishDate to set
   */
  public void setPublishDate(Date publishDate) {
    this.publishDate = publishDate;
  }

  /**
   * Gets the link associated with the feed item.
   *
   * @return the link
   */
  public String getLink() {
    return link;
  }

  /**
   * Sets the link associated with the feed item.
   *
   * @param link the link to set
   */
  public void setLink(String link) {
    this.link = link;
  }
}
