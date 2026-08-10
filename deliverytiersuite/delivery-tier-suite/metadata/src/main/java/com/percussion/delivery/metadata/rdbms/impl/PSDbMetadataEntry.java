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
package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;

/**
 * Represents metadata for a published page on the delivery server.
 *
 * <p>Not {@link java.io.Serializable}: JPA-managed entity exchanged as a DTO/graph, not via Java
 * serialization (avoids serial warnings on non-serializable collection field types).
 *
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSMetadataEntry")
@Table(
    name = "PERC_PAGE_METADATA",
    indexes = {@Index(name = "typeIndex", columnList = "type")})
public final class PSDbMetadataEntry implements IPSMetadataEntry {

  /** Hash of {@link #pagepath}; used as the database primary key. */
  @Id
  @Column(length = 40)
  @Nationalized
  private String pagepathHash;

  // This column may be marked as unique, but keep in mind that unique
  // keys greater than 767 characters are not supported on MySQL.
  /** Published site-relative page path of the indexed entry. */
  @Column(length = 2000)
  @Nationalized
  private String pagepath;

  /** Page name (last path segment) of the indexed entry. */
  @Basic @Nationalized private String name;

  /** Folder path that contains the page (without the site prefix). */
  @Column(length = 2000)
  @Nationalized
  private String folder;

  /** Link text associated with the page. */
  @Basic @Nationalized private String linktext;

  /** Lower-case copy of {@link #linktext} used for case-insensitive lookups. */
  @Basic @Nationalized private String linktext_lower;

  /** Content type of the page (e.g. {@code page}, {@code asset}). */
  @Basic @Nationalized private String type;

  /** Site name the page belongs to. */
  @Basic @Nationalized private String site;

  /** Properties attached to this entry, eagerly fetched and cascade-deleted with the entry. */
  @OnDelete(action = OnDeleteAction.CASCADE)
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = "entry",
      targetEntity = PSDbMetadataProperty.class)
  private Set<PSDbMetadataProperty> properties = new HashSet<>();

  /** HashCalculator instance used to get the hash of the metadata entry's pagepath. */
  private static PSHashCalculator hashCalculator = new PSHashCalculator();

  /** No-arg constructor required by Hibernate. */
  public PSDbMetadataEntry() {}

  /**
   * Constructs a fully populated metadata entry.
   *
   * @param name the file name; cannot be <code>null</code> or empty.
   * @param folder the folder path of the containing folder without the site folder. Cannot be
   *     <code>null</code> or empty.
   * @param pagepath the path of the file including the site folder. This is used as a unique key
   *     for the entry. Cannot be <code>null</code> or empty.
   * @param type the content type of the page; cannot be <code>null</code>.
   * @param site the site this page belongs to; cannot be <code>null</code> or empty.
   */
  public PSDbMetadataEntry(String name, String folder, String pagepath, String type, String site) {
    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("name cannot be null or empty");
    if (folder == null || folder.length() == 0)
      throw new IllegalArgumentException("folder cannot be null or empty");
    if (pagepath == null || pagepath.length() == 0)
      throw new IllegalArgumentException("pagepath cannot be null or empty");
    if (site == null || site.length() == 0)
      throw new IllegalArgumentException("site cannot be null or empty");

    // Direct assignment + private pagepathHash update; class is final (no this-escape).
    this.name = name;
    this.folder = folder;
    this.type = type == null ? "" : type;
    this.site = site;
    this.pagepath = pagepath;
    this.pagepathHash = hashCalculator.calculateHash(pagepath);
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the folder
   */
  public String getFolder() {
    return folder;
  }

  /**
   * @param folder the folder to set
   */
  public void setFolder(String folder) {
    this.folder = folder;
  }

  /**
   * Returns the hash of this entry's pagepath. Used as the database primary key so equality is
   * case-sensitive and independent of length-based pagepath differences.
   *
   * @return the pagepathHash, never <code>null</code> after the entry has been persisted.
   */
  public String getPagepathHash() {
    return pagepathHash;
  }

  /**
   * Returns the page path associated with this entry.
   *
   * @return the page path, may be <code>null</code>.
   */
  public String getPagepath() {
    return pagepath;
  }

  /**
   * @param path the pagepath to set
   */
  public void setPagepath(String path) {
    this.pagepath = path;

    if (this.pagepath == null) pagepathHash = hashCalculator.calculateHash(StringUtils.EMPTY);
    else pagepathHash = hashCalculator.calculateHash(this.pagepath);
  }

  /**
   * @return the linktext
   */
  public String getLinktext() {
    return linktext;
  }

  /**
   * @param linktext the linktext to set
   */
  public void setLinktext(String linktext) {
    this.linktext = linktext == null ? "" : linktext;
    this.linktext_lower = this.linktext.toLowerCase();
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type == null ? "" : type;
  }

  /**
   * @return the site
   */
  public String getSite() {
    return site;
  }

  /**
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Returns a defensive copy of the properties attached to this entry.
   *
   * @return a cloned set of the properties, never <code>null</code>, may be empty.
   */
  public Set<IPSMetadataProperty> getProperties() {
    if (properties == null) return null;
    Set<IPSMetadataProperty> results = new HashSet<>(properties.size());
    for (IPSMetadataProperty p : properties) results.add(p);
    return results;
  }

  /**
   * Replaces the properties attached to this entry with the supplied set.
   *
   * @param properties the properties to set; may be <code>null</code>.
   */
  public void setProperties(Set<IPSMetadataProperty> properties) {
    if (properties == null) this.properties = null;
    Set<PSDbMetadataProperty> dbprops = new HashSet<>();
    for (IPSMetadataProperty p : properties) {
      if (p instanceof PSDbMetadataProperty) {
        dbprops.add((PSDbMetadataProperty) p);
      } else {
        dbprops.add(new PSDbMetadataProperty(p.getName(), p.getValuetype(), p.getValue()));
      }
    }

    this.properties = dbprops;
  }

  /** Clears all the properties attached to this entry. */
  public void clearProperties() {
    if (properties != null) properties.clear();
  }

  /**
   * Adds a single property to the entry.
   *
   * @param prop the property to add; may be <code>null</code>.
   */
  public void addProperty(IPSMetadataProperty prop) {
    ((PSDbMetadataProperty) prop).setMetadataEntry(this);
    this.properties.add((PSDbMetadataProperty) prop);
  }

  /**
   * Helper method to return number of properties.
   *
   * @return number of properties.
   */
  public int getPropertyCount() {
    if (properties == null) return 0;
    return properties.size();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !getClass().getName().equals(obj.getClass().getName())) return false;
    PSDbMetadataEntry entry = (PSDbMetadataEntry) obj;
    return new EqualsBuilder()
        .append(folder, entry.folder)
        .append(linktext, entry.linktext)
        .append(name, entry.name)
        .append(pagepath, entry.pagepath)
        .append(site, entry.site)
        .append(type, entry.type)
        .isEquals();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(folder)
        .append(linktext)
        .append(name)
        .append(pagepath)
        .append(site)
        .append(type)
        .toHashCode();
  }
}
