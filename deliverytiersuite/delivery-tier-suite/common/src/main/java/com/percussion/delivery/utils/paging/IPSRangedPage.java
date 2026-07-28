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
package com.percussion.delivery.utils.paging;

import java.util.Map;

/**
 * Defines a generic Interface for a ranged page option for lookups to backend data stores.
 *
 * @author natechadwick
 */
public interface IPSRangedPage {
  /**
   * Returns the backend fields being sorted on, paired with the sort direction.
   *
   * @return a map of backend fields paired with their sort direction; may be empty, never <code>
   *     null</code>.
   */
  public Map<String, PSRangedPageSortDirection> getSortFields();

  /**
   * Sets the backend fields to sort by, indicating sort direction per field.
   *
   * @param fields map of sort fields and their sort directions; may be empty, never <code>null
   *     </code>.
   */
  public void setSortFields(Map<String, PSRangedPageSortDirection> fields);

  /**
   * Gets the current paging filters and values.
   *
   * @return a map of field/value pairs; may be empty, never <code>null</code>.
   */
  public Map<String, Object> getPageFields();

  /**
   * Sets the fields used to apply the range filter and the last value used to fetch the backend
   * page. The supplied map should pair each field with the value that was at the end of the last
   * page when paging forward, or at the beginning of the last page when paging backward. Field
   * values may be <code>null</code> when indicating the beginning or end of a dataset.
   *
   * @param fields a map of field/value pairs, never <code>null</code>.
   */
  public void setPageFields(Map<String, Object> fields);

  /**
   * Returns the current page size.
   *
   * @return a fixed page size, or a default size if none has been set; never <code>0</code>.
   */
  public int getPageSize();

  /**
   * Sets the current page size.
   *
   * @param size the desired page size; if set to <code>0</code> or a negative number, a default
   *     page size should be enforced.
   */
  public void setPageSize(int size);

  /**
   * Returns the current paging direction.
   *
   * @return the paging direction, never <code>null</code>.
   */
  public PSRangedPageDirection getDirection();

  /**
   * Sets the current paging direction.
   *
   * @param dir the paging direction, never <code>null</code>.
   */
  public void setDirection(PSRangedPageDirection dir);

  /**
   * Specifies the total number of pages available.
   *
   * @param numPages the total number of pages, never negative.
   */
  public void setPageCount(int numPages);

  /**
   * Returns the total number of pages available.
   *
   * @return the total number of pages.
   */
  public int getPageCount();

  /**
   * Specifies the current page number.
   *
   * @param pageNum the current page number (typically zero-based).
   */
  public void setCurrentPage(int pageNum);

  /**
   * Returns the current page number.
   *
   * @return the current page number.
   */
  public int getCurrentPage();
}
