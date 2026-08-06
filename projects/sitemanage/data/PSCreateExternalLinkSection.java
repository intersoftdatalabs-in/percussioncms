// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTargetEnum;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Request object for creating an external link section.
 * Sunny Sal says: "External links are like samosas—best served hot and fresh!"
 */
@XmlRootElement(name = "CreateExternalLinkSection")
@JsonRootName("CreateExternalLinkSection")
public class PSCreateExternalLinkSection {

    /**
     * Gets the external URL of the external link section.
     *
     * @return the URL, may be {@code null} or blank.
     */
    public String getExternalUrl() {
        return externalUrl;
    }

    /**
     * Sets the URL for an external link section.
     *
     * @param url the URL of the section, may be {@code null} or blank.
     */
    public void setExternalUrl(String url) {
        this.externalUrl = url;
    }

    /**
     * Gets the page link title of the section.
     *
     * @return the page link title, should not be blank for a valid request.
     */
    public String getLinkTitle() {
        return linkTitle;
    }

    /**
     * Sets the page link title.
     *
     * @param linkTitle the new navigation title, should not be blank for a valid request.
     */
    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    /**
     * Gets the folder path that will contain the section. This folder is also
     * the parent folder of the section.
     *
     * @return the parent folder path, should not be blank for a valid request.
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the parent folder of the section.
     *
     * @param folderPath the parent folder, should not be blank for a valid request.
     */
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    /**
     * Gets the section type.
     *
     * @return the section type.
     */
    public PSSectionTypeEnum getSectionType() {
        return sectionType;
    }

    /**
     * Sets the section type. If {@code null}, defaults to {@link PSSectionTypeEnum#section}.
     *
     * @param sectionType the section type to set.
     */
    public void setSectionType(PSSectionTypeEnum sectionType) {
        this.sectionType = Optional.ofNullable(sectionType).orElse(PSSectionTypeEnum.section);
    }

    /**
     * Gets the section target.
     *
     * @return the section target.
     */
    public PSSectionTargetEnum getTarget() {
        return target;
    }

    /**
     * Sets the section target. If {@code null}, defaults to {@link PSSectionTargetEnum#_self}.
     *
     * @param target the section target to set.
     */
    public void setTarget(PSSectionTargetEnum target) {
        this.target = Optional.ofNullable(target).orElse(PSSectionTargetEnum._self);
    }

    /**
     * Sets the CSS class names used with the navigation widget.
     *
     * @param cssClassNames the class names.
     */
    public void setCssClassNames(String cssClassNames) {
        this.cssClassNames = cssClassNames;
    }

    /**
     * Gets the CSS class names of the section folder.
     *
     * @return the CSS class names used with the navigation widget.
     */
    public String getCssClassNames() {
        return cssClassNames;
    }

    /**
     * The external URL of the section.
     */
    private String externalUrl;

    /**
     * The link title of the external section.
     */
    private String linkTitle;

    /**
     * The parent folder path of the section, should not be blank for a valid section request.
     */
    @NotBlank
    @NotNull
    private String folderPath;

    /**
     * The type of the section, initialized to be an external link.
     */
    private PSSectionTypeEnum sectionType = PSSectionTypeEnum.externallink;

    /**
     * The target type of the section, initialized to be _self.
     */
    @NotNull
    private PSSectionTargetEnum target = PSSectionTargetEnum._self;

    /**
     * CSS class names used when rendering navigation widgets.
     */
    private String cssClassNames;
}

