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
package com.percussion.pso.restservice.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * REST model for folder metadata and ACL.
 */
@XmlRootElement
public class FolderInfo {

  /**
   * Creates a new FolderInfo.
   */
  public FolderInfo() {
    // default
  }

  private FolderAcl folderAcl;
  private String pubFileName;
  private String globalTemplate;

  List<ItemRef> folderItems;
  /**
   * Returns the FolderItems.
   *
   * @return the value
   */

  @XmlElementWrapper(name = "Contents")
  @XmlElement(name = "Item")
  public List<ItemRef> getFolderItems() {
    return folderItems;
  }

  /**
   * Sets the FolderItems.
   * @param folderItems the folder items
   */
  public void setFolderItems(List<ItemRef> folderItems) {
    this.folderItems = folderItems;
  }

  /**
   * Sets the FolderAcl.
   * @param folderAcl the folder acl
   */
  public void setFolderAcl(FolderAcl folderAcl) {
    this.folderAcl = folderAcl;
  }
  /**
   * Returns the FolderAcl.
   *
   * @return the value
   */

  @XmlElement
  public FolderAcl getFolderAcl() {
    return folderAcl;
  }

  /**
   * Sets the PubFileName.
   * @param pubFileName the pub file name
   */
  public void setPubFileName(String pubFileName) {
    this.pubFileName = pubFileName;
  }
  /**
   * Returns the PubFileName.
   *
   * @return the value
   */

  @XmlAttribute
  public String getPubFileName() {
    return pubFileName;
  }

  /**
   * Sets the GlobalTemplate.
   * @param globalTemplate the global template
   */
  public void setGlobalTemplate(String globalTemplate) {
    this.globalTemplate = globalTemplate;
  }
  /**
   * Returns the GlobalTemplate.
   *
   * @return the value
   */

  @XmlAttribute
  public String getGlobalTemplate() {
    return globalTemplate;
  }
}
