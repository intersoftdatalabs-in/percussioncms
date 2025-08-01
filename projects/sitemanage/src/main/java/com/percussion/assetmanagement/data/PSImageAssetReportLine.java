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

// REFACTORED: CP-JAVA11

package com.percussion.assetmanagement.data;

import com.percussion.share.data.PSAbstractBaseCSVReportRow;

import java.util.Optional;
import java.util.stream.Stream;

/***
 * Represents a line in an Image Asset report. All Asset fields are represented
 * as well as site impact.
 *
 * @author natechadwick
 */
public class PSImageAssetReportLine extends PSAbstractBaseCSVReportRow {

    private int id;
    private String guid;
    private String name;
    private String title;
    private String altText;
    private String resourceLinkTitle;
    private String filename;
    private String folderPath;
    private String pubDate;
    private String contentCreatedDate;
    private String contentStartDate;
    private String contentPostDate;
    private String contentCreatedBy;
    private String contentModifiedDate;
    private String contentLastModifier;
    private String workflowState;
    private String workflowName;
    private String siteNames;
    private String pageNames;
    private String pagePaths;
    private String templateNames;
    private String bulkImportAction;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Optional<String> getGuid() {
        return Optional.ofNullable(guid);
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Optional<String> getAltText() {
        return Optional.ofNullable(altText);
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Optional<String> getResourceLinkTitle() {
        return Optional.ofNullable(resourceLinkTitle);
    }

    public void setResourceLinkTitle(String resourceLinkTitle) {
        this.resourceLinkTitle = resourceLinkTitle;
    }

    public Optional<String> getFilename() {
        return Optional.ofNullable(filename);
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Optional<String> getFolderPath() {
        return Optional.ofNullable(folderPath);
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Optional<String> getWorkflowState() {
        return Optional.ofNullable(workflowState);
    }

    public void setWorkflowState(String workflowState) {
        this.workflowState = workflowState;
    }

    public Optional<String> getBulkImportAction() {
        return Optional.ofNullable(bulkImportAction);
    }

    public void setBulkImportAction(String bulkImportAction) {
        this.bulkImportAction = bulkImportAction;
    }

    @Override
    public String getHeaderRow() {
        var headers = Stream.of(
                "CONTENTID", "GUID", "NAME", "FOLDER PATH", "FILENAME", "PAGE NAMES", "PAGE PATHS",
                "TEMPLATES", "SITES", "WORKFLOW", "STATE", "ALT TEXT", "TITLE", "LINK TITLE",
                "DATE CREATED", "CREATED BY", "MODIFIED DATE", "MODIFIED BY", "POST DATE",
                "SCHEDULED PUBLISH DATE", "PUBLISH DATE", "BULK ACTION"
        ).map(this::delimitValue).toArray(String[]::new);
        return String.join(",", headers) + this.endRow();
    }

    @Override
    public String toCSVRow() {
        var values = Stream.of(
                Integer.toString(id),
                guid,
                name,
                folderPath != null ? folderPath.replace("//Folders/$System$", "") : null,
                filename,
                pageNames,
                pagePaths,
                templateNames,
                siteNames,
                workflowName,
                workflowState,
                altText,
                title,
                resourceLinkTitle,
                contentCreatedDate,
                contentCreatedBy,
                contentModifiedDate,
                contentLastModifier,
                contentPostDate,
                contentStartDate,
                pubDate,
                bulkImportAction
        ).map(this::delimitValue).toArray(String[]::new);
        return String.join(",", values) + this.endRow();
    }

    public Optional<String> getPubDate() {
        return Optional.ofNullable(pubDate);
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public Optional<String> getContentCreatedDate() {
        return Optional.ofNullable(contentCreatedDate);
    }

    public void setContentCreatedDate(String contentCreatedDate) {
        this.contentCreatedDate = contentCreatedDate;
    }

    public Optional<String> getContentStartDate() {
        return Optional.ofNullable(contentStartDate);
    }

    public void setContentStartDate(String contentStartDate) {
        this.contentStartDate = contentStartDate;
    }

    public Optional<String> getContentPostDate() {
        return Optional.ofNullable(contentPostDate);
    }

    public void setContentPostDate(String contentPostDate) {
        this.contentPostDate = contentPostDate;
    }

    public Optional<String> getContentCreatedBy() {
        return Optional.ofNullable(contentCreatedBy);
    }

    public void setContentCreatedBy(String contentCreatedBy) {
        this.contentCreatedBy = contentCreatedBy;
    }

    public Optional<String> getContentModifiedDate() {
        return Optional.ofNullable(contentModifiedDate);
    }

    public void setContentModifiedDate(String contentModifiedDate) {
        this.contentModifiedDate = contentModifiedDate;
    }

    public Optional<String> getContentLastModifier() {
        return Optional.ofNullable(contentLastModifier);
    }

    public void setContentLastModifier(String contentLastModifier) {
        this.contentLastModifier = contentLastModifier;
    }

    public Optional<String> getWorkflowName() {
        return Optional.ofNullable(workflowName);
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Optional<String> getSiteNames() {
        return Optional.ofNullable(siteNames);
    }

    public void setSiteNames(String siteNames) {
        this.siteNames = siteNames;
    }

    public Optional<String> getPageNames() {
        return Optional.ofNullable(pageNames);
    }

    public void setPageNames(String pageNames) {
        this.pageNames = pageNames;
    }

    public Optional<String> getPagePaths() {
        return Optional.ofNullable(pagePaths);
    }

    public void setPagePaths(String pagePaths) {
        this.pagePaths = pagePaths;
    }

    public Optional<String> getTemplateNames() {
        return Optional.ofNullable(templateNames);
    }

    public void setTemplateNames(String templateNames) {
        this.templateNames = templateNames;
    }
}
