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

package com.percussion.services.widgetbuilder;

import com.percussion.share.data.PSAbstractDataObject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Widget Builder Definition entity representing widget configurations and metadata with modern Java 11 patterns.
 * This JPA entity stores all the necessary information for building and rendering widgets with enhanced
 * validation, Optional-based safe access, and comprehensive utility methods.
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

    // Enhanced getters and setters with modern Java 11 patterns

    /**
     * Gets the widget builder definition ID.
     *
     * @return the widget builder definition ID
     */
    public long getWidgetBuilderDefinitionId() {
        return widgetBuilderDefinitionId;
    }

    /**
     * Sets the widget builder definition ID with validation.
     *
     * @param widgetBuilderDefinitionId the widget builder definition ID to set
     * @throws IllegalArgumentException if the ID is negative (except -1 for new entities)
     */
    public void setWidgetBuilderDefinitionId(long widgetBuilderDefinitionId) {
        if (widgetBuilderDefinitionId < -1) {
            throw new IllegalArgumentException("Widget builder definition ID cannot be less than -1");
        }
        this.widgetBuilderDefinitionId = widgetBuilderDefinitionId;
    }

    /**
     * Checks if this is a new entity (not yet persisted).
     *
     * @return true if this is a new entity, false otherwise
     */
    public boolean isNew() {
        return widgetBuilderDefinitionId == -1L;
    }

    /**
     * Gets the widget prefix with safe access.
     *
     * @return Optional containing the prefix, empty if not set
     */
    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix).filter(p -> !p.trim().isEmpty());
    }

    /**
     * Sets the widget prefix with validation.
     *
     * @param prefix the prefix to set, may be null
     * @throws IllegalArgumentException if prefix is blank when not null
     */
    public void setPrefix(String prefix) {
        if (prefix != null && prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget prefix cannot be blank");
        }
        this.prefix = prefix;
    }

    /**
     * Gets the widget author with safe access.
     *
     * @return Optional containing the author, empty if not set
     */
    public Optional<String> getAuthor() {
        return Optional.ofNullable(author).filter(a -> !a.trim().isEmpty());
    }

    /**
     * Sets the widget author.
     *
     * @param author the author to set, may be null
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets the widget label with safe access.
     *
     * @return Optional containing the label, empty if not set
     */
    public Optional<String> getLabel() {
        return Optional.ofNullable(label).filter(l -> !l.trim().isEmpty());
    }

    /**
     * Returns the label as a legacy `getName()` for backward compatibility.
     * @return the label or null
     */
    public String getName() {
        return label;
    }

    /**
     * Sets the widget label with validation.
     *
     * @param label the label to set, may be null
     * @throws IllegalArgumentException if label is blank when not null
     */
    public void setLabel(String label) {
        if (label != null && label.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget label cannot be blank");
        }
        this.label = label;
    }

    /**
     * Gets the publisher URL with safe access.
     *
     * @return Optional containing the publisher URL, empty if not set
     */
    public Optional<String> getPublisherUrl() {
        return Optional.ofNullable(publisherUrl).filter(url -> !url.trim().isEmpty());
    }

    /**
     * Sets the publisher URL.
     *
     * @param publisherUrl the publisher URL to set, may be null
     */
    public void setPublisherUrl(String publisherUrl) {
        this.publisherUrl = publisherUrl;
    }

    /**
     * Gets the widget description with safe access.
     *
     * @return Optional containing the description, empty if not set
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description).filter(d -> !d.trim().isEmpty());
    }

    /**
     * Sets the widget description.
     *
     * @param description the description to set, may be null
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the widget version with safe access.
     *
     * @return Optional containing the version, empty if not set
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version).filter(v -> !v.trim().isEmpty());
    }

    /**
     * Sets the widget version with validation.
     *
     * @param version the version to set, may be null
     * @throws IllegalArgumentException if version is blank when not null
     */
    public void setVersion(String version) {
        if (version != null && version.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget version cannot be blank");
        }
        this.version = version;
    }

    /**
     * Gets the customized icon path with safe access.
     *
     * @return Optional containing the icon path, empty if not set
     */
    public Optional<String> getWidgetTrayCustomizedIconPath() {
        return Optional.ofNullable(widgetTrayCustomizedIconPath).filter(path -> !path.trim().isEmpty());
    }

    /**
     * Sets the customized icon path.
     *
     * @param widgetTrayCustomizedIconPath the icon path to set, may be null
     */
    public void setWidgetTrayCustomizedIconPath(String widgetTrayCustomizedIconPath) {
        this.widgetTrayCustomizedIconPath = widgetTrayCustomizedIconPath;
    }

    /**
     * Gets the tooltip message with safe access.
     *
     * @return Optional containing the tooltip message, empty if not set
     */
    public Optional<String> getToolTipMessage() {
        return Optional.ofNullable(toolTipMessage).filter(msg -> !msg.trim().isEmpty());
    }

    /**
     * Sets the tooltip message.
     *
     * @param toolTipMessage the tooltip message to set, may be null
     */
    public void setToolTipMessage(String toolTipMessage) {
        this.toolTipMessage = toolTipMessage;
    }

    /**
     * Checks if the widget is responsive.
     *
     * @return true if responsive, false otherwise
     */
    public boolean isResponsive() {
        return "y".equals(isResponsive);
    }

    /**
     * Sets the responsive flag for the widget.
     *
     * @param responsive true if responsive, false otherwise
     */
    public void setResponsive(boolean responsive) {
        this.isResponsive = responsive ? "y" : "n";
    }

    /**
     * Gets the fields configuration as a string.
     *
     * @return the fields string, never null but may be empty
     */
    public String getFields() {
        return fields != null ? fields : "";
    }

    /**
     * Sets the fields configuration.
     *
     * @param fields the fields string to set, may be null
     */
    public void setFields(String fields) {
        this.fields = fields;
    }

    /**
     * Gets the widget HTML content.
     *
     * @return the widget HTML, never null but may be empty
     */
    public String getWidgetHtml() {
        return widgetHtml != null ? widgetHtml : "";
    }

    /**
     * Sets the widget HTML content.
     *
     * @param widgetHtml the widget HTML to set, may be null
     */
    public void setWidgetHtml(String widgetHtml) {
        this.widgetHtml = widgetHtml;
    }

    /**
     * Gets the CSS files configuration.
     *
     * @return the CSS files string, never null but may be empty
     */
    public String getCssFiles() {
        return cssFiles != null ? cssFiles : "";
    }

    /**
     * Sets the CSS files configuration.
     *
     * @param cssFiles the CSS files string to set, may be null
     */
    public void setCssFiles(String cssFiles) {
        this.cssFiles = cssFiles;
    }

    /**
     * Gets the JavaScript files configuration.
     *
     * @return the JS files string, never null but may be empty
     */
    public String getJsFiles() {
        return jsFiles != null ? jsFiles : "";
    }

    /**
     * Sets the JavaScript files configuration.
     *
     * @param jsFiles the JS files string to set, may be null
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
        if (!getPrefix().isPresent()) {
            throw new IllegalArgumentException("Widget prefix is required");
        }
        if (!getLabel().isPresent()) {
            throw new IllegalArgumentException("Widget label is required");
        }
        if (!getVersion().isPresent()) {
            throw new IllegalArgumentException("Widget version is required");
        }
    }

    /**
     * Checks if this widget definition is complete and valid.
     *
     * @return true if all required fields are present, false otherwise
     */
    public boolean isValid() {
        return getPrefix().isPresent() && getLabel().isPresent() && getVersion().isPresent();
    }

    /**
     * Gets a display name for this widget definition.
     *
     * @return a display name combining label and version, or ID if no label
     */
    public String getDisplayName() {
        return getLabel()
            .map(l -> getVersion().map(v -> l + " (v" + v + ")").orElse(l))
            .orElse("Widget " + widgetBuilderDefinitionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        var other = (PSWidgetBuilderDefinition) obj;
        return widgetBuilderDefinitionId == other.widgetBuilderDefinitionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(widgetBuilderDefinitionId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PSWidgetBuilderDefinition.class.getSimpleName() + "[", "]")
            .add("id=" + widgetBuilderDefinitionId)
            .add("prefix='" + prefix + "'")
            .add("label='" + label + "'")
            .add("version='" + version + "'")
            .add("responsive=" + isResponsive())
            .toString();
    }
}
