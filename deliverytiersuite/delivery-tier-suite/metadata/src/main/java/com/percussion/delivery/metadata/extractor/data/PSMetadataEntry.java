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

package com.percussion.delivery.metadata.extractor.data;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents metadata for a published page on the delivery server.
 *
 * <p>Not {@link java.io.Serializable}: exchanged as REST/JAXB DTOs and indexer payloads, not via
 * Java serialization (avoids serial warnings on collection field types).
 *
 * @author miltonpividori
 */
@XmlType(propOrder = {"name"})
public class PSMetadataEntry implements IPSMetadataEntry {

  /** Published site-relative page path of the indexed entry. */
  private String pagepath;

  /** Page name (last path segment) of the indexed entry. */
  private String name;

  /** Folder path that contains the page (without the site prefix). */
  private String folder;

  /** Link text associated with the page. */
  private String linktext;

  /** Content type of the page. */
  private String type;

  /** Site name the page belongs to. */
  private String site;

  /** Properties attached to this entry. */
  @XmlElementWrapper(name = "property")
  @XmlElement(type = PSMetadataProperty.class)
  private Set<IPSMetadataProperty> properties = new HashSet<>();

  /** No-arg constructor required by JAXB. */
  public PSMetadataEntry() {}

  /**
   * Constructs a fully populated metadata entry.
   *
   * @param name the file name; cannot be <code>null</code> or empty.
   * @param folder the folder path of the containing folder without the site folder. Cannot be
   *     <code>null</code> or empty.
   * @param pagepath the path of the file including the site folder. Used as a unique key for the
   *     entry. Cannot be <code>null</code> or empty.
   * @param type the content type of the page; may not be <code>null</code>.
   * @param site the site this page belongs to; cannot be <code>null</code> or empty.
   */
  public PSMetadataEntry(String name, String folder, String pagepath, String type, String site) {
    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("name cannot be null or empty");
    if (folder == null || folder.length() == 0)
      throw new IllegalArgumentException("folder cannot be null or empty");
    if (pagepath == null || pagepath.length() == 0)
      throw new IllegalArgumentException("pagepath cannot be null or empty");
    if (site == null || site.length() == 0)
      throw new IllegalArgumentException("site cannot be null or empty");
    this.name = name;
    this.folder = folder;
    this.type = type;
    this.pagepath = pagepath;
    this.site = site;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getName()
   */
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setName(java.lang.String)
   */
  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getFolder()
   */
  public String getFolder() {
    return folder;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setFolder(java.lang.String)
   */
  public void setFolder(String folder) {
    this.folder = folder;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getPagepath()
   */
  public String getPagepath() {
    return pagepath;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setPagepath(java.lang.String)
   */
  public void setPagepath(String path) {
    this.pagepath = path;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getLinktext()
   */
  public String getLinktext() {
    return linktext;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setLinktext(java.lang.String)
   */
  public void setLinktext(String linktext) {
    this.linktext = linktext;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getType()
   */
  public String getType() {
    return type;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getSite()
   */
  public String getSite() {
    return site;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setSite(java.lang.String)
   */
  public void setSite(String site) {
    this.site = site;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#getProperties()
   */
  public Set<IPSMetadataProperty> getProperties() {
    if (properties == null) {
      properties = new HashSet<>();
    }
    return properties;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataEntry#setProperties(java.util.Set)
   */
  public void setProperties(Set<IPSMetadataProperty> properties) {
    Set<IPSMetadataProperty> convertedProperties = new HashSet<>();
    if (properties != null) {
      for (IPSMetadataProperty property : properties) {
        if (property instanceof PSMetadataProperty) {
          convertedProperties.add(property);
        } else {
          convertedProperties.add(
              new PSMetadataProperty(
                  property.getName(), property.getValuetype(), property.getValue()));
        }
      }
    }
    this.properties = convertedProperties;
  }

  public void addProperty(IPSMetadataProperty prop) {
    if (properties == null) properties = new HashSet<>();
    if (prop instanceof PSMetadataProperty) {
      properties.add(prop);
    } else {
      properties.add(new PSMetadataProperty(prop.getName(), prop.getValuetype(), prop.getValue()));
    }
  }

  public void clearProperties() {
    if (properties != null) properties.clear();
  }
}
