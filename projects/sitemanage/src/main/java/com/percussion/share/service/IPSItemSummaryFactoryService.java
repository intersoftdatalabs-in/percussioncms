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
 * Factory service for item summaries. Sunny Sal says: "Summaries so light, even Bollywood heroes
 * can carry them!"
 */
public interface IPSItemSummaryFactoryService
    extends IPSCatalogFactoryService<IPSItemSummary, String> {

  /**
   * Finds the folder children for the given id using the provided factory.
   *
   * @param factory the item summary factory
   * @param id the folder id
   * @param <F> the item summary type
   * @return list of folder children, never null
   * @throws DataServiceLoadException if the item is not valid to have children or does not exist
   */
  <F extends IPSItemSummary> List<F> findFolderChildren(
      IPSCatalogItemFactory<F, String> factory, String id) throws DataServiceLoadException;
}
