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

import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a query for page visits, including section path, promoted page paths, limit, time period, and sort order.
 */
@XmlRootElement(name = "visitQuery")
public class PSVisitQuery {

    private String sectionPath;
    private String promotedPagePaths;
    private String limit;
    private String timePeriod;
    private String sortOrder;

    public PSVisitQuery() {}

    private PSVisitQuery(Builder builder) {
        this.sectionPath = builder.sectionPath;
        this.promotedPagePaths = builder.promotedPagePaths;
        this.limit = builder.limit;
        this.timePeriod = builder.timePeriod;
        this.sortOrder = builder.sortOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sectionPath;
        private String promotedPagePaths;
        private String limit;
        private String timePeriod;
        private String sortOrder;

        public Builder sectionPath(String sectionPath) {
            this.sectionPath = sectionPath;
            return this;
        }

        public Builder promotedPagePaths(String promotedPagePaths) {
            this.promotedPagePaths = promotedPagePaths;
            return this;
        }

        public Builder limit(String limit) {
            this.limit = limit;
            return this;
        }

        public Builder timePeriod(String timePeriod) {
            this.timePeriod = timePeriod;
            return this;
        }

        public Builder sortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public PSVisitQuery build() {
            return new PSVisitQuery(this);
        }
    }

    public Optional<String> getTimePeriod() {
        return Optional.ofNullable(timePeriod);
    }

    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Optional<String> getSectionPath() {
        return Optional.ofNullable(sectionPath);
    }

    public void setSectionPath(String sectionPath) {
        this.sectionPath = sectionPath;
    }

    public Optional<String> getPromotedPagePaths() {
        return Optional.ofNullable(promotedPagePaths);
    }

    public void setPromotedPagePaths(String promotedPagePaths) {
        this.promotedPagePaths = promotedPagePaths;
    }

    public Optional<String> getLimit() {
        return Optional.ofNullable(limit);
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public Optional<String> getSortOrder() {
        return Optional.ofNullable(sortOrder);
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
