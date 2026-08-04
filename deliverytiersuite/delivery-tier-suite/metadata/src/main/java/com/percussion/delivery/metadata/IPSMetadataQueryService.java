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
package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import java.util.List;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service contract that executes structured metadata queries against the DTS metadata indexer.
 *
 * @author erikserating
 */
public interface IPSMetadataQueryService {
  /**
   * Executes a query against the metadata query service.
   *
   * @param query the metadata query, cannot be <code>null</code>.
   * @return PSPair which contains list of result objects, those are sorted according to the orderby
   *     in the query and number of results also determined by the maxresults value in the query and
   *     total count of available objects for the passed in query criteria
   * @throws Exception on query parsing error
   */
  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_UNCOMMITTED,
      readOnly = true)
  public PSPair<List<IPSMetadataEntry>, Integer> executeQuery(PSMetadataQuery query)
      throws Exception;

  /**
   * Executes a category aggregation query and returns the matching rows as raw {@code Object[]}
   * arrays. Each row is normally {@code [count, name, stringvalue]}.
   *
   * @param query the metadata query driving the category aggregation; may not be <code>null</code>.
   * @return the list of category aggregation rows, never <code>null</code>, may be empty.
   * @throws Exception if the query cannot be parsed or executed.
   */
  public List<Object[]> executeCategoryQuery(PSMetadataQuery query) throws Exception;

  /**
   * Based on the query hibernate return type would be different. Following enum is to handle the
   * return type variants produced by Hibernate for {@link #executeQuery(PSMetadataQuery)}:
   *
   * <ul>
   *   <li>{@link #NONE} - no explicit ORDER BY; results are returned as the entry list directly.
   *   <li>{@link #PROPERTY} - ORDER BY targets a column on the properties table; results carry an
   *       extra {@code sort1} projection.
   *   <li>{@link #METADATA} - ORDER BY targets a column on the entries table itself.
   * </ul>
   */
  public enum SORTTYPE {
    /** No explicit ORDER BY clause on the query. */
    NONE,
    /** ORDER BY targets a properties-table column; results carry an extra {@code sort1}. */
    PROPERTY,
    /** ORDER BY targets an entries-table column. */
    METADATA
  }

  // Column names in the properties table
  /** Column name for the date-typed property value on the properties table. */
  public static final String PROP_DATEVALUE_COLUMN_NAME = "datevalue";

  /** Column name for the numeric-typed property value on the properties table. */
  public static final String PROP_NUMBERVALUE_COLUMN_NAME = "numbervalue";

  /** Column name for the short string-typed property value on the properties table. */
  public static final String PROP_STRINGVALUE_COLUMN_NAME = "stringvalue";

  /** Column name for the value-hash projection used to compare string/text properties. */
  public static final String PROP_VALUEHASH_COLUMN_NAME = "valueHash";

  /** Column name for the long-form text-typed property value on the properties table. */
  public static final String PROP_TEXTVALUE_COLUMN_NAME = "textvalue";

  /** Suffix used to request ascending sort order. */
  public static final String SORT_ORDER_ASCEND = "asc";

  /** Suffix used to request descending sort order. */
  public static final String SORT_ORDER_DESCEND = "desc";

  /**
   * Returns the configured maximum number of results any single query may return.
   *
   * @return the configured limit, never <code>null</code>.
   */
  public Integer getQueryLimit();

  /**
   * Overrides the configured maximum number of results any single query may return.
   *
   * @param limit the new limit to apply, may be <code>null</code> to indicate no override.
   */
  public void setQueryLimit(Integer limit);
}
