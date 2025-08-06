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

package com.percussion.category.dao;

import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import java.util.Set;

/**
 * Data access object for category operations.
 * Provides methods to delete categories and retrieve page IDs by category.
 *
 * @author chriswright
 */
public interface IPSCategoryDao {

    /**
     * Deletes the specified category IDs from the ct_page child categories table.
     * Also evicts the provided page IDs from the Hibernate cache.
     *
     * @param ids     the category IDs to remove; must not be {@code null} or empty.
     * @param pageIds IDs corresponding to pages using the categories; must not be {@code null} or empty.
     *                Use {@link #getPageIdsFromCategoryIds(Set)} to obtain page IDs.
     */
    void delete(Set<String> ids, List<IPSGuid> pageIds);

    /**
     * Retrieves the page IDs that use the specified category IDs.
     *
     * @param ids the category IDs; must not be {@code null}.
     * @return a list of page IDs using the categories; never {@code null}, may be empty.
     */
    List<Integer> getPageIdsFromCategoryIds(Set<String> ids);
}
