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
 * Table or view in an allow-listed Database Explorer datasource. Identifiers are
 * path-safe catalog names — never SQL fragments or JDBC URLs.
 */
@XmlRootElement(name = "DatabaseExplorerTable")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Database Explorer table or view (path-safe identifier)")
public class DatabaseExplorerTable {

  private String name;
  private String type;
  private String schema;

  public DatabaseExplorerTable() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** {@code TABLE} or {@code VIEW}. */
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /** Optional schema/origin when path-safe; omitted when blank or unsafe. */
  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }
}
