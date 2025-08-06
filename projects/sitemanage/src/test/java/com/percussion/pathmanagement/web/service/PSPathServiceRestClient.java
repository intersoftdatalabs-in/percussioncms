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

package com.percussion.pathmanagement.web.service;

import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.pathmanagement.data.PSItemByWfStateRequest;
import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSRenameFolderItem;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.test.PSDataServiceRestClient;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;

public class PSPathServiceRestClient extends PSDataServiceRestClient<PSPathItem> {
    public PSPathServiceRestClient(String url) {
        super(PSPathItem.class, url, "/Rhythmyx/services/pathmanagement/path/");
    }

    @Override
    protected String getGetPath(String id) {
        return getPath() + "item/" + id;
    }

    public PSPathItem find(String path) {
        return getObjectFromPath(concatPath(getPath(), "item", path));
    }

    public PSPathItem findById(String id) {
        return getObjectFromPath(concatPath(getPath(), "item/id", id));
    }

    public PSItemProperties findItemProperties(String path) {
        return getObjectFromPath(concatPath(getPath(), "itemProperties", path), PSItemProperties.class);
    }

    public PSFolderProperties findFolderProperties(String id) {
        return getObjectFromPath(concatPath(getPath(), "folderProperties", id), PSFolderProperties.class);
    }

    public void saveFolderProperties(PSFolderProperties props) {
        var response = postObjectToPath(concatPath(getPath(), "saveFolderProperties"), props, PSNoContent.class);
        if (!"saveFolderProperties".equals(response.getOperation())) {
            throw new RuntimeException("\"saveFolderProperties\" operation failed.");
        }
    }

    public List<PSPathItem> findChildren(String path) {
        return getObjectsFromPath(concatPath(getPath(), "folder", path));
    }

    public PSPagedItemList findChildren(String path, Integer startIndex, Integer maxResults, String child) {
        return findChildren(path, startIndex, maxResults, child, null);
    }

    public PSPagedItemList findChildren(String path, Integer startIndex, Integer maxResults, String child, Integer displayFormat) {
        return findChildren(path, startIndex, maxResults, child, displayFormat, null, null);
    }

    public PSPagedItemList findChildren(String path, Integer startIndex, Integer maxResults, String child, Integer displayFormat, String sortColumn, String sortOrder) {
        return findChildren(path, startIndex, maxResults, child, displayFormat, sortColumn, sortOrder, null, null);
    }

    public PSPagedItemList findChildren(String path, Integer startIndex, Integer maxResults, String child, Integer displayFormat, String sortColumn, String sortOrder, String category, String type) {
        var params = new StringBuilder();
        if (startIndex != null) params.append(addQueryParam(params.toString(), "startIndex=" + startIndex));
        if (maxResults != null) params.append(addQueryParam(params.toString(), "maxResults=" + maxResults));
        if (child != null) params.append(addQueryParam(params.toString(), "child=" + child));
        if (displayFormat != null) params.append(addQueryParam(params.toString(), "displayFormatId=" + displayFormat));
        if (sortColumn != null) params.append(addQueryParam(params.toString(), "sortColumn=" + sortColumn));
        if (sortOrder != null) params.append(addQueryParam(params.toString(), "sortOrder=" + sortOrder));
        if (category != null) params.append(addQueryParam(params.toString(), "category=" + category));
        if (type != null) params.append(addQueryParam(params.toString(), "type=" + type));
        return getObjectFromPath(concatPath(getPath(), "paginatedFolder", path + params), PSPagedItemList.class);
    }

    private String addQueryParam(String existingParameterList, String newQueryParam) {
        if (newQueryParam == null || newQueryParam.isBlank()) return existingParameterList;
        if (existingParameterList == null || existingParameterList.isBlank()) return "?" + newQueryParam;
        return existingParameterList + "&" + newQueryParam;
    }

    public PSPathItem findRoot() {
        return getObjectFromPath(concatPath(getPath(), "root"));
    }

    public PSPathItem addFolder(String path) {
        return getObjectFromPath(concatPath(getPath(), "addFolder", path));
    }

    public PSPathItem addNewFolder(String path) {
        return getObjectFromPath(concatPath(getPath(), "addNewFolder", path));
    }

    public String deleteFolder(PSDeleteFolderCriteria criteria) {
        return postObjectToPath(concatPath(getPath(), "deleteFolder"), criteria);
    }

    public PSPathItem renameFolder(PSRenameFolderItem item) {
        return postObjectToPath(concatPath(getPath(), "renameFolder"), item, PSPathItem.class);
    }

    public void moveItem(PSMoveFolderItem request) {
        var response = postObjectToPath(concatPath(getPath(), "moveItem"), request, PSNoContent.class);
        if (!"moveItem".equals(response.getOperation())) {
            throw new RuntimeException("\"moveItem\" operation failed.");
        }
    }

    public String validateFolderDelete(String path) {
        return GET(concatPath(getPath(), "validateFolderDelete", path));
    }

    public List<PSItemProperties> findItemProperties(PSItemByWfStateRequest request) {
        return postObjectToPathAndGetObjects(concatPath(getPath(), "item/wfState"), request, PSItemProperties.class);
    }

    public String findLastExistingPath(String path) {
        return GET(concatPath(getPath(), "lastExisting", path));
    }
}
