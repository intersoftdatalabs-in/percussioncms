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

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Criteria for filtering and sorting comments.
 * Uses builder pattern for flexible construction.
 */
public class PSCommentCriteria {
    private static final Logger log = LogManager.getLogger(PSCommentCriteria.class);

    private final String pagePath;
    private final String username;
    private final String tag;
    private final PSCommentSort sort;
    private final APPROVAL_STATE state;
    private final String site;
    private final Boolean moderated;
    private final Boolean viewed;
    private final int maxResults;
    private final int startIndex;
    private final String lastCommentId;
    private final String sortBy;
    private final boolean ascending;
    private final String callback;

    private PSCommentCriteria(Builder builder) {
        this.pagePath = builder.pagePath;
        this.username = builder.username;
        this.tag = builder.tag;
        this.sort = builder.sort;
        this.state = builder.state;
        this.site = builder.site;
        this.moderated = builder.moderated;
        this.viewed = builder.viewed;
        this.maxResults = builder.maxResults;
        this.startIndex = builder.startIndex;
        this.lastCommentId = builder.lastCommentId;
        this.sortBy = builder.sortBy;
        this.ascending = builder.ascending;
        this.callback = builder.callback;
    }

    // Getters using Optional for nullable fields
    public Optional<String> getPagePath() { return Optional.ofNullable(pagePath); }
    public Optional<String> getUsername() { return Optional.ofNullable(username); }
    public Optional<String> getTag() { return Optional.ofNullable(tag); }
    public Optional<PSCommentSort> getSort() { return Optional.ofNullable(sort); }
    public Optional<APPROVAL_STATE> getState() { return Optional.ofNullable(state); }
    public Optional<String> getSite() { return Optional.ofNullable(site); }
    public Optional<Boolean> getModerated() { return Optional.ofNullable(moderated); }
    public Optional<Boolean> getViewed() { return Optional.ofNullable(viewed); }
    public Optional<String> getLastCommentId() { return Optional.ofNullable(lastCommentId); }
    public Optional<String> getSortBy() { return Optional.ofNullable(sortBy); }
    public Optional<String> getCallback() { return Optional.ofNullable(callback); }

    // Non-nullable fields with defaults
    public int getMaxResults() { return Math.max(0, maxResults); }
    public int getStartIndex() { return Math.max(0, startIndex); }
    public boolean isAscending() { return ascending; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String pagePath;
        private String username;
        private String tag;
        private PSCommentSort sort;
        private APPROVAL_STATE state;
        private String site;
        private Boolean moderated;
        private Boolean viewed;
        private int maxResults;
        private int startIndex;
        private String lastCommentId;
        private String sortBy;
        private boolean ascending = true;  // default value
        private String callback;

        public Builder pagePath(String val) { pagePath = val; return this; }
        public Builder username(String val) { username = val; return this; }
        public Builder tag(String val) { tag = val; return this; }
        public Builder sort(PSCommentSort val) { sort = val; return this; }
        public Builder state(APPROVAL_STATE val) { state = val; return this; }
        public Builder site(String val) { site = val; return this; }
        public Builder moderated(Boolean val) { moderated = val; return this; }
        public Builder viewed(Boolean val) { viewed = val; return this; }
        public Builder maxResults(int val) { maxResults = val; return this; }
        public Builder startIndex(int val) { startIndex = val; return this; }
        public Builder lastCommentId(String val) { lastCommentId = val; return this; }
        public Builder sortBy(String val) { sortBy = val; return this; }
        public Builder ascending(boolean val) { ascending = val; return this; }
        public Builder callback(String val) { callback = val; return this; }

        public PSCommentCriteria build() {
            return new PSCommentCriteria(this);
        }
    }
}
