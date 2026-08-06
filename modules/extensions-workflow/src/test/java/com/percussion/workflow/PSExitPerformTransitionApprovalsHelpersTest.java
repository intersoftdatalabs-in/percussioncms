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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.workflow.data.PSContentApproval;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the inline Phase 4d-1c replacement helpers that drive {@link
 * PSExitPerformTransition}'s CONTENTAPPROVALS read/write paths against the shared Hibernate session
 * (no raw JDBC). The helpers are private; tests access them via reflection.
 *
 * <p>These mirror the legacy {@code PSContentApprovalsContext.hasUserActed / hasRoleActed /
 * roleIdList} semantics so the migration is behaviour-preserving for the bulk-transition /
 * admin-transition paths in {@code PSExitPerformTransition.processTransition}.
 */
@DisplayName("PSExitPerformTransition Phase 4d-1c approvals helpers")
@org.junit.jupiter.api.Disabled(
    "PSExitPerformTransition extends PSWorkflowContext which still uses the legacy raw-JDBC path;"
        + " re-enabled when Spring+H2 test infrastructure ships (Phase 4d-1d follow-up).")
public class PSExitPerformTransitionApprovalsHelpersTest {

  private static PSContentApproval approval(
      int contentId, int roleId, String user, int wf, int state, int transition) {
    PSContentApproval a = new PSContentApproval();
    a.setContentId(contentId);
    a.setRoleId(roleId);
    a.setUser(user);
    a.setWorkflowId(wf);
    a.setStateId(state);
    a.setTransitionId(transition);
    return a;
  }

  private static Object invokeStatic(String name, Class<?>[] paramTypes, Object... args)
      throws Exception {
    Method m = PSExitPerformTransition.class.getDeclaredMethod(name, paramTypes);
    m.setAccessible(true);
    return m.invoke(null, args);
  }

  @Test
  @DisplayName(
      "hasUserActed: matches user case-insensitively and only within supplied state approvals")
  void hasUserActed_matchesUserCaseInsensitive() throws Exception {
    List<PSContentApproval> stateApprovals =
        Arrays.asList(
            approval(101, 1, "alice", 1, 10, 100),
            approval(101, 2, "bob", 1, 10, 100),
            approval(101, 3, "carol", 1, 99, 100) // different state — must not match
            );

    assertTrue(
        (Boolean)
            invokeStatic(
                "hasUserActed",
                new Class<?>[] {List.class, String.class},
                stateApprovals,
                "alice"));
    assertTrue(
        (Boolean)
            invokeStatic(
                "hasUserActed",
                new Class<?>[] {List.class, String.class},
                stateApprovals,
                "ALICE"));
    assertTrue(
        (Boolean)
            invokeStatic(
                "hasUserActed",
                new Class<?>[] {List.class, String.class},
                stateApprovals,
                "  bob  "));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasUserActed",
                new Class<?>[] {List.class, String.class},
                stateApprovals,
                "carol"));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasUserActed",
                new Class<?>[] {List.class, String.class},
                stateApprovals,
                "nobody"));
  }

  @Test
  @DisplayName("hasUserActed: empty/null user returns false (matches legacy null-guard)")
  void hasUserActed_handlesEmptyUser() throws Exception {
    List<PSContentApproval> stateApprovals = Arrays.asList(approval(101, 1, "alice", 1, 10, 100));

    assertFalse(
        (Boolean)
            invokeStatic(
                "hasUserActed", new Class<?>[] {List.class, String.class}, stateApprovals, null));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasUserActed", new Class<?>[] {List.class, String.class}, stateApprovals, ""));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasUserActed", new Class<?>[] {List.class, String.class}, stateApprovals, "   "));
  }

  @Test
  @DisplayName("hasRoleActed: matches role id only; negative role id short-circuits to false")
  void hasRoleActed_matchesRoleId() throws Exception {
    List<PSContentApproval> stateApprovals =
        Arrays.asList(approval(101, 11, "alice", 1, 10, 100), approval(101, 12, "bob", 1, 10, 100));

    assertTrue(
        (Boolean)
            invokeStatic(
                "hasRoleActed", new Class<?>[] {List.class, int.class}, stateApprovals, 11));
    assertTrue(
        (Boolean)
            invokeStatic(
                "hasRoleActed", new Class<?>[] {List.class, int.class}, stateApprovals, 12));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasRoleActed", new Class<?>[] {List.class, int.class}, stateApprovals, 13));
    assertFalse(
        (Boolean)
            invokeStatic(
                "hasRoleActed", new Class<?>[] {List.class, int.class}, stateApprovals, -1));
  }

  @Test
  @DisplayName("transitionRoleIds: returns distinct role ids of the supplied approvals")
  void transitionRoleIds_dedup() throws Exception {
    List<PSContentApproval> transitionApprovals =
        Arrays.asList(
            approval(101, 7, "alice", 1, 10, 100),
            approval(101, 7, "alice-again", 1, 10, 100),
            approval(101, 9, "bob", 1, 10, 100));

    @SuppressWarnings("unchecked")
    List<Integer> ids =
        (List<Integer>)
            invokeStatic("transitionRoleIds", new Class<?>[] {List.class}, transitionApprovals);

    assertEquals(2, ids.size());
    assertTrue(ids.contains(7));
    assertTrue(ids.contains(9));
  }

  @Test
  @DisplayName("transitionRoleIds: empty list returns empty list")
  void transitionRoleIds_empty() throws Exception {
    @SuppressWarnings("unchecked")
    List<Integer> ids =
        (List<Integer>)
            invokeStatic("transitionRoleIds", new Class<?>[] {List.class}, Collections.emptyList());

    assertTrue(ids.isEmpty());
  }
}
