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
package com.percussion.server.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.error.PSIllegalArgumentException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Typed collection helpers and ctor parsing for {@link PSConsoleCommandLogDump} (#3272). */
@Tag("UnitTest")
@DisplayName("PSConsoleCommandLogDump typed collections")
class PSConsoleCommandLogDumpTypedTest {

  @Test
  @DisplayName("toIntArray returns null for null or empty lists")
  void toIntArrayEmptyIsUnrestricted() {
    assertNull(PSConsoleCommandLogDump.toIntArray(null));
    assertNull(PSConsoleCommandLogDump.toIntArray(List.of()));
    assertNull(PSConsoleCommandLogDump.toIntArray(new ArrayList<>()));
  }

  @Test
  @DisplayName("toIntArray copies typed Integer values in order")
  void toIntArrayCopiesValues() {
    List<Integer> values = new ArrayList<>();
    values.add(1);
    values.add(4);
    values.add(0);
    assertArrayEquals(new int[] {1, 4, 0}, PSConsoleCommandLogDump.toIntArray(values));
  }

  @Test
  @DisplayName("empty args leave query types and application ids unrestricted")
  void emptyArgsLeaveFiltersNull() throws Exception {
    PSConsoleCommandLogDump cmd = new PSConsoleCommandLogDump("");
    assertNull(cmd.getFilterEntryTypes());
    assertNull(cmd.getFilterApplicationIds());
    assertTrue(cmd.getRecipients().isEmpty());
  }

  @Test
  @DisplayName("type tokens populate the typed query-type filter")
  void parsesTypedQueryTypes() throws Exception {
    PSConsoleCommandLogDump cmd = new PSConsoleCommandLogDump("type 1 type 4");
    assertArrayEquals(new int[] {1, 4}, cmd.getFilterEntryTypes());
    assertNull(cmd.getFilterApplicationIds());
    assertTrue(cmd.getRecipients().isEmpty());
  }

  @Test
  @DisplayName("server token maps to application id 0")
  void parsesServerAsApplicationZero() throws Exception {
    PSConsoleCommandLogDump cmd = new PSConsoleCommandLogDump("type 1 server");
    assertArrayEquals(new int[] {1}, cmd.getFilterEntryTypes());
    assertArrayEquals(new int[] {0}, cmd.getFilterApplicationIds());
  }

  @Test
  @DisplayName("application token populates the typed application-id filter")
  void parsesTypedApplicationIds() throws Exception {
    PSConsoleCommandLogDump cmd = new PSConsoleCommandLogDump("type 4 application 7");
    assertArrayEquals(new int[] {4}, cmd.getFilterEntryTypes());
    assertArrayEquals(new int[] {7}, cmd.getFilterApplicationIds());
    assertTrue(cmd.getRecipients().isEmpty());
  }

  @Test
  @DisplayName("non-numeric type token is rejected")
  void rejectsNonNumericType() {
    assertThrows(PSIllegalArgumentException.class, () -> new PSConsoleCommandLogDump("type abc"));
  }

  @Test
  @DisplayName("unknown token is rejected")
  void rejectsUnknownToken() {
    assertThrows(PSIllegalArgumentException.class, () -> new PSConsoleCommandLogDump("bogus 1"));
  }
}
