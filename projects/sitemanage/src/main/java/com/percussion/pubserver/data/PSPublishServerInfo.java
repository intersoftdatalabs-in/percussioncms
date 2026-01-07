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
package com.percussion.pubserver.data;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents publishing server information for Percussion CMS. Immutable except for properties
 * list, which is defensively copied.
 *
 * @author ignacioerro
 */
@XmlRootElement(name = "serverInfo")
public class PSPublishServerInfo {
  private static final long serialVersionUID = 1L;

  private Long serverId;
  private String serverName;
  private Boolean isDefault;
  private String description;
  private String type;
  private String serverType;
  private List<PSPublishServerProperty> properties = new ArrayList<>();
  private Boolean isModified;
  private Boolean canIncrementalPublish;
  private Boolean isFullPublishRequired;
  private Date lastFullPublishDate;
  private Date lastIncrementalPublishDate;

  /** Returns the server ID. */
  public Long getServerId() {
    return serverId;
  }

  /** Sets the server ID. */
  public void setServerId(Long serverId) {
    this.serverId = serverId;
  }

  /** Returns the server name. */
  public String getServerName() {
    return serverName;
  }

  /** Sets the server name. */
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /** Returns whether this is the default server. */
  public Boolean getIsDefault() {
    return isDefault;
  }

  /** Sets whether this is the default server. */
  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  /** Returns the server description. */
  public String getDescription() {
    return description;
  }

  /** Sets the server description. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** Returns the server type (e.g., File, Database). */
  public String getType() {
    return type;
  }

  /** Sets the server type (e.g., File, Database). */
  public void setType(String type) {
    this.type = type;
  }

  /** Returns the server type (e.g., PRODUCTION, STAGING). */
  public String getServerType() {
    return serverType;
  }

  /** Sets the server type (e.g., PRODUCTION, STAGING). */
  public void setServerType(String serverType) {
    this.serverType = serverType;
  }

  /** Returns an unmodifiable list of server properties. */
  public List<PSPublishServerProperty> getProperties() {
    return Collections.unmodifiableList(properties);
  }

  /** Sets the server properties. Defensive copy is made. */
  public void setProperties(List<PSPublishServerProperty> properties) {
    this.properties = properties == null ? new ArrayList<>() : new ArrayList<>(properties);
  }

  /** Sets whether the server is modified. */
  public void setIsModified(Boolean isModified) {
    this.isModified = isModified;
  }

  /** Returns whether the server is modified. */
  public Boolean getIsModified() {
    return isModified;
  }

  /**
   * Finds a property value by key (case-insensitive).
   *
   * @param key the property key
   * @return the property value, or null if not found
   */
  public String findProperty(String key) {
    if (key == null) {
      return null;
    }
    return properties.stream()
        .filter(p -> key.equalsIgnoreCase(p.getKey()))
        .map(PSPublishServerProperty::getValue)
        .findFirst()
        .orElse(null);
  }

  /** Returns whether incremental publishing is supported. */
  public Boolean getCanIncrementalPublish() {
    return canIncrementalPublish;
  }

  /** Sets whether incremental publishing is supported. */
  public void setCanIncrementalPublish(Boolean canIncrementalPublish) {
    this.canIncrementalPublish = canIncrementalPublish;
  }

  /** Returns whether a full publish is required. */
  public Boolean getIsFullPublishRequired() {
    return isFullPublishRequired;
  }

  /** Sets whether a full publish is required. */
  public void setIsFullPublishRequired(Boolean isFullPublishRequired) {
    this.isFullPublishRequired = isFullPublishRequired;
  }

  /** Returns the date of the last full publish (defensive copy). */
  public Date getLastFullPublishDate() {
    return lastFullPublishDate == null ? null : new Date(lastFullPublishDate.getTime());
  }

  /** Sets the date of the last full publish (defensive copy). */
  public void setLastFullPublishDate(Date lastFullPublishDate) {
    this.lastFullPublishDate =
        lastFullPublishDate == null ? null : new Date(lastFullPublishDate.getTime());
  }

  /** Returns the date of the last incremental publish (defensive copy). */
  public Date getLastIncrementalPublishDate() {
    return lastIncrementalPublishDate == null
        ? null
        : new Date(lastIncrementalPublishDate.getTime());
  }

  /** Sets the date of the last incremental publish (defensive copy). */
  public void setLastIncrementalPublishDate(Date lastIncrementalPublishDate) {
    this.lastIncrementalPublishDate =
        lastIncrementalPublishDate == null ? null : new Date(lastIncrementalPublishDate.getTime());
  }
}
