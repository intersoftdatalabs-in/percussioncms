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
package com.percussion.widgetbuilder.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.services.widgetbuilder.PSWidgetBuilderDefinition;
import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Lightweight version of {@link PSWidgetBuilderDefinitionData} for serialization of summary data
 * only.
 */
@XmlRootElement(name = "WidgetBuilderSummaryData")
@JsonRootName("WidgetBuilderSummaryData")
public class PSWidgetBuilderSummaryData extends PSAbstractPersistantObject {

  private static final long serialVersionUID = 1L;

  private long widgetId;
  private String prefix;
  private String author;
  private String label;
  private String publisherUrl;
  private String description;
  private String version;
  private String toolTipMessage;
  private String widgetTrayCustomizedIconPath;
  private boolean responsive;

  public PSWidgetBuilderSummaryData() {
    super();
  }

  /**
   * Copy constructor.
   *
   * @param src The summary to copy from, not {@code null}.
   */
  public PSWidgetBuilderSummaryData(PSWidgetBuilderSummaryData src) {
    super();
    if (src == null) {
      throw new IllegalArgumentException("src must not be null");
    }
    author = src.author;
    description = src.description;
    label = src.label;
    prefix = src.prefix;
    publisherUrl = src.publisherUrl;
    version = src.version;
    widgetId = src.widgetId;
    responsive = src.responsive;
    toolTipMessage = src.toolTipMessage;
    widgetTrayCustomizedIconPath = src.widgetTrayCustomizedIconPath;
  }

  /**
   * Create from DAO object.
   *
   * @param dao The DAO object to copy from, not {@code null}.
   */
  public PSWidgetBuilderSummaryData(PSWidgetBuilderDefinition dao) {
    super();
    if (dao == null) {
      throw new IllegalArgumentException("dao must not be null");
    }
    // Direct field seeds — avoid overridable setters during construction (this-escape).
    this.author = dao.getAuthor().orElse(null);
    this.description = dao.getDescription().orElse(null);
    this.label = dao.getLabel().orElse(null);
    this.prefix = dao.getPrefix().orElse(null);
    this.publisherUrl = dao.getPublisherUrl().orElse(null);
    this.version = dao.getVersion().orElse(null);
    this.widgetId = dao.getWidgetBuilderDefinitionId();
    this.responsive = dao.isResponsive();
    this.widgetTrayCustomizedIconPath = dao.getWidgetTrayCustomizedIconPath().orElse(null);
    this.toolTipMessage = dao.getToolTipMessage().orElse(null);
  }

  public String getToolTipMessage() {
    return toolTipMessage;
  }

  public void setToolTipMessage(String toolTipMessage) {
    this.toolTipMessage = toolTipMessage;
  }

  public String getWidgetTrayCustomizedIconPath() {
    return widgetTrayCustomizedIconPath;
  }

  public void setWidgetTrayCustomizedIconPath(String widgetTrayCustomizedIconPath) {
    this.widgetTrayCustomizedIconPath = widgetTrayCustomizedIconPath;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getLabel() {
    return label;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPublisherUrl() {
    return publisherUrl;
  }

  public void setPublisherUrl(String publisherUrl) {
    this.publisherUrl = publisherUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isResponsive() {
    return responsive;
  }

  public void setResponsive(boolean responsive) {
    this.responsive = responsive;
  }

  public long getWidgetId() {
    return this.widgetId;
  }

  public void setWidgetId(long id) {
    this.widgetId = id;
  }

  @Override
  public String getId() {
    return Long.toString(getWidgetId());
  }

  @Override
  public void setId(String id) {
    setWidgetId(Long.parseLong(id));
  }
}
