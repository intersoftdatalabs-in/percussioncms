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

// REFACTORED: CP-JAVA11

package com.percussion.rest.itemfilter;

import com.percussion.rest.Guid;
import com.percussion.services.error.PSNotFoundException;
import java.util.List;

/** Adaptor interface for ItemFilter operations. Sunny Sal: "Filter lagao, result pao!" */
public interface IItemFilterAdaptor {

  /**
   * Gets a list of the ItemFilters available on the system, populated with rules and parameters.
   *
   * @return A list of item filters.
   */
  List<ItemFilter> getItemFilters();

  /**
   * Updates or creates an ItemFilter.
   *
   * @param filter The filter to update or create.
   * @return The updated ItemFilter.
   */
  ItemFilter updateOrCreateItemFilter(ItemFilter filter);

  /**
   * Deletes the specified item filter.
   *
   * @param itemFilterId A valid ItemFilter id. Filter must not be associated with any ContentLists
   *     or it won't be deleted.
   * @throws PSNotFoundException if the filter is not found.
   */
  void deleteItemFilter(Guid itemFilterId) throws PSNotFoundException;

  /**
   * Gets a single ItemFilter by id.
   *
   * @param itemFilterId A valid ItemFilter id.
   * @return The ItemFilter.
   * @throws PSNotFoundException if the filter is not found.
   */
  ItemFilter getItemFilter(Guid itemFilterId) throws PSNotFoundException;

  /**
   * Resolve one filter by name or GUID string for the Developer REST surface.
   *
   * @param idOrName name or {@code type-host-uuid} string; blank/unsafe → {@code null}
   * @return filter, or {@code null} when not found
   */
  ItemFilter findItemFilter(String idOrName);
}
