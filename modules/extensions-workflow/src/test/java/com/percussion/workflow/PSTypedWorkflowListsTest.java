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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed list helpers used by {@link PSWorkflowRoleInfoStatic}. */
public class PSTypedWorkflowListsTest {

  @Test
  void intersectLists_returnsNullWhenEitherListIsNull() {
    assertNull(PSTypedWorkflowLists.intersectLists(null, List.of(1)));
    assertNull(PSTypedWorkflowLists.intersectLists(List.of(1), null));
    assertNull(PSTypedWorkflowLists.intersectLists(null, null));
  }

  @Test
  void intersectLists_preservesOrderOfFirstListAndDropsNonMembers() {
    List<Integer> result =
        PSTypedWorkflowLists.intersectLists(List.of(1, 2, 3, 4), List.of(4, 2, 9));
    assertEquals(List.of(2, 4), result);
  }

  @Test
  void intersectLists_emptyWhenEitherSideEmpty() {
    List<String> result =
        PSTypedWorkflowLists.intersectLists(Collections.emptyList(), List.of("a"));
    assertTrue(result.isEmpty());
  }

  @Test
  void filterList_retainsOnlyTrueMappedKeys() {
    Map<Integer, Boolean> map = new HashMap<>();
    map.put(1, true);
    map.put(2, false);
    map.put(3, true);
    List<Integer> result = PSTypedWorkflowLists.filterList(List.of(1, 2, 3, 4), map);
    assertEquals(List.of(1, 3), result);
  }

  @Test
  void filterList_returnsNullForNullOrEmptyMap() {
    assertNull(PSTypedWorkflowLists.filterList(List.of(1), null));
    assertNull(PSTypedWorkflowLists.filterList(List.of(1), Collections.emptyMap()));
    assertNull(PSTypedWorkflowLists.filterList(null, Map.of(1, true)));
  }

  @Test
  void lowerCaseList_lowerCasesStringsAndPreservesNulls() {
    List<String> input = Arrays.asList("Admin", null, "Editor");
    List<String> result = PSTypedWorkflowLists.lowerCaseList(input);
    assertEquals(Arrays.asList("admin", null, "editor"), result);
  }

  @Test
  void lowerCaseList_returnsNullForNullInput() {
    assertNull(PSTypedWorkflowLists.lowerCaseList(null));
  }
}
