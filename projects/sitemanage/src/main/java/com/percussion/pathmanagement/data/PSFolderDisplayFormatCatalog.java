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
 * Display formats Explorer may store on a folder as {@code sys_displayformat} (the id, not the
 * name). Not the list-column chooser (#4722) and not a page or asset format (#5131).
 */
@XmlRootElement(name = "FolderDisplayFormatCatalog")
@JsonRootName("FolderDisplayFormatCatalog")
public class PSFolderDisplayFormatCatalog extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private ArrayList<PSFolderDisplayFormatChoice> choices = new ArrayList<>();

  public List<PSFolderDisplayFormatChoice> getChoices() {
    return choices;
  }

  public void setChoices(List<PSFolderDisplayFormatChoice> choices) {
    this.choices = choices == null ? new ArrayList<>() : new ArrayList<>(choices);
  }
}
