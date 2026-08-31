/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.apibridge;

/**
 * Create the backend column for a persistable local content-type field. Production uses JDBC
 * against the CMS repository (Workbench table-factory ALTER). Unit tests inject a no-op or mock so
 * they do not require a live database — tests that cover persist must still verify this is invoked
 * <em>before</em> {@code IPSContentDesignWs.saveContentTypes} re-inits the content editor
 * application.
 */
interface ContentTypeLocalFieldColumnSchema {

  /**
   * Create {@code tableName.columnName} when it is absent. Idempotent when the column already
   * exists.
   *
   * @param tableName backend table (typically the content type's local table alias)
   * @param columnName column (typically the upper-cased field name)
   * @param fieldDataType field data type ({@code text}, {@code integer}, …)
   * @param dataFormat optional size for text columns (for example {@code 50})
   */
  void ensureColumn(String tableName, String columnName, String fieldDataType, String dataFormat);

  /** Test double that performs no DDL. */
  static ContentTypeLocalFieldColumnSchema noop() {
    return (tableName, columnName, fieldDataType, dataFormat) -> {
      // unit tests that do not exercise repository DDL
    };
  }
}
