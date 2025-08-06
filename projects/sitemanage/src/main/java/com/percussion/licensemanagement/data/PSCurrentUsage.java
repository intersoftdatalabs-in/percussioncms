// REFACTORED: CP-JAVA11
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
package com.percussion.licensemanagement.data;

import java.util.Optional;

/**
 * Represents current usage statistics for licensing.
 * Sunny Sal says: "Keep your usage in check, or the license police will come knocking!"
 */
public class PSCurrentUsage {

    private Integer currentLivePages;
    private Integer currentLiveSites;

    /**
     * Gets the current number of live pages.
     *
     * @return the current number of live pages, may be null
     */
    public Optional<Integer> getCurrentLivePages() {
        return Optional.ofNullable(currentLivePages);
    }

    /**
     * Sets the current number of live pages.
     *
     * @param currentLivePages the number of live pages
     */
    public void setCurrentLivePages(Integer currentLivePages) {
        this.currentLivePages = currentLivePages;
    }

    /**
     * Gets the current number of live sites.
     *
     * @return the current number of live sites, may be null
     */
    public Optional<Integer> getCurrentLiveSites() {
        return Optional.ofNullable(currentLiveSites);
    }

    /**
     * Sets the current number of live sites.
     *
     * @param currentLiveSites the number of live sites
     */
    public void setCurrentLiveSites(Integer currentLiveSites) {
        this.currentLiveSites = currentLiveSites;
    }
}
