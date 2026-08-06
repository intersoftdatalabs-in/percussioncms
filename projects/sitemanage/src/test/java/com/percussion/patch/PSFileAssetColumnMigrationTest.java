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

package com.percussion.patch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression: empty CT_PERCFILEASSET must not drop ITEM_FILE_ATTACHMENT (fresh install path that
 * broke psx_cepercFileAsset with "no such column ITEM_FILE_ATTACHMENT").
 */
public class PSFileAssetColumnMigrationTest {

  @Test
  void freshInstallOnlyNewColumn_skipsMigration() {
    assertFalse(PSFileAssetColumnMigration.shouldMigrate(true, false));
    assertFalse(PSFileAssetColumnMigration.shouldDropEmptyNewColumn(true, false, 0));
  }

  @Test
  void emptyNewCountAloneDoesNotDrop() {
    // Pre-fix bug: count==0 always dropped ITEM_FILE_ATTACHMENT even with no X column
    assertFalse(PSFileAssetColumnMigration.shouldDropEmptyNewColumn(true, false, 0));
  }

  @Test
  void dualColumnEmptyNew_dropsThenRename() {
    assertTrue(PSFileAssetColumnMigration.shouldMigrate(true, true));
    assertTrue(PSFileAssetColumnMigration.shouldDropEmptyNewColumn(true, true, 0));
  }

  @Test
  void dualColumnWithData_doesNotDropNew() {
    assertTrue(PSFileAssetColumnMigration.shouldMigrate(true, true));
    assertFalse(PSFileAssetColumnMigration.shouldDropEmptyNewColumn(true, true, 5));
  }

  @Test
  void onlyLegacyOldColumn_renamesWithoutDrop() {
    assertTrue(PSFileAssetColumnMigration.shouldMigrate(false, true));
    assertFalse(PSFileAssetColumnMigration.shouldDropEmptyNewColumn(false, true, 0));
  }
}
