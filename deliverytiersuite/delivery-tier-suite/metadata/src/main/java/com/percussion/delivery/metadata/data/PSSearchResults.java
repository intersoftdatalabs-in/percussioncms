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

package com.percussion.delivery.metadata.data;

import java.util.List;

/**
 * Represents count for total entries and list of {@link PSMetadataRestEntry} objects for requested
 * page number and page size. Returned by the {@code /metadata/get} REST endpoint.
 *
 * @author radharanisonnathi
 */
public class PSSearchResults {
  private Integer totalEntries;
  private List<PSMetadataRestEntry> resultEntries;

  /** No-arg constructor required by the JSON binding layer. */
  public PSSearchResults() {}

  /**
   * Returns the list of result entries for the requested page.
   *
   * @return the results, may be <code>null</code>.
   */
  public List<PSMetadataRestEntry> getResults() {
    return resultEntries;
  }

  /**
   * Replaces the result entries.
   *
   * @param resultEntries the results to set; may be <code>null</code>.
   */
  public void setResults(List<PSMetadataRestEntry> resultEntries) {
    this.resultEntries = resultEntries;
  }

  /**
   * Returns the total number of entries matched by the search.
   *
   * @return the total number of entries after the search, may be <code>null</code>.
   */
  public Integer getTotalEntries() {
    return totalEntries;
  }

  /**
   * Sets the total number of entries matched by the search.
   *
   * @param totalEntries the total entries to set; may be <code>null</code>.
   */
  public void setTotalEntries(Integer totalEntries) {
    this.totalEntries = totalEntries;
  }
}
