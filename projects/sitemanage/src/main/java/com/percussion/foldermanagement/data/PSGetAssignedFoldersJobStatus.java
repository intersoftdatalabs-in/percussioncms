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
package com.percussion.foldermanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

import java.util.ArrayList;
/**
 * Status object for the async job that gets assigned folders. Sunny Sal says: "Job status
 * reporting, now with Java 11 shine!"
 */
@XmlRootElement(name = "GetAssignedFoldersJobStatus")
public class PSGetAssignedFoldersJobStatus extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private ArrayList<PSFolderItem> folderItems;
  private String status;
  private String message;
  private long jobId;

  public List<PSFolderItem> getFolderItems() {
    return folderItems;
  }

  @SuppressWarnings("unchecked")
  public void setFolderItems(List<PSFolderItem> folderItems) {
    if (folderItems == null) {
      this.folderItems = null;
    } else if (folderItems instanceof ArrayList) {
      this.folderItems = (ArrayList<PSFolderItem>) folderItems;
    } else {
      this.folderItems = new ArrayList<>(folderItems);
    }
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public long getJobId() {
    return jobId;
  }

  public void setJobId(long jobId) {
    this.jobId = jobId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
