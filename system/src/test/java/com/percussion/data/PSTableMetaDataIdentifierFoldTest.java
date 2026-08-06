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
   * ColumnInfo.compareTo is case-insensitive so binarySearch finds COMMUNITYID when the driver
   * returned communityid.
   */
  @Test
  void columnBinarySearchIsCaseInsensitive() {
    List<StringComparableColumn> cols = new ArrayList<>();
    cols.add(new StringComparableColumn("communityid"));
    cols.add(new StringComparableColumn("contentid"));
    cols.add(new StringComparableColumn("locale"));
    Collections.sort(cols);

    assertTrue(Collections.binarySearch(cols, "COMMUNITYID") >= 0);
    assertTrue(Collections.binarySearch(cols, "ContentId") >= 0);
    assertTrue(Collections.binarySearch(cols, "missing") < 0);
  }

  /** Mirrors {@link PSTableMetaData.ColumnInfo#compareTo} case-insensitive contract. */
  private static final class StringComparableColumn implements Comparable<Object> {
    private final String name;

    StringComparableColumn(String name) {
      this.name = name;
    }

    @Override
    public int compareTo(Object o) {
      if (o instanceof String) {
        return name.compareToIgnoreCase((String) o);
      }
      return name.compareToIgnoreCase(((StringComparableColumn) o).name);
    }
  }
}
