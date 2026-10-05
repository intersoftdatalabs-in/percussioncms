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
package com.percussion.webservices.content.impl;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.workflow.PSCloneInitialWorkflowState;
import java.util.function.IntUnaryOperator;

/**
 * Which {@link PSComponentSummary} drives the checkout step after {@code prepareForEdit}.
 *
 * <p>A workflow transition commits {@code CONTENTSTATUS} in its own transaction (#5246). The
 * summary loaded before that transition still shows the public state and an empty checkout user.
 * Checkout must use a summary reloaded after the commit, or the item is checked out twice.
 */
public final class PSPrepareForEditCheckout {

  private PSPrepareForEditCheckout() {}

  public static PSComponentSummary summaryForCheckout(
      boolean didTransition, PSComponentSummary loadedBeforeTransition, PSComponentSummary reloaded) {
    if (loadedBeforeTransition == null) {
      throw new IllegalArgumentException("loadedBeforeTransition is required");
    }
    if (didTransition) {
      if (reloaded == null) {
        throw new IllegalArgumentException("reloaded summary is required after a transition");
      }
      return reloaded;
    }
    return loadedBeforeTransition;
  }

  /**
   * Summary used to decide checkout. Always the refreshed row: item create and a failed check-in
   * can leave the second-level cache with an empty checkout user and {@code CONTENTSTATEID} 0,
   * and checkout then runs {@code sys_wfPerformTransition} against that cache (#5246).
   */
  public static PSComponentSummary summaryForCheckoutDecision(
      PSComponentSummary loaded, PSComponentSummary refreshed) {
    return summaryForCheckout(true, loaded, refreshed);
  }

  /**
   * Workflow state to persist when {@code stateId} is unset. {@code 0} when the item already has a
   * state, or when no initial state can be named.
   *
   * <p>Uses {@link PSCloneInitialWorkflowState#coerceStateId(int, int)} so a missing workflow id
   * falls through to the system default workflow. Item create can leave {@code CONTENTSTATEID}
   * null; checkout then throws {@code stateId must be > 0} (#5246).
   */
  public static int missingStateToAssign(int workflowId, int stateId) {
    if (stateId > 0) {
      return 0;
    }
    int coerced = PSCloneInitialWorkflowState.coerceStateId(workflowId, stateId);
    return coerced > 0 ? coerced : 0;
  }

  /**
   * Same decision with an explicit lookup. A non-positive workflow id does not consult the system
   * default; {@link #missingStateToAssign(int, int)} does.
   */
  static int missingStateToAssign(
      int workflowId, int stateId, IntUnaryOperator initialStateByWorkflowId) {
    if (stateId > 0) {
      return 0;
    }
    int coerced =
        PSCloneInitialWorkflowState.coerceStateId(workflowId, stateId, initialStateByWorkflowId);
    return coerced > 0 ? coerced : 0;
  }
}
