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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSResultSet} construction and column map. */
@Tag("UnitTest")
class PSResultSetTypedConstructionTest {

  @Test
  void typedColumnMapAndListsRoundTrip() throws SQLException {
    List<String> names = new ArrayList<>();
    names.add("alpha");
    names.add("beta");
    List<Integer> values = new ArrayList<>();
    values.add(10);
    values.add(20);

    HashMap<String, Integer> colMap = new HashMap<>();
    colMap.put("NAME", 1);
    colMap.put("VAL", 2);

    PSResultSet rs = new PSResultSet(new List<?>[] {names, values}, colMap, null);
    assertTrue(rs.next());
    assertEquals("alpha", rs.getString("NAME"));
    assertEquals(10, rs.getInt("VAL"));
    assertTrue(rs.next());
    assertEquals("beta", rs.getString(1));
    assertEquals(20, rs.getInt(2));
    assertFalse(rs.next());

    Map<String, Integer> exposed = rs.getColumnNames();
    assertEquals(Integer.valueOf(1), exposed.get("NAME"));
    assertEquals(Integer.valueOf(2), exposed.get("VAL"));
    assertEquals(2, rs.getColumnData("NAME").size());
  }

  @Test
  void emptyConstructorHasOpenCursorBeforeFirst() throws SQLException {
    PSResultSet rs = new PSResultSet();
    assertTrue(rs.isBeforeFirst());
    assertFalse(rs.next());
  }
}
