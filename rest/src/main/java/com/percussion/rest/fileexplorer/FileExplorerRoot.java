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
 * Allow-listed File Explorer root (Workbench §12.1 browse). The catalog id is the API key —
 * filesystem paths are never returned on the wire.
 */
@XmlRootElement(name = "FileExplorerRoot")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allow-listed File Explorer root (id only; no filesystem path)")
public class FileExplorerRoot {

  private String id;
  private String displayName;
  private Boolean exists;

  public FileExplorerRoot() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** Whether the configured directory currently exists on the server. */
  public Boolean getExists() {
    return exists;
  }

  public void setExists(Boolean exists) {
    this.exists = exists;
  }
}
