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

package com.percussion.soln.listbuilder;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An instance of a list builder usually created from a content node.
 *
 * @author adamgent // REFACTORED: CP-JAVA11
 */
public class ListBuilderItem {
  /**
   * Creates a new ListBuilderItem.
   */
  public ListBuilderItem() {
    // default
  }


  private String dateRangeStart;
  private String dateRangeEnd;
  private String titleContains;
  private String contentType;
  private String slot;
  private String childSnippet;
  private String jcrQuery;
  private String folderPath;

  private Collection<String> folderPaths = new ArrayList<>();
  private Collection<String> contentTypes = new ArrayList<>();
  private Long count;

  /**
   * Returns the folder path.
   *
   * @return the result
   */
  public String getFolderPath() {
    return folderPath;
  }

  /**
   * Sets the folder path.
   *
   * @param folderPath the folder path
   */
  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Returns the content types.
   *
   * @return the result
   */
  public Collection<String> getContentTypes() {
    return contentTypes;
  }

  /**
   * Sets the content types.
   *
   * @param contentTypes the content types
   */
  public void setContentTypes(Collection<String> contentTypes) {
    this.contentTypes = contentTypes;
  }

  /**
   * Returns the folder paths.
   *
   * @return the result
   */
  public Collection<String> getFolderPaths() {
    return folderPaths;
  }

  /**
   * Sets the folder paths.
   *
   * @param folderPaths the folder paths
   */
  public void setFolderPaths(Collection<String> folderPaths) {
    this.folderPaths = folderPaths;
  }

  /**
   * Returns the date range start.
   *
   * @return the result
   */
  public String getDateRangeStart() {
    return dateRangeStart;
  }

  /**
   * Sets the date range start.
   *
   * @param dateRangeStart the date range start
   */
  public void setDateRangeStart(String dateRangeStart) {
    this.dateRangeStart = dateRangeStart;
  }

  /**
   * Returns the date range end.
   *
   * @return the result
   */
  public String getDateRangeEnd() {
    return dateRangeEnd;
  }

  /**
   * Sets the date range end.
   *
   * @param dateRangeEnd the date range end
   */
  public void setDateRangeEnd(String dateRangeEnd) {
    this.dateRangeEnd = dateRangeEnd;
  }

  /**
   * Returns the title contains.
   *
   * @return the result
   */
  public String getTitleContains() {
    return titleContains;
  }

  /**
   * Sets the title contains.
   *
   * @param titleContains the title contains
   */
  public void setTitleContains(String titleContains) {
    this.titleContains = titleContains;
  }

  /**
   * Returns the content type.
   *
   * @return the result
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets the content type.
   *
   * @param contentType the content type
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Returns the slot.
   *
   * @return the result
   */
  public String getSlot() {
    return slot;
  }

  /**
   * Sets the slot.
   *
   * @param slot the slot
   */
  public void setSlot(String slot) {
    this.slot = slot;
  }

  /**
   * Returns the child snippet.
   *
   * @return the result
   */
  public String getChildSnippet() {
    return childSnippet;
  }

  /**
   * Sets the child snippet.
   *
   * @param childSnippet the child snippet
   */
  public void setChildSnippet(String childSnippet) {
    this.childSnippet = childSnippet;
  }

  /**
   * Returns the jcr query.
   *
   * @return the result
   */
  public String getJcrQuery() {
    return jcrQuery;
  }

  /**
   * Sets the jcr query.
   *
   * @param jcrQuery the jcr query
   */
  public void setJcrQuery(String jcrQuery) {
    this.jcrQuery = jcrQuery;
  }

  /**
   * Returns the count.
   *
   * @return the result
   */
  public Long getCount() {
    return count;
  }

  /**
   * Sets the count.
   *
   * @param count the count
   */
  public void setCount(Long count) {
    this.count = count;
  }
}
