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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSTableChangeEvent} column maps after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSTableChangeEventTypedTest {

  @Test
  void constructorAndGetColumnsReturnDefensiveCopy() {
    Map<String, String> columns = new HashMap<>();
    columns.put("CONTENTID", "42");
    columns.put("TITLE", "hello");

    PSTableChangeEvent event =
        new PSTableChangeEvent("CONTENTSTATUS", PSTableChangeEvent.ACTION_UPDATE, columns);

    assertEquals("CONTENTSTATUS", event.getTableName());
    assertEquals(PSTableChangeEvent.ACTION_UPDATE, event.getActionType());

    Map<String, String> copy = event.getColumns();
    assertEquals(2, copy.size());
    assertEquals("42", copy.get("CONTENTID"));
    assertNotSame(columns, copy);

    // Mutating the returned map must not affect the event.
    copy.put("EXTRA", "x");
    assertEquals(2, event.getColumns().size());
  }

  @Test
  void rejectsNullOrEmptyTableAndNullColumns() {
    Map<String, String> columns = new HashMap<>();
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSTableChangeEvent(null, PSTableChangeEvent.ACTION_INSERT, columns));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSTableChangeEvent("  ", PSTableChangeEvent.ACTION_INSERT, columns));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSTableChangeEvent("T", PSTableChangeEvent.ACTION_INSERT, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSTableChangeEvent("T", PSTableChangeEvent.ACTION_UNDEFINED, columns));
  }

  @Test
  void isValidActionRecognizesKnownTypes() {
    assertTrue(PSTableChangeEvent.isValidAction(PSTableChangeEvent.ACTION_INSERT));
    assertTrue(PSTableChangeEvent.isValidAction(PSTableChangeEvent.ACTION_UPDATE));
    assertTrue(PSTableChangeEvent.isValidAction(PSTableChangeEvent.ACTION_DELETE));
  }
}
