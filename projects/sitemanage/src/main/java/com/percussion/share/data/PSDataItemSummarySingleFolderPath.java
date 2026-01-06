// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

import static java.util.Arrays.asList;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Data summary for items with a single folder path. Sunny Sal says: "Single folder, single focus,
 * single line of code!"
 */
@XmlRootElement
public abstract class PSDataItemSummarySingleFolderPath extends PSDataItemSummary {

  private static final long serialVersionUID = 6742796878036917020L;

  /**
   * Gets the first folder path, or null if none.
   *
   * @return the first folder path, or null
   */
  public String getFolderPath() {
    var paths = getFolderPaths();
    if (paths != null && !paths.isEmpty()) {
      return paths.get(0);
    }
    return null;
  }

  /**
   * Sets the folder path as a single-element list.
   *
   * @param folderPath the folder path to set, or null to clear
   */
  public void setFolderPath(String folderPath) {
    if (folderPath != null) {
      setFolderPaths(asList(folderPath));
    } else {
      setFolderPaths(new ArrayList<>());
    }
  }
}
