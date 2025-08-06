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
package com.percussion.share.service;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemSummaryUtils;
import com.percussion.share.service.exception.PSDataServiceException;

/**
 * Abstract data service for objects with summary support.
 *
 * @param <FULL> Full object type
 * @param <SUM> Summary type (must implement IPSItemSummary)
 */
public abstract class PSAbstractFullDataService<FULL, SUM extends IPSItemSummary>
    extends PSAbstractDataService<FULL, SUM, String>
    implements IPSDataService<FULL, SUM, String> {

    protected final IPSDataItemSummaryService itemSummaryService;

    public PSAbstractFullDataService(
            IPSDataItemSummaryService itemSummaryService,
            IPSGenericDao<FULL, String> dao) {
        super(dao);
        this.itemSummaryService = itemSummaryService;
    }

    @Override
    public SUM find(String id) throws PSDataServiceException {
        validateIdParameter("find", id);
        var itemSummary = itemSummaryService.find(id);
        var sum = createSummary(id);
        PSItemSummaryUtils.copyProperties(itemSummary, sum);
        return sum;
    }

    /**
     * Create a new summary instance for the given id.
     */
    protected abstract SUM createSummary(String id);
}
