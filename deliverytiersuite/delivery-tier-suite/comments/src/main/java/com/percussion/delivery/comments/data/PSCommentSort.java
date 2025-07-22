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
package com.percussion.delivery.comments.data;

import java.util.Objects;

/**
 * Immutable class representing sort configuration for comment queries.
 */
public final class PSCommentSort {
    private final SORTBY sortby;
    private final boolean ascending;

    /**
     * Creates a new sort configuration.
     *
     * @param sortby sort by option, must not be null
     * @param ascending true for ascending sort order, false for descending
     * @throws NullPointerException if sortby is null
     */
    public PSCommentSort(SORTBY sortby, boolean ascending) {
        this.sortby = Objects.requireNonNull(sortby, "sortby cannot be null");
        this.ascending = ascending;
    }

    /**
     * Creates a new ascending sort configuration.
     *
     * @param sortby sort by option, must not be null
     * @throws NullPointerException if sortby is null
     */
    public static PSCommentSort ascending(SORTBY sortby) {
        return new PSCommentSort(sortby, true);
    }

    /**
     * Creates a new descending sort configuration.
     *
     * @param sortby sort by option, must not be null
     * @throws NullPointerException if sortby is null
     */
    public static PSCommentSort descending(SORTBY sortby) {
        return new PSCommentSort(sortby, false);
    }

    /**
     * @return the sort by field, never null
     */
    public SORTBY getSortby() {
        return sortby;
    }

    /**
     * @return true if ascending order, false if descending
     */
    public boolean isAscending() {
        return ascending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSCommentSort)) return false;
        PSCommentSort that = (PSCommentSort) o;
        return ascending == that.ascending && sortby == that.sortby;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortby, ascending);
    }

    @Override
    public String toString() {
        return String.format("PSCommentSort{sortby=%s, %s}",
            sortby, ascending ? "ascending" : "descending");
    }

    /**
     * Enumeration of sort field options.
     */
    public enum SORTBY
    {
        CREATEDDATE,
        EMAIL,
        USERNAME
    }

}
