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
package com.percussion.pagemanagement.assembler;

/**
 * Represents a month in a blog year, with a count of entries.
 */
public class PSBlogMonth {

    private String month;
    private Integer count;

    /**
     * Constructs a blog month with the specified name and count.
     * @param month the month name
     * @param count the entry count
     */
    public PSBlogMonth(String month, Integer count) {
        this.month = month;
        this.count = count;
    }

    /**
     * Gets the month name.
     * @return the month name
     */
    public String getMonth() {
        return month;
    }

    /**
     * Sets the month name.
     * @param month the month to set
     */
    public void setMonth(String month) {
        this.month = month;
    }

    /**
     * Gets the entry count for the month.
     * @return the count
     */
    public Integer getCount() {
        return count;
    }

    /**
     * Sets the entry count for the month.
     * @param count the number of entries to set
     */
    public void setCount(Integer count) {
        this.count = count;
    }
}
