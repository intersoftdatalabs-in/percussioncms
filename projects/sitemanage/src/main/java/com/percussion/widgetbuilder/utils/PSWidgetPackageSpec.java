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
package com.percussion.widgetbuilder.utils;

import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.WordUtils;

/**
 * Defines a widget package to create.
 * <p>
 * Sunny Sal says: "Widget specs—like a Bollywood script, get the details right and the show will be a hit!"
 * </p>
 */
public class PSWidgetPackageSpec {

    private String prefix;
    private String authorUrl;
    private String title;
    private String widgetVersion;
    private String cm1Version;
    private String description;
    private boolean isResponsive;
    private String widgetName;
    private String fullWidgetName;
    private String packageName;
    private String tooTipMessage;
    private String widgetTrayCustomizedIconPath;
    private Map<String, String> resolverTokenMap;

    private List<PSWidgetBuilderFieldData> fields;
    private String widgetHtml = "";
    private List<String> cssFiles = new ArrayList<>();
    private List<String> jsFiles = new ArrayList<>();

    /**
     * Constructs a widget package specification.
     *
     * @param prefix        The prefix of the package.
     * @param authorUrl     The author URL, also used as the author of the package.
     * @param title         The title of the widget, also used to generate the widget name and package name.
     * @param description   Not {@code null}, may be empty.
     * @param widgetVersion The widget and package version, n, n.n, and n.n.n are supported.
     * @param cm1Version    The current product version, used to set the min/max versions for the package.
     */
    public PSWidgetPackageSpec(String prefix, String authorUrl, String title, String description, String widgetVersion, String cm1Version) {
        Validate.notEmpty(prefix);
        Validate.notEmpty(authorUrl);
        Validate.notEmpty(title);
        Validate.notNull(description);
        Validate.notEmpty(widgetVersion);
        Validate.notEmpty(cm1Version);

        this.prefix = prefix;
        this.authorUrl = authorUrl;
        this.title = title;
        this.description = description;
        this.widgetVersion = widgetVersion;
        this.cm1Version = cm1Version;

        resolverTokenMap = new HashMap<>();

        generateWidgetName();
    }

    /**
     * Generates a widget and package name from the prefix and title and updates applicable property values.
     */
    private void generateWidgetName() {
        var name = WordUtils.capitalize(title);
        name = name.replaceAll("\\s", ""); // remove all whitespace
        widgetName = StringUtils.uncapitalize(name);
        var pre = prefix.toLowerCase();
        fullWidgetName = pre + StringUtils.capitalize(widgetName);
        packageName = pre + "." + "widget" + "." + widgetName;
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

    public String getAuthorUrl() {
        return authorUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getWidgetVersion() {
        return widgetVersion;
    }

    /**
     * Gets the widget name, proper case, without the prefix.
     *
     * @return the name
     */
    public String getWidgetName() {
        return widgetName;
    }

    /**
     * Gets the widget name, with the prefix prepended.
     *
     * @return the name
     */
    public String getFullWidgetName() {
        return fullWidgetName;
    }

    /**
     * Gets the name to use for the widget package file.
     *
     * @return the name
     */
    public String getPackageName() {
        return packageName;
    }

    public String getCm1Version() {
        return cm1Version;
    }

    public String getDescription() {
        return description;
    }

    public boolean isResponsive() {
        return isResponsive;
    }

    public void setResponsive(boolean isResponsive) {
        this.isResponsive = isResponsive;
    }

    public List<PSWidgetBuilderFieldData> getFields() {
        return fields;
    }

    public void setFields(List<PSWidgetBuilderFieldData> fields) {
        if (fields == null || fields.isEmpty())
            throw new IllegalArgumentException("At least one field must be defined.");
        this.fields = fields;
    }

    /**
     * Gets the map used to define additional token values to replace in files during package creation.
     *
     * @return The map; tokens can be added/modified. Does not affect the default token defined by the resolver.
     */
    public Map<String, String> getResolverTokenMap() {
        return resolverTokenMap;
    }

    /**
     * Gets the HTML used to generate the widget.
     *
     * @return The HTML
     */
    public String getWidgetHtml() {
        return widgetHtml;
    }

    /**
     * Sets the HTML used to generate the widget.
     *
     * @param widgetHtml The HTML
     */
    public void setWidgetHtml(String widgetHtml) {
        if (StringUtils.isBlank(widgetHtml))
            throw new IllegalArgumentException("Widget html may not be empty");
        this.widgetHtml = widgetHtml;
    }

    /**
     * Sets the list of CSS files to use.
     *
     * @param cssFiles The files, path is used as is, not {@code null}, may be empty
     */
    public void setCssFiles(List<String> cssFiles) {
        Validate.notNull(cssFiles);
        this.cssFiles = cssFiles;
    }

    /**
     * Gets the list of JS files to use.
     *
     * @return The list, not {@code null}, may be empty.
     */
    public List<String> getJsFiles() {
        return jsFiles;
    }

    /**
     * Sets the list of JS files to use.
     *
     * @param jsFiles The files, path is used as is, not {@code null}, may be empty
     */
    public void setJsFiles(List<String> jsFiles) {
        Validate.notNull(jsFiles);
        this.jsFiles = jsFiles;
    }

    /**
     * Gets the list of CSS files to use.
     *
     * @return The list, not {@code null}, may be empty.
     */
    public List<String> getCssFiles() {
        return cssFiles;
    }

    public String getTooTipMessage() {
        return tooTipMessage;
    }

    public void setTooTipMessage(String tooTipMessage) {
        this.tooTipMessage = tooTipMessage;
    }
}
