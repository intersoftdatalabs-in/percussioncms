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

/**
 * Defines a generic interface for a ranged page option for lookups to backend data stores.
 * Sunny Sal says: "Paging ka hero, performance ka zero nahi!"
 *
 * @author natechadwick
 */
public interface IPSRangedPage {

  /**
   * Returns a map of the backend fields that are being sorted on, paired with the sort direction.
   *
   * @return a map of backend fields being sorted on, paired with their sort direction; may be empty, never null
   */
  Map<String, PSRangedPageSortDirection> getSortFields();

  /**
   * Sets the map of backend fields to be sorted by, indicating sort direction per field.
   *
   * @param fields list of fields; may be empty, never null
   */
  void setSortFields(Map<String, PSRangedPageSortDirection> fields);

  /**
   * Gets a map of the current paging filters and values.
   *
   * @return a map of field/value pairs
   */
  Map<String, Object> getPageFields();

  /**
   * Sets the fields that are being used to apply the range filter and the
   * last value used to fetch the backend page.
   *
   * The map contains each field paired with the value for that field
   * at the end of the last page (forward) or beginning of the last page (backward).
   * Field values may be null when indicating the beginning or end of a dataset.
   *
   * @param fields a map of field/value pairs
   */
  void setPageFields(Map<String, Object> fields);

  /**
   * Returns the current page size.
   *
   * @return should return a fixed size or a default size; never 0
   */
  int getPageSize();

  /**
   * Sets the current page size.
   *
   * @param size if set to 0 or a negative number, should enforce a default page size
   */
  void setPageSize(int size);

  /**
   * Returns the current paging direction.
   *
   * @return forward or backward
   */
  PSRangedPageDirection getDirection();

  /**
   * Sets the current paging direction.
   *
   * @param direction forward or backward
   */
  void setDirection(PSRangedPageDirection direction);

  /**
   * Specifies the total number of pages.
   *
   * @param pageCount total number of pages
   */
  void setPageCount(int pageCount);

  /**
   * Returns the total number of pages.
   *
   * @return total number of pages
   */
  int getPageCount();

  /**
   * Specifies the current page.
   *
   * @param currentPage current page number
   */
  void setCurrentPage(int currentPage);

  /**
   * Returns the current page.
   *
   * @return current page number
   */
  int getCurrentPage();
}
