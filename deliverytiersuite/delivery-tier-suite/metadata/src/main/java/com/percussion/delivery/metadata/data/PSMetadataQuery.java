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
package com.percussion.delivery.metadata.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * An object that represents a query made against the metadata service.
 *
 * @author erikserating
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PSMetadataQuery {

  /** Indicates weather the call was made from the editor or preview vs published website */
  private boolean isEditMode;

  /** Indicates that the request should be tracked. */
  private boolean trackBlogPost;

  /***
   * Indicates the full path to the blog post
   */
  private String blogPostFullPath;

  /**
   * A list of query criteria that is used to limit the results of a query. The criteria will be put
   * together with AND's (OR is not supported).
   *
   * <pre>
   *  The following operators are supported:
   *  Equals:  =
   *  Not Equals: !=
   *  Greater Than: &gt;
   *  Less Than: &lt;
   *  Greater than or equal to: &gt;=
   *  Less than or equal to: &lt;=
   *  LIKE
   *  IN
   * </pre>
   */
  private List<String> criteria;

  /** The maximum number of results to return per page. */
  private int maxResults;

  /***
   * Sets the Query limit configured on the client.
   */
  private int totalMaxResults;

  /**
   * Indicates which record to start with in the returned result set, using maxResults and
   * startIndex together allow for paging.
   */
  private int startIndex;

  /** Property name and sort direction that specifies how results are sorted. */
  private String orderBy;

  /** Property name and sort direction that specifies how results are sorted. */
  private boolean returnTotalEntries = false;

  /** Pagination label to use, default is "pages". */
  private String pagingPagesText;

  private String sortTagsBy;

  private String currentPageId;

  /** JSON / Jackson field name for {@link #setCriteria(List)} / {@link #getCriteria()}. */
  public static final String FIELD_CRITERIA = "criteria";

  /** JSON / Jackson field name for {@link #setMaxResults(int)} / {@link #getMaxResults()}. */
  public static final String FIELD_MAX_RESULTS = "maxResults";

  /**
   * JSON / Jackson field name for {@link #setTotalMaxResults(int)} / {@link #getTotalMaxResults()}.
   */
  public static final String FIELD_TOTAL_MAX_RESULTS = "totalMaxResults";

  /** JSON / Jackson field name for {@link #setStartIndex(int)} / {@link #getStartIndex()}. */
  public static final String FIELD_START_INDEX = "startIndex";

  /** JSON / Jackson field name for {@link #setOrderBy(String)} / {@link #getOrderBy()}. */
  public static final String FIELD_ORDER_BY = "orderBy";

  /**
   * JSON / Jackson field name for {@link #setReturnTotalEntries(boolean)} / {@link
   * #getReturnTotalEntries()}.
   */
  public static final String FIELD_RETURN_TOTAL_ENTRIES = "returnTotalEntries";

  /**
   * JSON / Jackson field name for {@link #setPagingPagesText(String)} / {@link
   * #getPagingPagesText()}.
   */
  public static final String FIELD_PAGING_PAGES_TEXT = "pagingPagesText";

  /** Default no-arg constructor required by Jackson. */
  public PSMetadataQuery() {}

  /**
   * Sets the query criteria list.
   *
   * @param criteria the criteria string list. May be <code>null</code> or empty.
   */
  public void setCriteria(List<String> criteria) {
    this.criteria = criteria;
  }

  /**
   * Sets the orderBy clause.
   *
   * @param orderby orderby string list, may be <code>null</code> or empty.
   */
  public void setOrderBy(String orderby) {
    this.orderBy = orderby;
  }

  /**
   * Sets the maxResults's value.
   *
   * @param max an integer greater than zero.
   */
  public void setMaxResults(int max) {
    maxResults = max;
  }

  /**
   * Sets the startIndex's value.
   *
   * @param start an integer of zero or more.
   */
  public void setStartIndex(int start) {
    startIndex = start;
  }

  /**
   * Sets the returnTotalEntries's value.
   *
   * @param returnTE a boolean - specifies if totalEntries field should be set in response.
   */
  public void setReturnTotalEntries(boolean returnTE) {
    returnTotalEntries = returnTE;
  }

  /**
   * Sets the pagination label
   *
   * @param pagingPagesText - Our pagination Label.
   */
  public void setPagingPagesText(String pagingPagesText) {
    this.pagingPagesText = pagingPagesText;
  }

  /**
   * Returns the query criteria list.
   *
   * @return the criteria list, may be <code>null</code>.
   */
  public List<String> getCriteria() {
    return criteria;
  }

  /**
   * Returns the page-size used to drive pagination.
   *
   * @return the maxResults value.
   */
  public int getMaxResults() {
    return maxResults;
  }

  /**
   * Returns the zero-based start index for pagination.
   *
   * @return the startIndex value.
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * Returns the {@code ORDER BY} clause.
   *
   * @return the orderBy clause, may be <code>null</code>.
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * Returns whether the response should also carry the total number of matching entries.
   *
   * @return the returnTotalEntries flag.
   */
  public boolean getReturnTotalEntries() {
    return returnTotalEntries;
  }

  /**
   * Returns the pagination label used by the client to render the pager UI.
   *
   * @return the pagination label, may be <code>null</code>.
   */
  public String getPagingPagesText() {
    return pagingPagesText;
  }

  /**
   * Returns whether the request originates from a context where blog post visits should be tracked.
   *
   * @return the {@code trackBlogPost} flag.
   */
  public boolean isTrackBlogPost() {
    return trackBlogPost;
  }

  /**
   * Sets whether the request originates from a context where blog post visits should be tracked.
   *
   * @param trackBlogPost the {@code trackBlogPost} flag to set.
   */
  public void setTrackBlogPost(boolean trackBlogPost) {
    this.trackBlogPost = trackBlogPost;
  }

  /**
   * Returns the full path of the blog post being viewed, if any.
   *
   * @return the blog post full path, may be <code>null</code>.
   */
  public String getBlogPostFullPath() {
    return blogPostFullPath;
  }

  /**
   * Sets the full path of the blog post being viewed.
   *
   * @param blogPostFullPath the blog post full path to set; may be <code>null</code>.
   */
  public void setBlogPostFullPath(String blogPostFullPath) {
    this.blogPostFullPath = blogPostFullPath;
  }

  /**
   * Returns the configured query limit enforced by the client.
   *
   * @return the totalMaxResults value, never negative.
   */
  public int getTotalMaxResults() {
    return totalMaxResults;
  }

  /**
   * Sets the configured query limit enforced by the client.
   *
   * @param totalMaxResults the totalMaxResults value to set; may not be negative.
   */
  public void setTotalMaxResults(int totalMaxResults) {
    this.totalMaxResults = totalMaxResults;
  }

  /**
   * Returns the field used to sort the tag aggregation when no specific {@code orderBy} is
   * supplied.
   *
   * @return the sortTagsBy field name, may be <code>null</code>.
   */
  public String getSortTagsBy() {
    return sortTagsBy;
  }

  /**
   * Sets the field used to sort the tag aggregation when no specific {@code orderBy} is supplied.
   *
   * @param sortTagsBy the sortTagsBy field name to set; may be <code>null</code>.
   */
  public void setSortTagsBy(String sortTagsBy) {
    this.sortTagsBy = sortTagsBy;
  }

  /**
   * Returns the id of the current page within the paginated result set.
   *
   * @return the currentPageId, may be <code>null</code>.
   */
  public String getCurrentPageId() {
    return currentPageId;
  }

  /**
   * Sets the id of the current page within the paginated result set.
   *
   * @param currentPageId the currentPageId to set; may be <code>null</code>.
   */
  public void setCurrentPageId(String currentPageId) {
    this.currentPageId = currentPageId;
  }

  /**
   * Returns whether the query was made from the editor / preview vs the published website.
   *
   * @return the {@code editMode} flag.
   */
  public boolean isEditMode() {
    return isEditMode;
  }

  /**
   * Sets whether the query was made from the editor / preview vs the published website.
   *
   * @param editMode the {@code editMode} flag to set.
   */
  public void setEditMode(boolean editMode) {
    this.isEditMode = editMode;
  }
}
