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
package com.percussion.searchmanagement.data;

import java.util.Collections;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;

/** Encapsulates search criteria for content search operations. */
@XmlRootElement(name = "SearchCriteria")
@XmlAccessorType(XmlAccessType.FIELD)
public class PSSearchCriteria {

  private String query;
  private String searchType;
  private Integer startIndex;
  private Integer maxResults;
  private String sortColumn;
  private String sortOrder;
  private Integer formatId;
  private Map<String, String> searchFields;
  private String folderPath;

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Gets the fields to search on. Key is the field name, value is the search value.
   *
   * @return Unmodifiable map of fields, never null.
   */
  public Map<String, String> getSearchFields() {
    return searchFields == null
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(searchFields);
  }

  public void setSearchFields(Map<String, String> searchFields) {
    this.searchFields = searchFields;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
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

  /**
   * Sets the sort order. Accepts only "asc" or "desc" (case-insensitive). Defaults to "asc".
   *
   * @param sortOrder the sort order string
   */
  public void setSortOrder(String sortOrder) {
    if (StringUtils.isBlank(sortOrder)) {
      this.sortOrder = "asc";
    } else {
      var trimmed = sortOrder.trim().toLowerCase();
      this.sortOrder = (trimmed.equals("asc") || trimmed.equals("desc")) ? trimmed : "asc";
    }
  }

  public Integer getFormatId() {
    return formatId;
  }

  public void setFormatId(Integer formatId) {
    this.formatId = formatId;
  }

  public String getSearchType() {
    return searchType;
  }

  public void setSearchType(String searchType) {
    this.searchType = searchType;
  }

  /**
   * Determines if this criteria object has no meaningful criteria set.
   *
   * @return true if empty, false otherwise
   */
  public boolean isEmpty() {
    if (StringUtils.isNotBlank(query)) {
      return false;
    }
    if (StringUtils.isNotBlank(folderPath) && !folderPath.equalsIgnoreCase("//Sites/")) {
      return false;
    }
    if (StringUtils.isNotBlank(searchType)) {
      return false;
    }
    return true;
  }
}
