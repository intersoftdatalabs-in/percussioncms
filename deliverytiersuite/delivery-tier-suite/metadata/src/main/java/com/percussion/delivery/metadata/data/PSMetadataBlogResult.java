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

/**
 * Represents the result of a blog entry query, including previous, current, and next entries.
 */
public class PSMetadataBlogResult {

    private PSMetadataRestEntry previous;
    private PSMetadataRestEntry current;
    private PSMetadataRestEntry next;

    /**
     * Returns the previous blog entry.
     *
     * @return the previous entry.
     */
    public PSMetadataRestEntry getPrevious() {
        return previous;
    }

    /**
     * Sets the previous blog entry.
     *
     * @param previous the previous entry to set.
     */
    public void setPrevious(PSMetadataRestEntry previous) {
        this.previous = previous;
    }

    /**
     * Returns the current blog entry.
     *
     * @return the current entry.
     */
    public PSMetadataRestEntry getCurrent() {
        return current;
    }

    /**
     * Sets the current blog entry.
     *
     * @param current the current entry to set.
     */
    public void setCurrent(PSMetadataRestEntry current) {
        this.current = current;
    }

    /**
     * Returns the next blog entry.
     *
     * @return the next entry.
     */
    public PSMetadataRestEntry getNext() {
        return next;
    }

    /**
     * Sets the next blog entry.
     *
     * @param next the next entry to set.
     */
    public void setNext(PSMetadataRestEntry next) {
        this.next = next;
    }
}
