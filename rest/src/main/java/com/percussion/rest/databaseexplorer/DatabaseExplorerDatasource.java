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

package com.percussion.rest.databaseexplorer;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Allow-listed Database Explorer datasource (Workbench §12.2 browse). The catalog
 * id is the API key — JDBC URLs and credentials are never returned on the wire.
 */
@XmlRootElement(name = "DatabaseExplorerDatasource")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allow-listed Database Explorer datasource (id only; no JDBC URL)")
public class DatabaseExplorerDatasource {

  private String id;
  private String displayName;
  private Boolean repository;
  private Boolean available;

  public DatabaseExplorerDatasource() {}

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

  /** Whether this catalog id maps to the CMS repository datasource. */
  public Boolean getRepository() {
    return repository;
  }

  public void setRepository(Boolean repository) {
    this.repository = repository;
  }

  /** Whether a JDBC catalog connection can currently be opened. */
  public Boolean getAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }
}
