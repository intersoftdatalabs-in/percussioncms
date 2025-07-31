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
 * Provides a generic implementation of a ranged page object.
 *
 * Intended for use in all Delivery Services that retrieve data
 * from the server for processing in the client user interface.
 *
 * All find operations must implement paging to ensure
 * viability, scalability, and performance of both client and server.
 *
 * For corner case datasets where ranged paging will not work, an alternative
 * paging provider should be created.
 *
 * @author natechadwick
 */
public class PSDefaultRangedPage implements IPSRangedPage {

  /** Default used when page size is not set. Target is roughly 3 UX screens of data. */
  public static final int DEFAULT_PAGE_SIZE = 75;

  /** The direction of the paging operation. */
  private PSRangedPageDirection direction;

  /** The map of sort fields with sort directions. */
  private ConcurrentHashMap<String, PSRangedPageSortDirection> sortFields = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String, Object> pageFields = new ConcurrentHashMap<>();

  private int pageSize = DEFAULT_PAGE_SIZE;

  private int pageCount;
  private int currentPage;

  @Override
  public Map<String, PSRangedPageSortDirection> getSortFields() {
    return sortFields;
  }

  @Override
  public void setSortFields(Map<String, PSRangedPageSortDirection> fields) {
    if (fields != null) {
      if (fields instanceof ConcurrentHashMap) {
        sortFields = (ConcurrentHashMap<String, PSRangedPageSortDirection>) fields;
      } else {
        sortFields = new ConcurrentHashMap<>(fields);
      }
    }
  }

  @Override
  public Map<String, Object> getPageFields() {
    return pageFields;
  }

  @Override
  public void setPageFields(Map<String, Object> fields) {
    if (fields == null) {
      throw new IllegalArgumentException("Field list may not be null");
    }
    if (fields instanceof ConcurrentHashMap) {
      pageFields = (ConcurrentHashMap<String, Object>) fields;
    } else {
      pageFields = new ConcurrentHashMap<>(fields);
    }
  }

  @Override
  public int getPageSize() {
    return pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
  }

  @Override
  public void setPageSize(int size) {
    pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
  }

  @Override
  public PSRangedPageDirection getDirection() {
    if (direction == null) {
      direction = PSRangedPageDirection.FORWARD;
    }
    return direction;
  }

  @Override
  public void setDirection(PSRangedPageDirection direction) {
    this.direction = direction;
  }

  public PSDefaultRangedPage() {}

  public PSDefaultRangedPage(IPSRangedPage page) {
    direction = page.getDirection();
    var pf = page.getPageFields();
    pageFields = pf instanceof ConcurrentHashMap
        ? (ConcurrentHashMap<String, Object>) pf
        : new ConcurrentHashMap<>(pf);
    pageSize = page.getPageSize();
    var sf = page.getSortFields();
    sortFields = sf instanceof ConcurrentHashMap
        ? (ConcurrentHashMap<String, PSRangedPageSortDirection>) sf
        : new ConcurrentHashMap<>(sf);
  }

  @Override
  public void setPageCount(int pageCount) {
    this.pageCount = pageCount;
  }

  @Override
  public int getPageCount() {
    return pageCount;
  }

  @Override
  public void setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
  }

  @Override
  public int getCurrentPage() {
    return currentPage;
  }
}
