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
package com.percussion.publishingdesign.data;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "demandPublishRequest")
public class PSDemandPublishRequest {
  /** Content item ids (legacy numeric or string locators). */
  private List<String> contentIds = new ArrayList<>();

  /**
   * Optional folder id per item; if shorter than contentIds, remaining items resolve folder via
   * content parent lookup when content WS is available.
   */
  private List<String> folderIds = new ArrayList<>();

  public List<String> getContentIds() {
    return contentIds;
  }

  public void setContentIds(List<String> contentIds) {
    this.contentIds = contentIds != null ? contentIds : new ArrayList<>();
  }

  public List<String> getFolderIds() {
    return folderIds;
  }

  public void setFolderIds(List<String> folderIds) {
    this.folderIds = folderIds != null ? folderIds : new ArrayList<>();
  }
}
