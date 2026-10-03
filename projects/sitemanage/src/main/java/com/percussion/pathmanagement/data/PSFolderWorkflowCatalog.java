/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflows Explorer may assign to a folder ({@code sys_workflowid}). Not the content-type
 * allow-list used by item Set workflow (#5104 / #5076).
 */
@XmlRootElement(name = "FolderWorkflowCatalog")
@JsonRootName("FolderWorkflowCatalog")
public class PSFolderWorkflowCatalog extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private ArrayList<PSFolderWorkflowChoice> choices = new ArrayList<>();

  public List<PSFolderWorkflowChoice> getChoices() {
    return choices;
  }

  public void setChoices(List<PSFolderWorkflowChoice> choices) {
    this.choices = choices == null ? new ArrayList<>() : new ArrayList<>(choices);
  }
}
