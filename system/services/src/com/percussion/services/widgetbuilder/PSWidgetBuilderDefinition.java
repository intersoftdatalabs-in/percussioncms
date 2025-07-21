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

package com.percussion.services.widgetbuilder;

import com.percussion.share.data.PSAbstractDataObject;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Widget Builder Definition entity representing widget configurations and metadata.
 * This JPA entity stores all the necessary information for building and rendering widgets.
 *
 * @author matthewernewein
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSWidgetBuilderDefinition")
@Table(name = "PSX_WIDGETBUILDERDEFINITION")
public class PSWidgetBuilderDefinition extends PSAbstractDataObject {

    @Id
    @Column(name = "WIDGETBUILDERDEFINITIONID")
    private long widgetBuilderDefinitionId = -1L;

    @Basic
    @Column(name = "PREFIX", nullable = true)
    private String prefix;

    @Basic
    @Column(name = "AUTHOR", nullable = true)
    private String author;

    @Basic
    @Column(name = "LABEL", nullable = true)
    private String label;

    @Basic
    @Column(name = "PUBLISHERURL", nullable = true)
    private String publisherUrl;

    @Basic
    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Basic
    @Column(name = "VERSION", nullable = true)
    private String version;

    @Basic
    @Column(name = "FIELDS", nullable = true)
    private String fields = "";

    @Basic
    @Column(name = "WIDGET_HTML", nullable = true)
    private String widgetHtml = "";

    @Basic
    @Column(name = "CSS_FILES", nullable = true)
    private String cssFiles = "";

    @Basic
    @Column(name = "JS_FILES", nullable = true)
    private String jsFiles = "";

    @Basic
    @Column(name = "IS_RESPONSIVE", nullable = true)
    private String isResponsive;

    @Basic
    @Column(name = "WIDGET_TRAY_CUSTOMIZED_ICON_PATH", nullable = true)
    private String widgetTrayCustomizedIconPath;

    @Basic
    @Column(name = "TOOLTIP_MESSAGE", nullable = true)
    private String toolTipMessage;

    // Getters and Setters with modern Java 11 style

    public String getWidgetTrayCustomizedIconPath() {
        return widgetTrayCustomizedIconPath;
    }

    public void setWidgetTrayCustomizedIconPath(String widgetTrayCustomizedIconPath) {
        this.widgetTrayCustomizedIconPath = widgetTrayCustomizedIconPath;
    }

    public String getToolTipMessage() {
        return toolTipMessage;
    }

    public void setToolTipMessage(String toolTipMessage) {
        this.toolTipMessage = toolTipMessage;
    }

    /**
     * Gets the widget builder definition ID.
     *
     * @return the widgetBuilderDefinitionId
     */
    public long getWidgetBuilderDefinitionId() {
        return widgetBuilderDefinitionId;
    }

    /**
     * Sets the widget builder definition ID.
     *
     * @param widgetBuilderDefinitionId the widgetBuilderDefinitionId to set
     */
    public void setWidgetBuilderDefinitionId(long widgetBuilderDefinitionId) {
        this.widgetBuilderDefinitionId = widgetBuilderDefinitionId;
    }

    /**
     * Gets the widget prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the widget prefix.
     *
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the widget label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the widget label.
     *
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the widget author.
     *
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the widget author.
     *
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets the publisher URL.
     *
     * @return the publisherUrl
     */
    public String getPublisherUrl() {
        return publisherUrl;
    }

    /**
     * Sets the publisher URL.
     *
     * @param publisherUrl the publisherUrl to set
     */
    public void setPublisherUrl(String publisherUrl) {
        this.publisherUrl = publisherUrl;
    }

    /**
     * Gets the widget description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the widget description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the widget version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the widget version.
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Checks if the widget is responsive.
     *
     * @return {@code true} if responsive, {@code false} otherwise
     */
    public boolean isResponsive() {
        return "y".equals(isResponsive);
    }

    /**
     * Sets the responsive flag for the widget.
     *
     * @param responsive {@code true} if responsive, {@code false} otherwise
     */
    public void setResponsive(boolean responsive) {
        this.isResponsive = responsive ? "y" : "n";
    }

    /**
     * Gets the string that represents the collection of fields.
     *
     * @return The field data, never {@code null}, may be empty.
     */
    public String getFields() {
        return fields == null ? "" : fields;
    }

    /**
     * Sets the string that represents the collection of fields.
     *
     * @param fields the fields to set, may be {@code null}
     */
    public void setFields(String fields) {
        this.fields = fields;
    }

    /**
     * Gets the widget HTML content.
     *
     * @return The widget HTML, never {@code null}, may be empty.
     */
    public String getWidgetHtml() {
        return widgetHtml == null ? "" : widgetHtml;
    }

    /**
     * Sets the widget HTML content.
     *
     * @param widgetHtml the widget HTML to set, may be {@code null}
     */
    public void setWidgetHtml(String widgetHtml) {
        this.widgetHtml = widgetHtml;
    }

    /**
     * Gets the CSS files associated with the widget.
     *
     * @return The CSS files, never {@code null}, may be empty.
     */
    public String getCssFiles() {
        return cssFiles == null ? "" : cssFiles;
    }

    /**
     * Sets the CSS files associated with the widget.
     *
     * @param cssFiles the CSS files to set, may be {@code null}
     */
    public void setCssFiles(String cssFiles) {
        this.cssFiles = cssFiles;
    }

    /**
     * Gets the JavaScript files associated with the widget.
     *
     * @return The JS files, never {@code null}, may be empty.
     */
    public String getJsFiles() {
        return jsFiles == null ? "" : jsFiles;
    }

    /**
     * Sets the JavaScript files associated with the widget.
     *
     * @param jsFiles the JS files to set, may be {@code null}
     */
    public void setJsFiles(String jsFiles) {
        this.jsFiles = jsFiles;
    }

    /**
     * Validates this widget definition ensuring all required fields are properly set.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        Validate.notBlank(prefix, "Widget prefix cannot be blank");
        Validate.notBlank(label, "Widget label cannot be blank");
        Validate.notBlank(version, "Widget version cannot be blank");
    }
}
