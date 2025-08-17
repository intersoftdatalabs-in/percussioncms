// REFACTORED: CP-JAVA11
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
package com.percussion.share.service;

import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import java.util.List;

/**
 * Extremely low-level wrapper to CM System for relatively fast item retrieval.
 *
 * <p>Be aware: Although this is public API, it should probably not be used externally and may be
 * removed in the future.
 *
 * @param <S> item summary type
 * @author adamgent
 */
public interface IPSItemSummaryService<S extends IPSItemSummary>
    extends IPSCatalogService<S, String> {

  /**
   * Returns the id for the given path. The path could be to an asset, page, or folder. <strong>
   * NOTICE that the return value may be {@code null}. </strong> Higher-level layers and API should
   * deal with the null return value.
   *
   * @param path never {@code null} or empty
   * @return may be {@code null} if there is no item at the given path
   */
  String pathToId(String path) throws IPSDataService.DataServiceNotFoundException;

  /**
   * Returns the id for the given path and relationship type. The path could be to an asset, page,
   * or folder. <strong> NOTICE that the return value may be {@code null}. </strong> Higher-level
   * layers and API should deal with the null return value.
   *
   * @param path never {@code null} or empty
   * @param relationshipTypeName the relationship type name
   * @return may be {@code null} if there is no item at the given path
   */
  String pathToId(String path, String relationshipTypeName)
      throws IPSDataService.DataServiceNotFoundException;

  /**
   * Returns the items that are children to the given id. The id should probably be an item that is
   * a folder.
   *
   * @param id never {@code null} or empty
   * @return never {@code null}, may be empty
   * @throws DataServiceLoadException if the item is not valid to have children or does not exist
   */
  List<S> findFolderChildren(String id) throws DataServiceLoadException;
}
