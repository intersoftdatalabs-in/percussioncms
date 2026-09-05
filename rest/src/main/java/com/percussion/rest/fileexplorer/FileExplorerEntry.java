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

package com.percussion.rest.fileexplorer;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Child of an allow-listed File Explorer root. {@code relativePath} is {@code /}-separated under
 * the root id — never an absolute, drive, or UNC filesystem path.
 */
@XmlRootElement(name = "FileExplorerEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "File Explorer child entry (relative path under an allow-listed root)")
public class FileExplorerEntry {

  private String name;
  private String relativePath;
  private Boolean directory;
  private Long size;

  public FileExplorerEntry() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public Boolean getDirectory() {
    return directory;
  }

  public void setDirectory(Boolean directory) {
    this.directory = directory;
  }

  /** Byte size for regular files; omitted for directories. */
  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }
}
