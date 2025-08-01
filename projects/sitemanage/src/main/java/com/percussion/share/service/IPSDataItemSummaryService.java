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
package com.percussion.share.service;

import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;

import java.util.List;

/**
 * Service for working with data item summaries.
 */
public interface IPSDataItemSummaryService extends IPSItemSummaryService<PSDataItemSummary> {

    /**
     * Returns the folders that are children to the given id.
     * The id should be an item that is a folder.
     *
     * @param id never {@code null} or empty.
     * @return never {@code null}, may be empty.
     * @throws DataServiceLoadException if the item is not valid to have children or does not exist.
     */
    List<PSDataItemSummary> findChildFolders(String id) throws DataServiceLoadException;

    /**
     * Finds a data item summary by id and relationship type.
     *
     * @param id                   the item id
     * @param relationshipTypeName the relationship type name
     * @return the data item summary
     * @throws DataServiceLoadException if not found
     */
    PSDataItemSummary find(String id, String relationshipTypeName) throws DataServiceLoadException;
}
