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
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.data.FeedType;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * JPA entity backing a row in the {@code PERC_FEED_DESCRIPTORS} table that stores a single feed
 * descriptor for a particular site.
 *
 * @author erikserating
 */
@Entity
@Table(name = "PERC_FEED_DESCRIPTORS")
public class PSFeedDescriptor implements IPSFeedDescriptor, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 2756156009184830398L;

  /** Site portion of the composite key, may be <code>null</code> before persistence. */
  @Id
  @Column(length = 255)
  private String site;

  /** Feed name portion of the composite key, may be <code>null</code> before persistence. */
  @Id
  @Column(length = 255)
  private String name;

  /** Feed title, may be <code>null</code>. */
  @Basic
  @Column(length = 2000)
  private String title;

  /** Feed description, may be <code>null</code>. */
  @Basic
  @Column(length = 4000)
  private String description;

  /** Link to the page the feed represents, may be <code>null</code>. */
  @Basic
  @Column(length = 2000)
  private String link;

  /** Feed output type, may be <code>null</code>. */
  @Basic
  @Column(length = 2000)
  private String type;

  /** Query used to retrieve the feed data from the metadata service, may be <code>null</code>. */
  @Basic
  @Column(length = 4000)
  private String query;

  /** Default no-arg constructor required by JPA. */
  public PSFeedDescriptor() {}

  /**
   * Copies the values from the supplied descriptor into a new JPA entity.
   *
   * @param descriptor the descriptor to copy, never <code>null</code>
   */
  public PSFeedDescriptor(IPSFeedDescriptor descriptor) {
    this.name = descriptor.getName();
    this.site = descriptor.getSite();
    this.title = descriptor.getTitle();
    this.description = descriptor.getDescription();
    this.link = descriptor.getLink();
    this.type = descriptor.getType();
    this.query = descriptor.getQuery();
  }

  /**
   * Gets the feed description.
   *
   * @return the feed description, may be <code>null</code>
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the feed type as an enum.
   *
   * @return the parsed {@link FeedType}, never <code>null</code>
   */
  public FeedType getFeedType() {
    return FeedType.valueOf(type);
  }

  /**
   * Gets the link to the page the feed represents.
   *
   * @return the link, may be <code>null</code>
   */
  public String getLink() {
    return link;
  }

  /**
   * Gets the feed name.
   *
   * @return the feed name, may be <code>null</code>
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the query used to retrieve the feed data from the metadata service.
   *
   * @return the query, may be <code>null</code>
   */
  public String getQuery() {
    return query;
  }

  /**
   * Gets the name of the site this feed belongs to.
   *
   * @return the site name, may be <code>null</code>
   */
  public String getSite() {
    return site;
  }

  /**
   * Gets the feed title.
   *
   * @return the title, may be <code>null</code>
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the feed output type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the feed output type.
   *
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Sets the name of the site this feed belongs to.
   *
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Sets the feed name.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the feed title.
   *
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the feed description.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Sets the link to the page the feed represents.
   *
   * @param link the link to set
   */
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * Sets the query used to retrieve the feed data from the metadata service.
   *
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((site == null) ? 0 : site.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PSFeedDescriptor other = (PSFeedDescriptor) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (site == null) {
      return other.site == null;
    } else {
      return site.equals(other.site);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PSFeedDescriptor [");
    if (site != null) {
      builder.append("site=");
      builder.append(site);
      builder.append(", ");
    }
    if (name != null) {
      builder.append("name=");
      builder.append(name);
      builder.append(", ");
    }
    if (title != null) {
      builder.append("title=");
      builder.append(title);
      builder.append(", ");
    }
    if (description != null) {
      builder.append("description=");
      builder.append(description);
      builder.append(", ");
    }
    if (link != null) {
      builder.append("link=");
      builder.append(link);
      builder.append(", ");
    }
    if (type != null) {
      builder.append("type=");
      builder.append(type);
      builder.append(", ");
    }
    if (query != null) {
      builder.append("query=");
      builder.append(query);
    }
    builder.append("]");
    return builder.toString();
  }
}
