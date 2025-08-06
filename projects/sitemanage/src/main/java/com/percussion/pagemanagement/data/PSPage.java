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

import com.percussion.share.service.IPSLinkableItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import net.sf.oval.constraint.AssertValid;
import net.sf.oval.constraint.NotNull;

/**
 * A page is the base item for all site pages.
 */
@XmlRootElement(name = "Page")
public class PSPage extends PSPageSummary implements IPSLinkableItem, IPSHtmlMetadata {

    private static final long serialVersionUID = 1L;

    private String additionalHeadContent;
    private String afterBodyStartContent;
    private String beforeBodyCloseContent;
    private String protectedRegion;
    private String protectedRegionText;
    @NotNull
    @AssertValid
    private PSRegionBranches regionBranches = new PSRegionBranches();
    private String summary;
    private Integer workflowId;
    private PSMetadataDocType docType;
    private boolean addToRecent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSPage)) return false;
        var psPage = (PSPage) o;
        return isAddToRecent() == psPage.isAddToRecent()
                && Objects.equals(getAdditionalHeadContent(), psPage.getAdditionalHeadContent())
                && Objects.equals(getAfterBodyStartContent(), psPage.getAfterBodyStartContent())
                && Objects.equals(getBeforeBodyCloseContent(), psPage.getBeforeBodyCloseContent())
                && Objects.equals(getProtectedRegion(), psPage.getProtectedRegion())
                && Objects.equals(getProtectedRegionText(), psPage.getProtectedRegionText())
                && Objects.equals(getRegionBranches(), psPage.getRegionBranches())
                && Objects.equals(getSummary(), psPage.getSummary())
                && Objects.equals(getWorkflowId(), psPage.getWorkflowId())
                && Objects.equals(getDocType(), psPage.getDocType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAdditionalHeadContent(), getAfterBodyStartContent(), getBeforeBodyCloseContent(),
                getProtectedRegion(), getProtectedRegionText(), getRegionBranches(), getSummary(), getWorkflowId(),
                getDocType(), isAddToRecent());
    }

    /**
     * Gets all widgets contained in this page, which does not include widgets in the underlying template.
     * @return the widgets, never {@code null}, may be empty.
     */
    public List<PSWidgetItem> getWidgets() {
        return getWidgets(null);
    }

    /**
     * Gets all widgets contained in this page, which does not include widgets
     * in the underlying template.
     * @param template if not {@code null} excludes widgets from page
     *                 regions that also have the widgets on template region.
     * @return the widgets, never {@code null}, may be empty.
     */
    public List<PSWidgetItem> getWidgets(PSTemplate template) {
        var widgetList = new ArrayList<PSWidgetItem>();
        Map<String, List<PSWidgetItem>> tplWidgetMap = new HashMap<>();
        Set<String> tplRegIds = new HashSet<>();
        if (template != null) {
            tplWidgetMap = template.getRegionTree().getRegionWidgetsMap();
            var tplRegs = template.getRegionTree().getRootRegion().getAllRegions();
            for (var reg : tplRegs) {
                tplRegIds.add(reg.getRegionId());
            }
        }
        var widgetMap = getRegionBranches().getRegionWidgetsMap();
        for (var r : getRegionBranches().getRegions()) {
            // Don't return widgets if template has widgets in a region and page also has widgets in the same region.
            // As the page widgets will not get rendered. Also don't return page widgets if the region doesn't exist
            // on the template anymore.
            if (!tplWidgetMap.keySet().contains(r.getRegionId()) && tplRegIds.contains(r.getRegionId())) {
                var widgets = widgetMap.get(r.getRegionId());
                if (widgets != null) {
                    widgetList.addAll(widgets);
                }
            }
        }
        return widgetList;
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

    public PSRegionBranches getRegionBranches() {
        return regionBranches;
    }

    public void setRegionBranches(PSRegionBranches pageRegionBranches) {
        this.regionBranches = pageRegionBranches;
    }

    /**
     * @return the summary may be {@code null} or empty.
     */
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * The workflow ID associated with the page. Never {@code null}.
     */
    @XmlTransient
    public Integer getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String getProtectedRegion() {
        return this.protectedRegion;
    }

    @Override
    public void setProtectedRegion(String protectedRegion) {
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

    public boolean isAddToRecent() {
        return addToRecent;
    }

    public void setAddToRecent(boolean addToRecent) {
        this.addToRecent = addToRecent;
    }

    @Override
    public String toString() {
        return "PSPage{" +
                "additionalHeadContent='" + additionalHeadContent + '\'' +
                ", afterBodyStartContent='" + afterBodyStartContent + '\'' +
                ", beforeBodyCloseContent='" + beforeBodyCloseContent + '\'' +
                ", protectedRegion='" + protectedRegion + '\'' +
                ", protectedRegionText='" + protectedRegionText + '\'' +
                ", regionBranches=" + regionBranches +
                ", summary='" + summary + '\'' +
                ", workflowId=" + workflowId +
                ", docType=" + docType +
                ", addToRecent=" + addToRecent +
                '}';
    }
}
