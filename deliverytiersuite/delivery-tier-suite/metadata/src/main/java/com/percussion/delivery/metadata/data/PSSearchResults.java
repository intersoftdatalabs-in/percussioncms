// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
import java.util.Optional;

/**
 * Represents the total count of entries and a list of PSMetadataRestEntry objects for the requested
 * page number and page size.
 *
 * @author radharanisonnathi
 */
public class PSSearchResults {

    private Integer totalEntries;
    private List<PSMetadataRestEntry> resultEntries;

    public PSSearchResults() {}

    public PSSearchResults(Integer totalEntries, List<PSMetadataRestEntry> resultEntries) {
        this.totalEntries = totalEntries;
        this.resultEntries = resultEntries;
    }

    /**
     * Returns the result entries for the search.
     *
     * @return the result entries list, may be null.
     */
    public Optional<List<PSMetadataRestEntry>> getResults() {
        return Optional.ofNullable(resultEntries);
    }

    /**
     * Sets the result entries for the search.
     *
     * @param resultEntries the result entries to set.
     */
    public void setResults(List<PSMetadataRestEntry> resultEntries) {
        this.resultEntries = resultEntries;
    }

    /**
     * Returns the total number of entries after the search.
     *
     * @return total entries, may be null.
     */
    public Optional<Integer> getTotalEntries() {
        return Optional.ofNullable(totalEntries);
    }

    /**
     * Sets the total number of entries after the search.
     *
     * @param totalEntries total entries to set.
     */
    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }
}
