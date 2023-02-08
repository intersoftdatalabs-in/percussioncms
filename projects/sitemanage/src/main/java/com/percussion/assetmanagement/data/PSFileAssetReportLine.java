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

package com.percussion.assetmanagement.data;

import com.percussion.share.data.PSAbstractBaseCSVReportRow;

/***
 * Represents a line in the File Asset Reports.
 * 
 * @author natechadwick
 *
 */
public class PSFileAssetReportLine extends PSAbstractBaseCSVReportRow {

	
	private int id;
	private String guid;
	private String name;
	private String title;
	private String extension;
	private String filename;
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public String getPubDate() {
		return pubDate;
	}

	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}

	public String getContentCreatedDate() {
		return contentCreatedDate;
	}

	public void setContentCreatedDate(String contentCreatedDate) {
		this.contentCreatedDate = contentCreatedDate;
	}

	public String getContentStartDate() {
		return contentStartDate;
	}

	public void setContentStartDate(String contentStartDate) {
		this.contentStartDate = contentStartDate;
	}

	public String getContentPostDate() {
		return contentPostDate;
	}

	public void setContentPostDate(String contentPostDate) {
		this.contentPostDate = contentPostDate;
	}

	public String getContentCreatedBy() {
		return contentCreatedBy;
	}

	public void setContentCreatedBy(String contentCreatedBy) {
		this.contentCreatedBy = contentCreatedBy;
	}

	public String getContentModifiedDate() {
		return contentModifiedDate;
	}

	public void setContentModifiedDate(String contentModifiedDate) {
		this.contentModifiedDate = contentModifiedDate;
	}

	public String getContentLastModifier() {
		return contentLastModifier;
	}

	public void setContentLastModifier(String contentLastModifier) {
		this.contentLastModifier = contentLastModifier;
	}

	public String getWorkflowState() {
		return workflowState;
	}

	public void setWorkflowState(String workflowState) {
		this.workflowState = workflowState;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getSiteNames() {
		return siteNames;
	}

	public void setSiteNames(String siteNames) {
		this.siteNames = siteNames;
	}

	public String getPageNames() {
		return pageNames;
	}

	public void setPageNames(String pageNames) {
		this.pageNames = pageNames;
	}

	public String getPagePaths() {
		return pagePaths;
	}

	public void setPagePaths(String pagePaths) {
		this.pagePaths = pagePaths;
	}

	public String getTemplateNames() {
		return templateNames;
	}

	public void setTemplateNames(String templateNames) {
		this.templateNames = templateNames;
	}

	public String getBulkImportAction() {
		return bulkImportAction;
	}

	public void setBulkImportAction(String bulkImportAction) {
		this.bulkImportAction = bulkImportAction;
	}
	// Added AltText column for non-ada-files report | CMS-3216
    public String getAltText() { return altText; }

    public void setAltText(String altText) {
        this.altText = altText;
    }

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
	// Added AltText column for non-ada-files report | CMS-3216
    private String altText;
	
	@Override
	public String getHeaderRow(){
StringBuilder sb = new StringBuilder();
		
		sb.append(this.delimitValue("CONTENTID"));
		sb.append(",");
		sb.append(this.delimitValue("GUID"));
		sb.append(",");
		sb.append(this.delimitValue("NAME"));
		sb.append(",");
		sb.append(this.delimitValue("FOLDER PATH"));
		sb.append(",");
		sb.append(this.delimitValue("FILENAME"));
		sb.append(",");
		sb.append(this.delimitValue("EXTENSION"));
		sb.append(",");
		sb.append(this.delimitValue("PAGE NAMES"));
		sb.append(",");
		sb.append(this.delimitValue("PAGE PATHS"));
		sb.append(",");
		sb.append(this.delimitValue("TEMPLATES"));
		sb.append(",");
		sb.append(this.delimitValue("SITES"));
		sb.append(",");
		sb.append(this.delimitValue("WORKFLOW"));
		sb.append(",");
		sb.append(this.delimitValue("STATE"));
		sb.append(",");
		// Added AltText header to non-ada-files report | CMS-3216
        sb.append(this.delimitValue("ALT TEXT"));
        sb.append(",");
		sb.append(this.delimitValue("TITLE"));
		sb.append(",");
		sb.append(this.delimitValue("DATE CREATED"));
		sb.append(",");
		sb.append(this.delimitValue("CREATED BY"));
		sb.append(",");
		sb.append(this.delimitValue("MODIFIED DATE"));
		sb.append(",");
		sb.append(this.delimitValue("MODIFIED BY"));
		sb.append(",");

		sb.append(this.delimitValue("POST DATE"));
		sb.append(",");
		sb.append(this.delimitValue("SCHEDULED PUBLISH DATE"));
		sb.append(",");
		sb.append(this.delimitValue("PUBLISH DATE"));
		sb.append(",");
		sb.append(this.delimitValue("BULK ACTION"));
		sb.append(this.endRow());
		
		return sb.toString();
	}
	
	@Override
	public String toCSVRow() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.delimitValue(Integer.toString(id)));
		sb.append(",");
		sb.append(this.delimitValue(guid));
		sb.append(",");
		sb.append(this.delimitValue(name));
		sb.append(",");
		sb.append(this.delimitValue(folderPath).replace("//Folders/$System$", ""));
		sb.append(",");
		sb.append(this.delimitValue(this.filename));
		sb.append(",");
		sb.append(this.delimitValue(this.extension));
		sb.append(",");
		sb.append(this.delimitValue(this.pageNames));
		sb.append(",");
		sb.append(this.delimitValue(this.pagePaths));
		sb.append(",");
		sb.append(this.delimitValue(this.templateNames));
		sb.append(",");
		sb.append(this.delimitValue(this.siteNames));
		sb.append(",");
		sb.append(this.delimitValue(this.workflowName));
		sb.append(",");
		sb.append(this.delimitValue(this.workflowState));
		sb.append(",");
		// Added AltText column value to non-ada-files report | CMS-3216
		sb.append(this.delimitValue(this.altText));
        sb.append(",");
		sb.append(this.delimitValue(this.title));
		sb.append(",");
		sb.append(this.delimitValue(this.contentCreatedDate));
		sb.append(",");
		sb.append(this.delimitValue(this.contentCreatedBy));
		sb.append(",");
		sb.append(this.delimitValue(this.contentModifiedDate));
		sb.append(",");
		sb.append(this.delimitValue(this.contentLastModifier));
		sb.append(",");
		sb.append(this.delimitValue(this.contentPostDate));
		sb.append(",");
		sb.append(this.delimitValue(this.contentStartDate));
		sb.append(",");
		sb.append(this.delimitValue(this.pubDate));
		sb.append(",");
		sb.append(this.delimitValue(this.bulkImportAction));
		sb.append(this.endRow());
		
		return sb.toString();
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

		

}
