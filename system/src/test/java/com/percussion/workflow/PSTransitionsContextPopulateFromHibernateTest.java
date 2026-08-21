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

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Hibernate factory cursor must sit on the first row (JDBC constructor parity). {@code
 * PSExitPerformTransition} reads {@link PSTransitionsContext#getTransitionFromStateID()} without
 * calling {@code moveNext()} first; a cursor at {@code -1} left from-state {@code 0} and made
 * Explorer Expire (and every other trigger) HTTP 500 {@code INVALID_TRANSITION} (#3668).
 */
@Tag("UnitTest")
class PSTransitionsContextPopulateFromHibernateTest {

  @Test
  void emptyRowsLeaveCountZeroAndFromStateUnset() {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(6, List.of());

    assertTrue(ctx.isEmpty());
    assertEquals(0, ctx.getTransitionCount());
    assertEquals(0, ctx.getTransitionFromStateID());
  }

  @Test
  void firstRowIsReadableWithoutMoveNext() {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(
        6, List.of(transition(8, "Expire", 3, 5), transition(9, "Quick Edit", 3, 4)));

    assertFalse(ctx.isEmpty());
    assertEquals(2, ctx.getTransitionCount());
    assertEquals(8, ctx.getTransitionID());
    assertEquals("Expire", ctx.getTransitionActionTrigger());
    assertEquals(3, ctx.getTransitionFromStateID());
    assertEquals(5, ctx.getTransitionToStateID());
  }

  @Test
  void moveNextAdvancesPastTheAlreadyPositionedFirstRow() throws SQLException {
    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(
        6, List.of(transition(8, "Expire", 3, 5), transition(9, "Quick Edit", 3, 4)));

    assertTrue(ctx.moveNext());
    assertEquals(9, ctx.getTransitionID());
    assertEquals("Quick Edit", ctx.getTransitionActionTrigger());
    assertEquals(4, ctx.getTransitionToStateID());
    assertFalse(ctx.moveNext());
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
