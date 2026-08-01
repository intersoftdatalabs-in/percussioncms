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
package com.percussion.delivery.comments.data;

import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * A small data class representing comment criteria to find comments with.
 *
 * @author erikserating
 */
public class PSCommentCriteria {
  private static final Logger log = LogManager.getLogger(PSCommentCriteria.class);

  /** Default no-arg constructor required for bean-style instantiation and JSON deserialization. */
  public PSCommentCriteria() {}

  private String sortby;
  private String ascending;
  private String callback;

  private String pagepath;

  /** The user that created the comments to be returned. May be <code>null</code> or empty. */
  private String username;

  /** A tag string that exists on the comments to be returned. May be <code>null</code> or empty. */
  private String tag;

  /** Sort object specifying sort order for results. May be <code>null</code>. */
  private PSCommentSort sort;

  /**
   * The approval state to filter by. May be <code>null</code> in which case comments in any state
   * may be returned.
   */
  private APPROVAL_STATE state;

  /**
   * The name of the site to filter by. May be <code>null</code>, in which case comments from every
   * site will be returned.
   */
  private String site;

  /** Flag indicating that the comment has been actively moderated. May be <code>null</code>. */
  private Boolean moderated;

  /**
   * Flag indicating that the comment has been previously viewed by an admin or moderator. May be
   * <code>null</code>.
   */
  private Boolean viewed;

  /** The maximum number of results to return. If zero or less then all results will be returned. */
  private int maxResults;

  /**
   * The index offset of results returned, used for paging. If zero or less then start index will be
   * zero.
   */
  private int startIndex;

  /**
   * The id of the last comment added, used for returning the last comment added. If zero or less
   * then last comment Id will be zero.
   */
  private String lastCommentId;

  /**
   * Gets the page path filter.
   *
   * @return the pagepath
   */
  public String getPagepath() {
    return pagepath;
  }

  /**
   * Sets the page path filter.
   *
   * @param pagepath the pagepath to set
   */
  public void setPagepath(String pagepath) {
    this.pagepath = pagepath;
  }

  /**
   * Gets the username filter.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username filter.
   *
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the tag filter.
   *
   * @return the tag
   */
  public String getTag() {
    return tag;
  }

  /**
   * Sets the tag filter.
   *
   * @param tag the tag to set
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Gets the sort order.
   *
   * @return the sort
   */
  public PSCommentSort getSort() {
    return sort;
  }

  /**
   * Sets the sort order.
   *
   * @param sort the sort to set
   */
  public void setSort(PSCommentSort sort) {
    this.sort = sort;
  }

  /**
   * Gets the approval state filter.
   *
   * @return the state
   */
  public APPROVAL_STATE getState() {
    return state;
  }

  /**
   * Sets the approval state filter.
   *
   * @param state the state to set
   */
  public void setState(APPROVAL_STATE state) {
    this.state = state;
  }

  /**
   * Gets the site filter.
   *
   * @return the site
   */
  public String getSite() {
    return site;
  }

  /**
   * Sets the site filter.
   *
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Gets the maximum number of results to return.
   *
   * @return the maxResults
   */
  public int getMaxResults() {
    return maxResults;
  }

  /**
   * Sets the maximum number of results to return.
   *
   * @param maxResults the maxResults to set
   */
  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  /**
   * Gets the start index used for paging.
   *
   * @return the startIndex
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * Sets the start index used for paging.
   *
   * @param startIndex the startIndex to set
   */
  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  /**
   * Checks whether the moderated filter is set.
   *
   * @return the moderated
   */
  public Boolean isModerated() {
    return moderated;
  }

  /**
   * Sets the moderated filter.
   *
   * @param moderated the moderated to set
   */
  public void setModerated(Boolean moderated) {
    this.moderated = moderated;
  }

  /**
   * Checks whether the viewed filter is set.
   *
   * @return the viewed
   */
  public Boolean isViewed() {
    return viewed;
  }

  /**
   * Sets the viewed filter.
   *
   * @param viewed the viewed to set
   */
  public void setViewed(Boolean viewed) {
    this.viewed = viewed;
  }

  /**
   * Gets the id of the last comment added.
   *
   * @return the id of the last comment
   */
  public String getLastCommentId() {
    return lastCommentId;
  }

  /**
   * Sets the id of the last comment added.
   *
   * @param lastCommentId the id of the last comment to set
   */
  public void setLastCommentId(String lastCommentId) {
    this.lastCommentId = lastCommentId;
  }

  /***
   * Convenience method that returns the a JSON string representing this instance.
   *
   * @return A JSON formatted string, "" if the de-serialization fails.
   */
  public String toJSON() {
    String ret = "";
    ObjectMapper mapper = JsonMapper.builder().build();

    try {
      ret = mapper.writeValueAsString(this);
    } catch (Exception e) {
      log.warn("Error deserializing to JSON.", e);
    }
    return ret;
  }

  /**
   * Gets the relative path of the page not including the site. May be <code>null</code> or empty.
   *
   * @return the relative page path filter.
   */
  public String getSortby() {
    return sortby;
  }

  /**
   * Sets the relative path of the page not including the site.
   *
   * @param sortby the sortby value to set.
   */
  public void setSortby(String sortby) {
    this.sortby = sortby;
  }

  /**
   * Gets the ascending sort flag.
   *
   * @return the ascending flag value.
   */
  public String getAscending() {
    return ascending;
  }

  /**
   * Sets the ascending sort flag.
   *
   * @param ascending the ascending flag value to set.
   */
  public void setAscending(String ascending) {
    this.ascending = ascending;
  }

  /**
   * Gets the JSONP callback name.
   *
   * @return the JSONP callback name.
   */
  public String getCallback() {
    return callback;
  }

  /**
   * Sets the JSONP callback name.
   *
   * @param callback the JSONP callback name to set.
   */
  public void setCallback(String callback) {
    this.callback = callback;
  }
}
