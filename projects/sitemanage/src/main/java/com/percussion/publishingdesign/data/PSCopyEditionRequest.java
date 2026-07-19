/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.publishingdesign.data;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "copyEditionRequest")
public class PSCopyEditionRequest {
  private String sourceEditionId;
  private String targetSiteId;
  private String newName;
  private boolean copyContentLists;

  public String getSourceEditionId() {
    return sourceEditionId;
  }

  public void setSourceEditionId(String sourceEditionId) {
    this.sourceEditionId = sourceEditionId;
  }

  public String getTargetSiteId() {
    return targetSiteId;
  }

  public void setTargetSiteId(String targetSiteId) {
    this.targetSiteId = targetSiteId;
  }

  public String getNewName() {
    return newName;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }

  public boolean isCopyContentLists() {
    return copyContentLists;
  }

  public void setCopyContentLists(boolean copyContentLists) {
    this.copyContentLists = copyContentLists;
  }
}
