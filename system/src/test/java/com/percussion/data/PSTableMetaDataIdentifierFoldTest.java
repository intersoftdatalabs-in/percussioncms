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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Identifier / column name folding for backends that store unquoted names in lower (PostgreSQL,
 * typical MySQL) or upper case. Prevents empty {@code DatabaseMetaData#getColumns} results and "no
 * such column COMMUNITYID" on matrix smoke.
 */
@Tag("UnitTest")
class PSTableMetaDataIdentifierFoldTest {

  @Test
  void foldsToLowerWhenDriverStoresLowerCase() throws Exception {
    DatabaseMetaData dmd = mock(DatabaseMetaData.class);
    when(dmd.storesLowerCaseIdentifiers()).thenReturn(true);
    when(dmd.storesUpperCaseIdentifiers()).thenReturn(false);

    assertEquals("contentstatus", PSTableMetaData.foldStoredIdentifier("CONTENTSTATUS", dmd));
    assertEquals("public", PSTableMetaData.foldStoredIdentifier("Public", dmd));
    assertEquals(null, PSTableMetaData.foldStoredIdentifier(null, dmd));
  }

  @Test
  void foldsToUpperWhenDriverStoresUpperCase() throws Exception {
    DatabaseMetaData dmd = mock(DatabaseMetaData.class);
    when(dmd.storesLowerCaseIdentifiers()).thenReturn(false);
    when(dmd.storesUpperCaseIdentifiers()).thenReturn(true);

    assertEquals("CONTENTSTATUS", PSTableMetaData.foldStoredIdentifier("contentstatus", dmd));
  }

  @Test
  void leavesMixedCaseWhenDriverPreservesCase() throws Exception {
    DatabaseMetaData dmd = mock(DatabaseMetaData.class);
    when(dmd.storesLowerCaseIdentifiers()).thenReturn(false);
    when(dmd.storesUpperCaseIdentifiers()).thenReturn(false);

    assertEquals("ContentStatus", PSTableMetaData.foldStoredIdentifier("ContentStatus", dmd));
  }

  /**
   * Case-insensitive column lookup (same algorithm as {@code PSTableMetaData#findColumnIndex}) so
   * COMMUNITYID is found when the driver returned communityid.
   */
  @Test
  void columnBinarySearchIsCaseInsensitive() {
    List<String> cols = new ArrayList<>();
    cols.add("communityid");
    cols.add("contentid");
    cols.add("locale");
    Collections.sort(cols, String.CASE_INSENSITIVE_ORDER);

    assertTrue(findColumnIndex(cols, "COMMUNITYID") >= 0);
    assertTrue(findColumnIndex(cols, "ContentId") >= 0);
    assertTrue(findColumnIndex(cols, "missing") < 0);
    assertEquals(-1, findColumnIndex(cols, null));
  }

  /** Mirrors package-private {@code PSTableMetaData#findColumnIndex} for unit isolation. */
  private static int findColumnIndex(List<String> columns, String columnName) {
    if (columnName == null) {
      return -1;
    }
    int low = 0;
    int high = columns.size() - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int cmp = columns.get(mid).compareToIgnoreCase(columnName);
      if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      } else {
        return mid;
      }
    }
    return -(low + 1);
  }
}
