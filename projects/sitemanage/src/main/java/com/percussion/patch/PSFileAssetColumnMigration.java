/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.patch;

/**
 * Pure decision helpers for the legacy {@code CT_PERCFILEASSET} attachment-column rename ({@code
 * ITEM_FILE_ATTACHMENTX} → {@code ITEM_FILE_ATTACHMENT}).
 *
 * <p>Historical upgrade left some repositories with both columns. The migration must <em>never</em>
 * drop {@code ITEM_FILE_ATTACHMENT} merely because the table has zero non-null values — that is
 * normal for a fresh install and was destroying the only real column, then failing content editor
 * init with {@code no such column ITEM_FILE_ATTACHMENT}.
 */
public final class PSFileAssetColumnMigration {

  public static final String TABLE = "CT_PERCFILEASSET";
  public static final String COLUMN_NEW = "ITEM_FILE_ATTACHMENT";
  public static final String COLUMN_OLD = "ITEM_FILE_ATTACHMENTX";

  private PSFileAssetColumnMigration() {}

  /**
   * Whether any migration SQL should run.
   *
   * @param hasNewColumn {@code true} if {@link #COLUMN_NEW} exists
   * @param hasOldColumn {@code true} if {@link #COLUMN_OLD} exists
   * @return {@code true} only when the old (legacy) column is present
   */
  public static boolean shouldMigrate(boolean hasNewColumn, boolean hasOldColumn) {
    return hasOldColumn;
  }

  /**
   * Whether the empty "new" column should be dropped before renaming the old one.
   *
   * <p>Only when both columns exist and the new column has no non-null data (legacy dual-column
   * cleanup). Never drop when the old column is absent.
   *
   * @param hasNewColumn whether new column exists
   * @param hasOldColumn whether old column exists
   * @param nonNullNewCount rows with non-null new-column values
   * @return {@code true} if {@code DROP COLUMN} on the new name is required first
   */
  public static boolean shouldDropEmptyNewColumn(
      boolean hasNewColumn, boolean hasOldColumn, int nonNullNewCount) {
    return hasNewColumn && hasOldColumn && nonNullNewCount == 0;
  }
}
