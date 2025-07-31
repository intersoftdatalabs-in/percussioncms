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
package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for executing metadata queries.
 */
public interface IPSMetadataQueryService {

    /**
     * Executes a query against the metadata service.
     * @param query the metadata query, cannot be null.
     * @return PSPair containing list of result objects and total count.
     * @throws Exception on query parsing error.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    PSPair<List<IPSMetadataEntry>, Integer> executeQuery(PSMetadataQuery query) throws Exception;

    /**
     * Executes a category query against the metadata service.
     * @param query the metadata query.
     * @return list of category query results.
     * @throws Exception on query parsing error.
     */
    List<Object[]> executeCategoryQuery(PSMetadataQuery query) throws Exception;

    /**
     * Enum for Hibernate query return types.
     */
    enum SORTTYPE {
        NONE, PROPERTY, METADATA
    }

    // Column names in the properties table
    String PROP_DATEVALUE_COLUMN_NAME = "datevalue";
    String PROP_NUMBERVALUE_COLUMN_NAME = "numbervalue";
    String PROP_STRINGVALUE_COLUMN_NAME = "stringvalue";
    String PROP_VALUEHASH_COLUMN_NAME = "valueHash";
    String PROP_TEXTVALUE_COLUMN_NAME = "textvalue";
    String SORT_ORDER_ASCEND = "asc";
    String SORT_ORDER_DESCEND = "desc";

    Integer getQueryLimit();
    void setQueryLimit(Integer limit);
}
