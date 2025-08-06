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

package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSItemProperties;
import java.util.Optional;

/**
 * Holds traffic details for items under a named site by date.
 */
@JsonRootName(value = "TrafficDetails")
public class PSTrafficDetails extends PSItemProperties {

    private int visits;
    private int visitsDelta;

    public PSTrafficDetails() {
        // Default constructor
    }

    /**
     * Gets the total number of visits for this page.
     *
     * @return Optional containing visits count, or empty if not set.
     */
    public Optional<Integer> getVisits() {
        return Optional.of(visits);
    }

    /**
     * Gets the delta of visits for this item.
     *
     * @return Optional containing visits delta, or empty if not set.
     */
    public Optional<Integer> getVisitsDelta() {
        return Optional.of(visitsDelta);
    }

    /**
     * Sets the delta of visits for this item.
     *
     * @param visitsDelta the delta of visits
     */
    public void setVisitsDelta(int visitsDelta) {
        this.visitsDelta = visitsDelta;
    }

    /**
     * Sets the total number of visits for this page.
     *
     * @param visits the total visits
     */
    public void setVisits(int visits) {
        this.visits = visits;
    }
}
