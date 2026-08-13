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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSConnectionDetail;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral coverage for #3282 NEXTNUMBER vs seed {@code PSX_OBJECTACL.SYSID} reconcile. */
@Tag("UnitTest")
class PSObjectAclNextNumberReconcilerTest {

  @Test
  @DisplayName("peek 1001 + seed max 1006 advances NEXTNUMBER to 1007")
  void reconcileAdvancesPastSeedEveryoneSysid() {
    RecordingNextNumber numbers = new RecordingNextNumber(1001);
    boolean moved = PSObjectAclNextNumberReconciler.reconcile(numbers, () -> 1006);
    assertTrue(moved);
    assertEquals(1007, numbers.peek(PSObjectAclNextNumberReconciler.NEXTNUMBER_KEY));
    assertEquals(1007, numbers.lastAdvancedTo);
  }

  @Test
  void reconcileNoOpWhenPeekAlreadyFree() {
    RecordingNextNumber numbers = new RecordingNextNumber(2001);
    boolean moved = PSObjectAclNextNumberReconciler.reconcile(numbers, () -> 1006);
    assertFalse(moved);
    assertEquals(-1, numbers.lastAdvancedTo);
    assertEquals(2001, numbers.peek(PSObjectAclNextNumberReconciler.NEXTNUMBER_KEY));
  }

  @Test
  void reconcileRejectsNullDeps() {
    RecordingNextNumber numbers = new RecordingNextNumber(1001);
    assertThrows(
        IllegalArgumentException.class,
        () -> PSObjectAclNextNumberReconciler.reconcile(null, () -> 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSObjectAclNextNumberReconciler.reconcile(numbers, null));
  }

  @Test
  void qualifiedTableUsesSchemaWhenPresent() {
    PSConnectionDetail details =
        new PSConnectionDetail("rxdefault", "h2", "rx", "PUBLIC", "jdbc:h2:mem:t");
    assertEquals("PUBLIC.PSX_OBJECTACL", PSObjectAclNextNumberReconciler.qualifiedTable(details));
  }

  @Test
  void qualifiedTableOmitsBlankSchema() {
    assertEquals("PSX_OBJECTACL", PSObjectAclNextNumberReconciler.qualifiedTable(null));
    PSConnectionDetail details =
        new PSConnectionDetail("rxdefault", "h2", "rx", null, "jdbc:h2:mem:t");
    assertEquals("PSX_OBJECTACL", PSObjectAclNextNumberReconciler.qualifiedTable(details));
  }

  private static final class RecordingNextNumber
      implements PSObjectAclNextNumberReconciler.NextNumberView {
    private final Map<String, Integer> values = new HashMap<>();
    private int lastAdvancedTo = -1;

    RecordingNextNumber(int peek) {
      values.put(PSObjectAclNextNumberReconciler.NEXTNUMBER_KEY, peek);
    }

    @Override
    public int peek(String key) {
      return values.getOrDefault(key, 0);
    }

    @Override
    public void advanceTo(String key, int nextId) {
      lastAdvancedTo = nextId;
      values.put(key, nextId);
    }
  }
}
