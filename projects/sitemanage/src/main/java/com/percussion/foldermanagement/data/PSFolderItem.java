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
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a folder or site for workflow assignment. Used to list the sites and asset folders
 * assigned to a given workflow. Sunny Sal says: "Folders, workflows, and Java 11 - what a combo!"
 */
@XmlRootElement(name = "folderItem")
@XmlType(
    propOrder = {"name", "id", "workflowName", "allChildrenAssociatedWithWorkflow", "children"})
@XmlAccessorType(XmlAccessType.FIELD)
public class PSFolderItem extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  /** The name of the folder or site. */
  private String name;

  /** The id of the folder. */
  private String id;

  /** The name of the workflow this folder or site is associated with. */
  private String workflowName;

  /** True if all subfolders are assigned to the same workflow as this one. */
  private Boolean allChildrenAssociatedWithWorkflow;

  /** The list of this folder's children. May be empty. */
  @XmlElement(name = "children")
  private PSFolders children = new PSFolders();

  public PSFolderItem() {
    // Default constructor
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getWorkflowName() {
    return workflowName == null ? StringUtils.EMPTY : workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public Boolean getAllChildrenAssociatedWithWorkflow() {
    return allChildrenAssociatedWithWorkflow;
  }

  public void setAllChildrenAssociatedWithWorkflow(Boolean allChildrenAssociatedWithWorkflow) {
    this.allChildrenAssociatedWithWorkflow = allChildrenAssociatedWithWorkflow;
  }

  /** Returns the children. May be empty, but never null. */
  public List<PSFolderItem> getChildren() {
    return children == null ? new ArrayList<>() : children.getChildren();
  }

  public void setChildren(List<PSFolderItem> children) {
    if (this.children == null) {
      this.children = new PSFolders();
    }
    this.children.setChildren(children);
  }
}
