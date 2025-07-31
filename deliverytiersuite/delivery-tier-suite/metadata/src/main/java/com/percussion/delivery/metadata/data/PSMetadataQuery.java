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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;

/**
 * Represents a query made against the metadata service.
 * Supports paging, sorting, and filtering.
 *
 * @author erikserating
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PSMetadataQuery {

    private boolean editMode;
    private boolean trackBlogPost;
    private String blogPostFullPath;
    private List<String> criteria;
    private int maxResults;
    private int totalMaxResults;
    private int startIndex;
    private String orderBy;
    private boolean returnTotalEntries = false;
    private String pagingPagesText;
    private String sortTagsBy;
    private String currentPageId;

    public static final String FIELD_CRITERIA = "criteria";
    public static final String FIELD_MAX_RESULTS = "maxResults";
    public static final String FIELD_TOTAL_MAX_RESULTS = "totalMaxResults";
    public static final String FIELD_START_INDEX = "startIndex";
    public static final String FIELD_ORDER_BY = "orderBy";
    public static final String FIELD_RETURN_TOTAL_ENTRIES = "returnTotalEntries";
    public static final String FIELD_PAGING_PAGES_TEXT = "pagingPagesText";

    public PSMetadataQuery() {}

    /**
     * Sets the query criteria list.
     *
     * @param criteria the criteria string list. May be null or empty.
     */
    public void setCriteria(List<String> criteria) {
        this.criteria = criteria;
    }

    /**
     * Sets the orderBy clause.
     *
     * @param orderBy orderBy string, may be null or empty.
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * Sets the maxResults value.
     *
     * @param max an integer greater than zero.
     */
    public void setMaxResults(int max) {
        maxResults = max;
    }

    /**
     * Sets the startIndex value.
     *
     * @param start an integer zero or more.
     */
    public void setStartIndex(int start) {
        startIndex = start;
    }

    /**
     * Sets the returnTotalEntries value.
     *
     * @param returnTE specifies if totalEntries field should be set in response.
     */
    public void setReturnTotalEntries(boolean returnTE) {
        returnTotalEntries = returnTE;
    }

    /**
     * Sets the pagination label.
     *
     * @param pagingPagesText the pagination label.
     */
    public void setPagingPagesText(String pagingPagesText) {
        this.pagingPagesText = pagingPagesText;
    }

    /**
     * @return the criteria
     */
    public Optional<List<String>> getCriteria() {
        return Optional.ofNullable(criteria);
    }

    /**
     * @return the maxResults
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * @return the startIndex
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return the orderBy
     */
    public Optional<String> getOrderBy() {
        return Optional.ofNullable(orderBy);
    }

    /**
     * @return the returnTotalEntries
     */
    public boolean getReturnTotalEntries() {
        return returnTotalEntries;
    }

    /**
     * @return the pagination label
     */
    public Optional<String> getPagingPagesText() {
        return Optional.ofNullable(pagingPagesText);
    }

    public boolean isTrackBlogPost() {
        return trackBlogPost;
    }

    public void setTrackBlogPost(boolean trackBlogPost) {
        this.trackBlogPost = trackBlogPost;
    }

    public Optional<String> getBlogPostFullPath() {
        return Optional.ofNullable(blogPostFullPath);
    }

    public void setBlogPostFullPath(String blogPostFullPath) {
        this.blogPostFullPath = blogPostFullPath;
    }

    public int getTotalMaxResults() {
        return totalMaxResults;
    }

    public void setTotalMaxResults(int totalMaxResults) {
        this.totalMaxResults = totalMaxResults;
    }

    public Optional<String> getSortTagsBy() {
        return Optional.ofNullable(sortTagsBy);
    }

    public void setSortTagsBy(String sortTagsBy) {
        this.sortTagsBy = sortTagsBy;
    }

    public Optional<String> getCurrentPageId() {
        return Optional.ofNullable(currentPageId);
    }

    public void setCurrentPageId(String currentPageId) {
        this.currentPageId = currentPageId;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
}
