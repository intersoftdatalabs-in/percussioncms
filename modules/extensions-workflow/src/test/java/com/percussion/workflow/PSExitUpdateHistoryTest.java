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

import com.percussion.extension.IPSWorkFlowContext;
import org.junit.jupiter.api.Test;

/** Check-in must survive a skipped content-status history write (#5318). */
class PSExitUpdateHistoryTest {

  @Test
  void skippedHistoryWriteDoesNotFailCheckIn() {
    PSWorkFlowContext context = new PSWorkFlowContext(5, 551, 11, 0, 6);

    assertFalse(PSExitUpdateHistory.recordHistoryId(context, 0));
    assertEquals(IPSWorkFlowContext.WORKFLOW_CONTEXT_INITIAL_INTEGER_VALUE, context.getHistoryID());
  }

  @Test
  void writtenHistoryIdIsStored() {
    PSWorkFlowContext context = new PSWorkFlowContext(5, 551, 11, 0, 6);

    assertTrue(PSExitUpdateHistory.recordHistoryId(context, 42));
    assertEquals(42, context.getHistoryID());
  }
}
