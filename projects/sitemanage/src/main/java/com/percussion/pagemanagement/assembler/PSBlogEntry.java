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
package com.percussion.pagemanagement.assembler;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a blog entry containing a set of years.
 */
public class PSBlogEntry {

    private Set<PSBlogYear> years;

    public PSBlogEntry() {
        this.years = new HashSet<>();
    }

    /**
     * Gets the years for this blog entry.
     * @return the years set, never {@code null}
     */
    public Set<PSBlogYear> getYears() {
        return years;
    }

    /**
     * Sets the years for this blog entry.
     * @param years the years to set
     */
    public void setYears(Set<PSBlogYear> years) {
        this.years = years;
    }
}
