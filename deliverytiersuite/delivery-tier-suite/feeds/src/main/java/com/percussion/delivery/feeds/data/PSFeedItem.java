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
package com.percussion.delivery.feeds.data;

import java.util.Date;
import java.util.Objects;

/**
 * @author erikserating
 */
public class PSFeedItem {
  private String title;
  private String description;
  private Date publishDate;
  private String link;

  public PSFeedItem() {
    super();
  }

  public PSFeedItem(String title, String description, Date publishDate, String link) {
    this.title = Objects.requireNonNull(title, "title cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.publishDate = Objects.requireNonNull(publishDate, "publishDate cannot be null");
    this.link = Objects.requireNonNull(link, "link cannot be null");
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the publishDate
   */
  public Date getPublishDate() {
    return publishDate;
  }

  /**
   * @param publishDate the publishDate to set
   */
  public void setPublishDate(Date publishDate) {
    this.publishDate = publishDate;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return link;
  }

  /**
   * @param link the link to set
   */
  public void setLink(String link) {
    this.link = link;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PSFeedItem that = (PSFeedItem) o;
    return Objects.equals(title, that.title)
        && Objects.equals(description, that.description)
        && Objects.equals(publishDate, that.publishDate)
        && Objects.equals(link, that.link);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, publishDate, link);
  }

  @Override
  public String toString() {
    return "PSFeedItem{"
        + "title='"
        + title
        + '\''
        + ", description='"
        + description
        + '\''
        + ", publishDate="
        + publishDate
        + ", link='"
        + link
        + '\''
        + '}';
  }
}
