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
package com.percussion.itemmanagement.community;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemCommunityAssignmentRulesTest {

  @Test
  void acceptsADifferentListedCommunity() {
    assertEquals(
        ItemCommunityAssignmentRules.Reason.OK,
        ItemCommunityAssignmentRules.decide("20", 10, Set.of(10, 20)));
  }

  @Test
  void rejectsBlankMalformedUnchangedAndForbidden() {
    assertEquals(
        ItemCommunityAssignmentRules.Reason.BLANK,
        ItemCommunityAssignmentRules.decide("  ", 10, Set.of(20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.MALFORMED,
        ItemCommunityAssignmentRules.decide("nope", 10, Set.of(20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.MALFORMED,
        ItemCommunityAssignmentRules.decide("0", 10, Set.of(20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.MALFORMED,
        ItemCommunityAssignmentRules.decide("-1", 10, Set.of(20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.UNCHANGED,
        ItemCommunityAssignmentRules.decide("10", 10, Set.of(10, 20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.FORBIDDEN,
        ItemCommunityAssignmentRules.decide("99", 10, Set.of(10, 20)));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.FORBIDDEN,
        ItemCommunityAssignmentRules.decide("20", 10, null));
    assertEquals(
        ItemCommunityAssignmentRules.Reason.FORBIDDEN,
        ItemCommunityAssignmentRules.decide("20", 10, List.of()));
  }
}
