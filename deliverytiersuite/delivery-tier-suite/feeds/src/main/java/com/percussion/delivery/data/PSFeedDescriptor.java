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

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import java.util.Objects;

/** A transfer object impl of the IPSFeedDescriptor interface. */
public class PSFeedDescriptor implements IPSFeedDescriptor {

  private String name;
  private String site;
  private String description;
  private String link;
  private String title;
  private String query;

  private String type;

  /** Default no-arg constructor required by Jackson serialization. */
  public PSFeedDescriptor() {
    super();
  }

  /**
   * Constructs a fully-populated feed descriptor.
   *
   * @param name the feed name, never <code>null</code>
   * @param site the name of the site the feed belongs to, never <code>null</code>
   * @param description the feed description, never <code>null</code>
   * @param link the link to the page the feed represents, never <code>null</code>
   * @param title the feed title, never <code>null</code>
   * @param query the query used to get the feed data from the meta-data service, never <code>null
   *     </code>
   * @param type the feed output type, never <code>null</code>
   */
  public PSFeedDescriptor(
      String name,
      String site,
      String description,
      String link,
      String title,
      String query,
      String type) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.site = Objects.requireNonNull(site, "site cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.link = Objects.requireNonNull(link, "link cannot be null");
    this.title = Objects.requireNonNull(title, "title cannot be null");
    this.query = Objects.requireNonNull(query, "query cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  /**
   * Gets the feed description.
   *
   * @return the feed description, never <code>null</code>
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the link to the page the feed represents.
   *
   * @return the link to the page the feed represents, never <code>null</code>
   */
  public String getLink() {
    return link;
  }

  /**
   * Gets the feed name.
   *
   * @return the feed name, never <code>null</code>
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the query used to get the feed data from the meta-data service.
   *
   * @return the query used to get the feed data from the meta-data service, never <code>null
   *     </code>
   */
  public String getQuery() {
    return query;
  }

  /**
   * Gets the name of the site the feed belongs to.
   *
   * @return the name of the site the feed belongs to, never <code>null</code>
   */
  public String getSite() {
    return site;
  }

  /**
   * Gets the feed title.
   *
   * @return the feed title, never <code>null</code>
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
   * Sets the feed name.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the name of the site the feed belongs to.
   *
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
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
   * Sets the feed title.
   *
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the query used to get the feed data from the meta-data service.
   *
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PSFeedDescriptor that = (PSFeedDescriptor) obj;
    return Objects.equals(name, that.name)
        && Objects.equals(site, that.site)
        && Objects.equals(description, that.description)
        && Objects.equals(link, that.link)
        && Objects.equals(title, that.title)
        && Objects.equals(query, that.query)
        && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, site, description, link, title, query, type);
  }

  /**
   * Convenience constructor that builds a descriptor with no feed type set.
   *
   * @param name the feed name, never <code>null</code>
   * @param site the name of the site the feed belongs to, never <code>null</code>
   * @param description the feed description, never <code>null</code>
   * @param link the link to the page the feed represents, never <code>null</code>
   * @param title the feed title, never <code>null</code>
   * @param query the query used to get the feed data from the meta-data service, never <code>null
   *     </code>
   */
  public PSFeedDescriptor(
      String name, String site, String description, String link, String title, String query) {
    this(name, site, description, link, title, query, null);
  }

  @Override
  public String toString() {
    return "PSFeedDescriptor{"
        + "name='"
        + name
        + '\''
        + ", site='"
        + site
        + '\''
        + ", description='"
        + description
        + '\''
        + ", link='"
        + link
        + '\''
        + ", title='"
        + title
        + '\''
        + ", query='"
        + query
        + '\''
        + ", type='"
        + type
        + '\''
        + '}';
  }
}
