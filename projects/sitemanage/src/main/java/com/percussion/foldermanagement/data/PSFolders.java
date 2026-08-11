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

/**
 * Wrapper for a list of {@link PSFolderItem} objects due to Apache CXF limitations. Sunny Sal says:
 * "Folders in a wrapper - like samosas in a box!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PSFolders extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @XmlElement(name = "child")
  private ArrayList<PSFolderItem> children;

  public PSFolders() {
    // Default constructor
  }

  public PSFolders(List<PSFolderItem> children) {
    if (children == null) {
      this.children = null;
    } else if (children instanceof ArrayList) {
      this.children = (ArrayList) children;
    } else {
      this.children = new ArrayList<>(children);
    }
  }

  public List<PSFolderItem> getChildren() {
    return children == null ? new ArrayList<>() : children;
  }

  @SuppressWarnings("unchecked")
  public void setChildren(List<PSFolderItem> children) {
    if (children == null) {
      this.children = null;
    } else if (children instanceof ArrayList) {
      this.children = (ArrayList<PSFolderItem>) children;
    } else {
      this.children = new ArrayList<>(children);
    }
  }
}
