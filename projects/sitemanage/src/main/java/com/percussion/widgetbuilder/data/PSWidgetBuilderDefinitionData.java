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

import com.percussion.services.widgetbuilder.PSWidgetBuilderDefinition;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents the full definition of a widget for the Widget Builder. Includes fields, HTML, JS, and
 * CSS resources.
 */
@XmlRootElement(name = "WidgetBuilderDefinitionData")
public class PSWidgetBuilderDefinitionData extends PSWidgetBuilderSummaryData {

  private static final long serialVersionUID = -1L;

  private PSWidgetBuilderFieldsListData fieldsList = new PSWidgetBuilderFieldsListData();
  private String widgetHtml;
  private PSWidgetBuilderResourceListData jsFileList = new PSWidgetBuilderResourceListData();
  private PSWidgetBuilderResourceListData cssFileList = new PSWidgetBuilderResourceListData();

  public PSWidgetBuilderDefinitionData() {
    super();
  }

  /**
   * Create from DAO.
   *
   * @param dao The DAO to copy from, not {@code null}.
   */
  public PSWidgetBuilderDefinitionData(PSWidgetBuilderDefinition dao) {
    super(dao);
    // Direct field seeds — avoid overridable setters during construction (this-escape).
    if (StringUtils.isNotBlank(dao.getFields())) {
      this.fieldsList = PSWidgetBuilderFieldsListData.fromXml(dao.getFields());
    }
    this.widgetHtml = dao.getWidgetHtml();
    if (StringUtils.isNotBlank(dao.getCssFiles())) {
      this.cssFileList = PSWidgetBuilderResourceListData.fromXml(dao.getCssFiles());
    }
    if (StringUtils.isNotBlank(dao.getJsFiles())) {
      this.jsFileList = PSWidgetBuilderResourceListData.fromXml(dao.getJsFiles());
    }
  }

  public static PSWidgetBuilderDefinition createDaoObject(PSWidgetBuilderDefinitionData data) {
    var definition = new PSWidgetBuilderDefinition();
    definition.setAuthor(data.getAuthor());
    definition.setDescription(data.getDescription());
    definition.setLabel(data.getLabel());
    definition.setPrefix(data.getPrefix());
    definition.setPublisherUrl(data.getPublisherUrl());
    definition.setVersion(data.getVersion());
    if (data.getWidgetId() > 0) {
      definition.setWidgetBuilderDefinitionId(data.getWidgetId());
    }
    definition.setFields(data.getFieldsList().toXml());
    definition.setJsFiles(data.getJsFileList().toXml());
    definition.setCssFiles(data.getCssFileList().toXml());
    definition.setWidgetHtml(data.getWidgetHtml());
    definition.setResponsive(data.isResponsive());
    definition.setWidgetTrayCustomizedIconPath(data.getWidgetTrayCustomizedIconPath());
    definition.setToolTipMessage(data.getToolTipMessage());
    return definition;
  }

  /**
   * Get the list of fields.
   *
   * @return The fields list, never {@code null}.
   */
  public PSWidgetBuilderFieldsListData getFieldsList() {
    return fieldsList;
  }

  /**
   * Set the list of fields.
   *
   * @param fieldsList The field list, not {@code null}.
   */
  public void setFieldsList(PSWidgetBuilderFieldsListData fieldsList) {
    Validate.notNull(fieldsList, "fieldsList must not be null");
    this.fieldsList = fieldsList;
  }

  /**
   * Set the HTML used to render the widget.
   *
   * @param widgetHtml The HTML, not {@code null} or empty.
   */
  public void setWidgetHtml(String widgetHtml) {
    Validate.notNull(widgetHtml, "widgetHtml must not be null");
    this.widgetHtml = widgetHtml;
  }

  /**
   * Get the HTML used to render the widget.
   *
   * @return The HTML, may be {@code null}, not empty.
   */
  public String getWidgetHtml() {
    return widgetHtml;
  }

  /**
   * Get the list of JS files.
   *
   * @return The list, not {@code null}.
   */
  public PSWidgetBuilderResourceListData getJsFileList() {
    return jsFileList;
  }

  /**
   * Set the list of JS files.
   *
   * @param jsFileList The list, not {@code null}.
   */
  public void setJsFileList(PSWidgetBuilderResourceListData jsFileList) {
    Validate.notNull(jsFileList, "jsFileList must not be null");
    this.jsFileList = jsFileList;
  }

  /**
   * Get the list of CSS files.
   *
   * @return The list, not {@code null}.
   */
  public PSWidgetBuilderResourceListData getCssFileList() {
    return cssFileList;
  }

  /**
   * Set the list of CSS files.
   *
   * @param cssFileList The list, not {@code null}.
   */
  public void setCssFileList(PSWidgetBuilderResourceListData cssFileList) {
    Validate.notNull(cssFileList, "cssFileList must not be null");
    this.cssFileList = cssFileList;
  }

  @Override
  public String toString() {
    return "PSWidgetBuilderDefinitionData{"
        + "fieldsList="
        + fieldsList
        + ", widgetHtml='"
        + widgetHtml
        + '\''
        + ", jsFileList="
        + jsFileList
        + ", cssFileList="
        + cssFileList
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSWidgetBuilderDefinitionData)) return false;
    var that = (PSWidgetBuilderDefinitionData) o;
    return Objects.equals(getFieldsList(), that.getFieldsList())
        && Objects.equals(getWidgetHtml(), that.getWidgetHtml())
        && Objects.equals(getJsFileList(), that.getJsFileList())
        && Objects.equals(getCssFileList(), that.getCssFileList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFieldsList(), getWidgetHtml(), getJsFileList(), getCssFileList());
  }
}
