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

package com.percussion.rest.views;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Optional overrides for {@code POST /rest/views/{idOrName}/execute}.
 *
 * <p>v1 does not accept a full field-criteria rewrite — design operators live on the loaded view
 * ({@code PSSearch} of type view). Clients may only scope, page, and sort.
 *
 * <p>Peer of {@code SearchExecuteRequest} in the searches catalog; this type is separate so Views
 * and Searches stay distinct public contracts.
 */
@XmlRootElement(name = "ViewExecuteRequest")
@JsonRootName("ViewExecuteRequest")
@Schema(description = "Optional overrides when executing a CX design view by id or name")
public class ViewExecuteRequest {

  private String folderPath;
  private Integer startIndex;
  private Integer maxResults;
  private String sortColumn;
  private String sortOrder;

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  public Integer getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(Integer startIndex) {
    this.startIndex = startIndex;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  public String getSortColumn() {
    return sortColumn;
  }

  public void setSortColumn(String sortColumn) {
    this.sortColumn = sortColumn;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }
}
