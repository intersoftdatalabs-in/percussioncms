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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Immutable container for page summaries, used for JSON/XML serialization.
 */
@XmlRootElement(name = "pageSummaries")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"summaries"})
public final class PSPageSummaries {
    @XmlElement(required = true)
    private final List<PSPageSummary> summaries;

    /**
     * Creates a new instance with the given summaries.
     * @param summaries the page summaries, must not be null
     * @throws NullPointerException if summaries is null
     */
    public PSPageSummaries(List<PSPageSummary> summaries) {
        this.summaries = List.copyOf(Objects.requireNonNull(summaries, "summaries must not be null"));
    }

    /**
     * Default constructor for JAXB.
     */
    protected PSPageSummaries() {
        this.summaries = Collections.emptyList();
    }

    /**
     * @return an unmodifiable view of the page summaries, never null
     */
    public List<PSPageSummary> getSummaries() {
        return summaries;
    }

    /**
     * @return true if there are no summaries
     */
    public boolean isEmpty() {
        return summaries.isEmpty();
    }

    /**
     * @return the number of summaries
     */
    public int size() {
        return summaries.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSPageSummaries)) return false;
        PSPageSummaries that = (PSPageSummaries) o;
        return summaries.equals(that.summaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summaries);
    }

    @Override
    public String toString() {
        return String.format("PSPageSummaries{size=%d}", summaries.size());
    }
}
