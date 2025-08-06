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
// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import net.sf.oval.constraint.AssertValid;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notEmpty;

/**
 * A template is an instance of a layout, which may contain one or more widgets.
 * @author YuBingChen, Sunny Sal
 */
@XmlRootElement(name = "Template")
@JsonRootName("Template")
public class PSTemplate extends PSTemplateSummary implements IPSHtmlMetadata {

    public PSTemplate() {}

    public PSTemplate(PSTemplate template) {
        super(template);
        this.bodyMarkup = template.bodyMarkup;
        this.htmlHeader = template.htmlHeader;
        this.cssRegion = template.cssRegion;
        this.cssOverride = template.cssOverride;
        this.theme = template.theme;
        this.regionTree = template.regionTree;
        this.additionalHeadContent = template.additionalHeadContent;
        this.afterBodyStartContent = template.afterBodyStartContent;
        this.beforeBodyCloseContent = template.beforeBodyCloseContent;
        this.protectedRegion = template.protectedRegion;
        this.protectedRegionText = template.protectedRegionText;
        this.serverVersion = template.serverVersion;
        this.docType = template.docType;
        this.type = template.type;
    }

    private static final long serialVersionUID = 1L;

    public static final String HTML_HEADER = "HTML_HEADER";
    public static final String CSS_MARKUP = "CSS_MARKUP";
    public static final String BODY_MARKUP = "BODY_MARKUP";

    private String bodyMarkup;
    private String htmlHeader;
    private String cssRegion;
    private String cssOverride;
    private String theme;

    @AssertValid
    private PSRegionTree regionTree;
    private String additionalHeadContent;
    private String afterBodyStartContent;
    private String beforeBodyCloseContent;
    private String protectedRegion;
    private String protectedRegionText;
    private String serverVersion;
    private PSMetadataDocType docType;
    private String type;

    public PSRegionTree getRegionTree() {
        return regionTree;
    }

    public void setRegionTree(PSRegionTree templateRegionTree) {
        this.regionTree = templateRegionTree;
    }

    public String getBodyMarkup() {
        return bodyMarkup;
    }

    public void setBodyMarkup(String bodyMarkup) {
        this.bodyMarkup = bodyMarkup;
    }

    public String getHtmlHeader() {
        return htmlHeader;
    }

    public void setHtmlHeader(String htmlHeader) {
        this.htmlHeader = htmlHeader;
    }

    public String getCssRegion() {
        return cssRegion;
    }

    public void setCssRegion(String cssRegion) {
        this.cssRegion = cssRegion;
    }

    public String getCssOverride() {
        return this.cssOverride;
    }

    public void setCssOverride(String cssOverride) {
        this.cssOverride = cssOverride;
    }

    public String getTheme() {
        return this.theme;
    }

    public void setTheme(String theme) {
        if (theme != null && theme.length() > MAX_THEME) {
            theme = theme.substring(0, MAX_THEME);
        }
        this.theme = theme;
    }

    @Override
    public String getAdditionalHeadContent() {
        return additionalHeadContent;
    }

    @Override
    public void setAdditionalHeadContent(String additionalHeadContent) {
        this.additionalHeadContent = additionalHeadContent;
    }

    @Override
    public String getAfterBodyStartContent() {
        return afterBodyStartContent;
    }

    @Override
    public void setAfterBodyStartContent(String header) {
        this.afterBodyStartContent = header;
    }

    @Override
    public String getBeforeBodyCloseContent() {
        return beforeBodyCloseContent;
    }

    @Override
    public void setBeforeBodyCloseContent(String footer) {
        this.beforeBodyCloseContent = footer;
    }

    public String getServerVersion() {
        return this.serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    @Override
    public String getProtectedRegion() {
        return this.protectedRegion;
    }

    @Override
    public void setProtectedRegion(String protectedRegion) {
        if (protectedRegion != null && protectedRegion.length() > MAX_PROTECTED_REGION) {
            protectedRegion = protectedRegion.substring(0, MAX_PROTECTED_REGION);
        }
        this.protectedRegion = protectedRegion;
    }

    @Override
    public String getProtectedRegionText() {
        return this.protectedRegionText;
    }

    @Override
    public void setProtectedRegionText(String protectedRegionText) {
        this.protectedRegionText = protectedRegionText;
    }

    @Override
    public PSMetadataDocType getDocType() {
        return docType;
    }

    @Override
    public void setDocType(PSMetadataDocType docType) {
        this.docType = docType;
    }

    /**
     * Determines if this template has an instance of the specified widget.
     * @param definitionId of the widget, never blank.
     * @return true if the template has an instance of the widget, false otherwise.
     */
    public boolean hasWidget(String definitionId) {
        notEmpty(definitionId, "definitionId may not be empty");
        return getWidgets().stream()
                .anyMatch(widget -> widget.getDefinitionId().equalsIgnoreCase(definitionId));
    }

    /**
     * Gets all widgets contained in this template.
     * @return list of widget items, never null, may be empty.
     */
    public List<PSWidgetItem> getWidgets() {
        var widgetList = new ArrayList<PSWidgetItem>();
        var treeRegion = getRegionTree();
        if (treeRegion != null) {
            Map<String, List<PSWidgetItem>> widgetMap = treeRegion.getRegionWidgetsMap();
            for (var widgets : widgetMap.values()) {
                widgetList.addAll(widgets);
            }
        }
        return widgetList;
    }

    /**
     * Gets the type of the template.
     * Possible values are NORMAL or UNASSIGNED.
     * @return the string with the type of template. May be null.
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the template.
     * Possible values are NORMAL or UNASSIGNED.
     * @param type - the type of template
     */
    @Override
    public void setType(String type) {
        if (type != null && type.length() > MAX_TYPE) {
            type = type.substring(0, MAX_TYPE);
        }
        this.type = type;
    }

    /**
     * Public enum for the template's type.
     * Possible values are NORMAL and UNASSIGNED.
     */
    public enum PSTemplateTypeEnum {
        NORMAL("NORMAL"),
        UNASSIGNED("UNASSIGNED");

        private final String label;

        PSTemplateTypeEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static PSTemplateTypeEnum getEnum(String label) {
            for (var v : values()) {
                if (v.getLabel().equals(label)) {
                    return v;
                }
            }
            return null;
        }
    }
}
