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
 * Communities Explorer may assign to a folder. Not the item allow-list used by Set community on a
 * page or asset (#5105 / #5077).
 */
@XmlRootElement(name = "FolderCommunityCatalog")
@JsonRootName("FolderCommunityCatalog")
public class PSFolderCommunityCatalog extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private ArrayList<PSFolderCommunityChoice> choices = new ArrayList<>();

  public List<PSFolderCommunityChoice> getChoices() {
    return choices;
  }

  public void setChoices(List<PSFolderCommunityChoice> choices) {
    this.choices = choices == null ? new ArrayList<>() : new ArrayList<>(choices);
  }
}
