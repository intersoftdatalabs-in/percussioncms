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
// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils.paging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a generic implementation of a Ranged Page object.
 *
 * Intended for use in all Delivery Services that retrieve data
 * from the server for processing in the client user interface.
 *
 * In general all find operations must implement paging to ensure
 * the viability/scalability and performance of both the client and server.
 *
 * For corner case datasets where Ranged Paging will not work, an alternative
 * paging provider should be created.
 *
 * @author natechadwick
 */
public class PSDefaultRangedPage implements IPSRangedPage {

  /***
   * Default used when page size is not set.
   * Target is roughly 3 UX screens of data.
   */
  public static final int DEFAULT_PAGE_SIZE = 75;

  /***
   * The direction of the paging operation.
   */
  private PSRangedPageDirection direction;

  /***
   * The map of sort fields with sort directions
   */
  private ConcurrentHashMap<String, PSRangedPageSortDirection> sortFields = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String, Object> pageFields = new ConcurrentHashMap<>();

  private int pageSize = DEFAULT_PAGE_SIZE;

  private int pageCount;
  private int currentPage;

  @Override
  public Map<String, PSRangedPageSortDirection> getSortFields() {
    return this.sortFields;
  }

  @Override
  public void setSortFields(Map<String, PSRangedPageSortDirection> fields) {
    if (fields != null) {
      if (fields instanceof ConcurrentHashMap) {
        this.sortFields = (ConcurrentHashMap<String, PSRangedPageSortDirection>) fields;
      } else {
        this.sortFields = new ConcurrentHashMap<>(fields);
      }
    }
  }

  @Override
  public Map<String, Object> getPageFields() {
    return this.pageFields;
  }

  @Override
  public void setPageFields(Map<String, Object> fields) {
    if (fields == null) {
      throw new IllegalArgumentException("Field list may not be null");
    }
    if (fields instanceof ConcurrentHashMap) {
      this.pageFields = (ConcurrentHashMap<String, Object>) fields;
    } else {
      this.pageFields = new ConcurrentHashMap<>(fields);
    }
  }

  @Override
  public int getPageSize() {
    return this.pageSize <= 0 ? DEFAULT_PAGE_SIZE : this.pageSize;
  }

  @Override
  public void setPageSize(int size) {
    this.pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
  }

  @Override
  public PSRangedPageDirection getDirection() {
    if (this.direction == null) {
      this.direction = PSRangedPageDirection.FORWARD;
    }
    return this.direction;
  }

  @Override
  public void setDirection(PSRangedPageDirection dir) {
    this.direction = dir;
  }

  public PSDefaultRangedPage() {}

  public PSDefaultRangedPage(IPSRangedPage page) {
    this.direction = page.getDirection();
    var pf = page.getPageFields();
    this.pageFields = pf instanceof ConcurrentHashMap
        ? (ConcurrentHashMap<String, Object>) pf
        : new ConcurrentHashMap<>(pf);
    this.pageSize = page.getPageSize();
    var sf = page.getSortFields();
    this.sortFields = sf instanceof ConcurrentHashMap
        ? (ConcurrentHashMap<String, PSRangedPageSortDirection>) sf
        : new ConcurrentHashMap<>(sf);
  }

  @Override
  public void setPageCount(int numPages) {
    pageCount = numPages;
  }

  @Override
  public int getPageCount() {
    return pageCount;
  }

  @Override
  public void setCurrentPage(int pageNum) {
    currentPage = pageNum;
  }

  @Override
  public int getCurrentPage() {
    return currentPage;
  }
}
