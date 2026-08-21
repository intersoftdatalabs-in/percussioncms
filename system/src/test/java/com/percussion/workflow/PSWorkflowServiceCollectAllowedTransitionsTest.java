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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import com.percussion.services.workflow.impl.PSWorkflowService;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@link PSWorkflowService#collectAllowedTransitions} must include the first Hibernate row.
 * {@code populateFromHibernate} sits on row 0 (JDBC constructor parity, #3668); {@code while
 * (tc.moveNext())} skipped Expire / the only allowed SOAP transition (#3683).
 */
@Tag("UnitTest")
class PSWorkflowServiceCollectAllowedTransitionsTest {

  @Test
  void emptyContextReturnsEmptyList() throws SQLException {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(6, List.of());

    List<PSTransitionInfo> out =
        PSWorkflowService.collectAllowedTransitions(
            ctx, true, "alice", List.of("Admin"), "Admin", List.of());

    assertTrue(out.isEmpty());
  }

  @Test
  void nullContextReturnsEmptyList() throws SQLException {
    List<PSTransitionInfo> out =
        PSWorkflowService.collectAllowedTransitions(
            null, true, "alice", List.of("Admin"), "Admin", List.of());
    assertTrue(out.isEmpty());
  }

  @Test
  void singleRowIsIncludedWithoutCallingMoveNextFirst() throws SQLException {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(6, List.of(transition(8, "Expire", 3, 5)));

    List<PSTransitionInfo> out =
        PSWorkflowService.collectAllowedTransitions(
            ctx, true, "alice", List.of("Admin"), "Admin", List.of());

    assertEquals(1, out.size());
    assertEquals(8, out.get(0).getId());
    assertEquals("Expire", out.get(0).getTrigger());
    assertEquals(5, out.get(0).getToStateId());
  }

  @Test
  void twoRowsAreBothIncluded() throws SQLException {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(
        6, List.of(transition(8, "Expire", 3, 5), transition(9, "Quick Edit", 3, 4)));

    List<PSTransitionInfo> out =
        PSWorkflowService.collectAllowedTransitions(
            ctx, true, "alice", List.of("Admin"), "Admin", List.of());

    assertEquals(2, out.size());
    assertEquals(8, out.get(0).getId());
    assertEquals("Expire", out.get(0).getTrigger());
    assertEquals(9, out.get(1).getId());
    assertEquals("Quick Edit", out.get(1).getTrigger());
  }

  private static PSTransition transition(int id, String trigger, long fromState, long toState) {
    PSTransition t = new PSTransition();
    t.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, id));
    t.setLabel(trigger);
    t.setDescription(trigger);
    t.setTrigger(trigger);
    t.setStateId(fromState);
    t.setToState(toState);
    t.setApprovals(1);
    t.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    t.setAllowAllRoles(true);
    t.setTransitionAction("");
    return t;
  }
}
