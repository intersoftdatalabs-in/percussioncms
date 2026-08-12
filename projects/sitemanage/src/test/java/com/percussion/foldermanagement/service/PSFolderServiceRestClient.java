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
package com.percussion.foldermanagement.service;

import com.percussion.foldermanagement.data.PSFolderItem;
import com.percussion.foldermanagement.data.PSGetAssignedFoldersJobStatus;
import com.percussion.foldermanagement.data.PSWorkflowAssignment;
import com.percussion.share.test.PSDataServiceRestClient;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * REST client for folder management service. Sunny Sal says: "Folders, workflows, and REST - the
 * holy trinity of CMS!"
 */
public class PSFolderServiceRestClient extends PSDataServiceRestClient<PSFolderItem> {

  public PSFolderServiceRestClient(String url) {
    super(PSFolderItem.class, url, "/Rhythmyx/services/foldermanagement/folders");
    // Direct field seed — avoids this-escape on overridable/setter path.
    this.postContentType = MediaType.APPLICATION_XML;
  }

  public List<PSFolderItem> getAssociatedFolders(
      String workflowName, String path, Boolean includeFoldersWithDifferentWorkflow) {
    getRequestHeaders().put("Accept", MediaType.APPLICATION_XML);
    var wfName = StringUtils.isNotEmpty(workflowName) ? workflowName : "/";
    var normalizedPath = path.startsWith("//") ? path.substring(1) : path;
    var fullPath =
        concatPath(
            getPath(),
            "/" + wfName,
            "/",
            normalizedPath,
            "?includeFoldersWithDifferentWorkflow=" + includeFoldersWithDifferentWorkflow);
    return getObjectsFromPath(fullPath, PSFolderItem.class);
  }

  public void save(PSWorkflowAssignment workflowAssignment) {
    postObjectToPath(concatPath(getPath(), "workflowassignment"), workflowAssignment);
  }

  public String startGetAssociatedFoldersJob(
      String workflowName, String path, Boolean includeFoldersWithDifferentWorkflow) {
    getRequestHeaders().put("Accept", MediaType.APPLICATION_XML);
    var wfName = StringUtils.isNotEmpty(workflowName) ? workflowName : "/";
    var normalizedPath = path.startsWith("//") ? path.substring(1) : path;
    var fullPath =
        concatPath(
            getPath(),
            "/GetAssociatedFoldersJob/start/" + wfName,
            "/",
            normalizedPath,
            "?includeFoldersWithDifferentWorkflow=" + includeFoldersWithDifferentWorkflow);
    return GET(fullPath);
  }

  public PSGetAssignedFoldersJobStatus getAssociatedFoldersJobResults(String jobId) {
    getRequestHeaders().put("Accept", MediaType.APPLICATION_XML);
    return getObjectFromPath(
        concatPath(getPath(), "GetAssociatedFoldersJob/status", jobId),
        PSGetAssignedFoldersJobStatus.class);
  }
}
