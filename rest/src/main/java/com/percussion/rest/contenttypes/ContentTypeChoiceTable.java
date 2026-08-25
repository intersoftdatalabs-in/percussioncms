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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Table-backed choice source for a field control (CD-07 {@code tableinfo}). */
@XmlRootElement(name = "ContentTypeChoiceTable")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SQL table choice source (datasource, table, label/value columns)")
public class ContentTypeChoiceTable {

  @Schema(description = "Datasource name; empty uses the default CMS datasource")
  private String dataSource;

  @Schema(required = true, description = "Table name")
  private String tableName;

  @Schema(required = true, description = "Label column")
  private String labelColumn;

  @Schema(required = true, description = "Value column")
  private String valueColumn;

  public ContentTypeChoiceTable() {}

  public String getDataSource() {
    return dataSource;
  }

  public void setDataSource(String dataSource) {
    this.dataSource = dataSource;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getLabelColumn() {
    return labelColumn;
  }

  public void setLabelColumn(String labelColumn) {
    this.labelColumn = labelColumn;
  }

  public String getValueColumn() {
    return valueColumn;
  }

  public void setValueColumn(String valueColumn) {
    this.valueColumn = valueColumn;
  }
}
