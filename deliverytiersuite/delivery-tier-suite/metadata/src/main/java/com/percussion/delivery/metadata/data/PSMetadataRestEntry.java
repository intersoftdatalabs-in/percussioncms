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
package com.percussion.delivery.metadata.data;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Represents a metadata entry in the REST layer. It's used to return exactly what's needed by the
 * REST clients - a flat property map keyed by property name.
 */
public class PSMetadataRestEntry {
  /** Date format used for string-serialized date values such as {@code 2011-01-21T09:36:05}. */
  FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

  private String pagepath;

  private String name;

  private String folder;

  private String linktext;

  private String type;

  private String site;

  private HashMap<String, Object> properties = new HashMap<>();

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataRestEntry() {}

  /**
   * Returns the page path.
   *
   * @return the pagepath, may be <code>null</code>.
   */
  public String getPagepath() {
    return pagepath;
  }

  /**
   * Sets the page path.
   *
   * @param pagepath the pagepath to set; may be <code>null</code>.
   */
  public void setPagepath(String pagepath) {
    this.pagepath = pagepath;
  }

  /**
   * Returns the page name.
   *
   * @return the name, may be <code>null</code>.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the page name.
   *
   * @param name the name to set; may be <code>null</code>.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the folder path.
   *
   * @return the folder, may be <code>null</code>.
   */
  public String getFolder() {
    return folder;
  }

  /**
   * Sets the folder path.
   *
   * @param folder the folder to set; may be <code>null</code>.
   */
  public void setFolder(String folder) {
    this.folder = folder;
  }

  /**
   * Returns the link text.
   *
   * @return the linktext, may be <code>null</code>.
   */
  public String getLinktext() {
    return linktext;
  }

  /**
   * Sets the link text.
   *
   * @param linktext the linktext to set; may be <code>null</code>.
   */
  public void setLinktext(String linktext) {
    this.linktext = linktext;
  }

  /**
   * Returns the content type.
   *
   * @return the type, may be <code>null</code>.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the content type.
   *
   * @param type the type to set; may be <code>null</code>.
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the site name.
   *
   * @return the site, may be <code>null</code>.
   */
  public String getSite() {
    return site;
  }

  /**
   * Sets the site name.
   *
   * @param site the site to set; may be <code>null</code>.
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Returns the properties map. Values are typically strings or lists of strings after {@link
   * #addMetadataProperty(IPSMetadataProperty)} has run.
   *
   * @return the properties map, never <code>null</code>.
   */
  public HashMap<String, Object> getProperties() {
    return properties;
  }

  /**
   * Replaces the entire properties map.
   *
   * @param properties the properties map to set; may not be <code>null</code>.
   */
  public void setProperties(HashMap<String, Object> properties) {
    this.properties = properties;
  }

  /**
   * Adds a PSMetadataProperty to the Map 'properties', so it's converted to String as desired with
   * this format:
   *
   * <pre>
   * {
   *      "propertyName" : "propertyValue"
   * }
   * </pre>
   *
   * @param metadataProperty A PSMetadataProperty instance that will be added to the 'properties'
   *     Map.
   */
  public void addMetadataProperty(IPSMetadataProperty metadataProperty) {
    String newValue = "";
    if (metadataProperty.getValuetype().equals(IPSMetadataProperty.VALUETYPE.NUMBER)) {
      newValue = metadataProperty.getNumbervalue().toString();
    } else if (metadataProperty.getValuetype().equals(IPSMetadataProperty.VALUETYPE.DATE)) {
      newValue = dateFormat.format(metadataProperty.getDatevalue());
    } else {
      newValue = metadataProperty.getStringvalue();
    }
    if (!this.properties.containsKey(metadataProperty.getName())) {
      this.properties.put(metadataProperty.getName(), newValue);
    } else {
      Object value = this.properties.get(metadataProperty.getName());
      if (value instanceof String) {
        List<String> multiValued = new ArrayList<>();
        multiValued.add((String) value);
        multiValued.add(newValue);
        this.properties.put(metadataProperty.getName(), multiValued);
      } else {
        ((List<String>) value).add(newValue);
        this.properties.put(metadataProperty.getName(), value);
      }
    }
  }
}
