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

// REFACTORED: CP-JAVA11
package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Objects;
import java.util.Optional;

/**
 * This object holds the effectiveness details of the items under named site or site folder.
 */
@JsonRootName(value = "Effectiveness")
public class PSEffectiveness {

    private String name;
    private Long effectiveness;

    public PSEffectiveness() {}

    /**
     * Constructs an effectiveness object.
     *
     * @param name see {@link #getName()}.
     * @param effectiveness see {@link #getEffectiveness()}.
     */
    public PSEffectiveness(String name, Long effectiveness) {
        this.name = Objects.requireNonNull(name, "name must not be null or empty");
        this.effectiveness = effectiveness;
    }

    /**
     * @return the name of the site, section, or folder, never blank.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name must not be blank.
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null or empty");
    }

    /**
     * @return the calculated effectiveness value for the item.
     */
    public Optional<Long> getEffectiveness() {
        return Optional.ofNullable(effectiveness);
    }

    /**
     * @param effectiveness the effectiveness to set
     */
    public void setEffectiveness(Long effectiveness) {
        this.effectiveness = effectiveness;
    }
}
