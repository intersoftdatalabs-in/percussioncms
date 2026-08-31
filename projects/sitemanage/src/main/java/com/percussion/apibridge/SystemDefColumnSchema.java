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
 * Create or drop the backend column for a persistable system-def field. Production uses JDBC
 * against the CMS repository ({@code CONTENTSTATUS} by default). Unit tests inject a no-op or
 * mock so they do not require a live database.
 */
interface SystemDefColumnSchema {

  /**
   * Create {@code tableName.columnName} when it is absent. Idempotent when the column already
   * exists.
   *
   * @param tableName backend table (typically {@code CONTENTSTATUS})
   * @param columnName column (typically the upper-cased field name)
   * @param fieldDataType system-def field data type ({@code text}, {@code integer}, …)
   * @param dataFormat optional size for text columns (for example {@code 50})
   */
  void ensureColumn(String tableName, String columnName, String fieldDataType, String dataFormat);

  /**
   * Drop {@code tableName.columnName} when it exists. Missing columns are not an error (H2
   * {@code no such column} after an XML-only add).
   */
  void dropColumnIfPresent(String tableName, String columnName);

  /** Test double that performs no DDL. */
  static SystemDefColumnSchema noop() {
    return new SystemDefColumnSchema() {
      @Override
      public void ensureColumn(
          String tableName, String columnName, String fieldDataType, String dataFormat) {
        // unit tests that do not exercise repository DDL
      }

      @Override
      public void dropColumnIfPresent(String tableName, String columnName) {
        // unit tests that do not exercise repository DDL
      }
    };
  }
}
