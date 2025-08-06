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

package com.percussion.pagemanagement.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.percussion.share.data.PSAbstractDataObject;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Metadata fields to be saved to template or page.
 * Provides HTML metadata for head, body, and protected regions.
 * @author Luis
 * @author adamgent
 */
@XmlRootElement(name = "HtmlMetadata")
public class PSHtmlMetadata extends PSAbstractDataObject implements IPSHtmlMetadata {

    private static final long serialVersionUID = 1L;

    @NotNull
    @NotEmpty
    private String id;
    private String additionalHeadContent = "";
    private String afterBodyStartContent = "";
    private String beforeBodyCloseContent = "";
    private String protectedRegion = "";
    private String protectedRegionText = "";
    private PSMetadataDocType docType;
    private String description;

    /**
     * Gets the ID of the page or template.
     * @return never {@code null} or empty.
     */
    @XmlElement
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    public void setAfterBodyStartContent(String afterBodyStartContent) {
        this.afterBodyStartContent = afterBodyStartContent;
    }

    @Override
    public String getBeforeBodyCloseContent() {
        return beforeBodyCloseContent;
    }

    @Override
    public void setBeforeBodyCloseContent(String beforeBodyCloseContent) {
        this.beforeBodyCloseContent = beforeBodyCloseContent;
    }

    @Override
    public String getProtectedRegion() {
        return protectedRegion;
    }

    @Override
    public void setProtectedRegion(String protectedRegion) {
        this.protectedRegion = protectedRegion;
    }

    @Override
    public String getProtectedRegionText() {
        return protectedRegionText;
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

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
