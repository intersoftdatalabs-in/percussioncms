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
package com.percussion.rest.explorer;

import java.util.ArrayList;
import java.util.List;

/** Session column overlay for one Explorer folder list (#4722). */
public class ExplorerListColumns {

  private String folderPath;
  private List<String> columns = new ArrayList<>();

  public ExplorerListColumns() {}

  public ExplorerListColumns(String folderPath, List<String> columns) {
    this.folderPath = folderPath;
    if (columns != null) {
      this.columns = new ArrayList<>(columns);
    }
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
  }
}
