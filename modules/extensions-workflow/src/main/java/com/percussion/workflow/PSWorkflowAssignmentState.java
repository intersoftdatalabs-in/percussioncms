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

/**
 * Which workflow state supplies role rows after an action.
 *
 * <p>Check-in and checkout leave the to-state at {@code 0}. That value means the action did not
 * move the item, not that the item has no state. Role lookup with {@code 0} throws {@code stateId
 * must be > 0} and fails the whole check-in or checkout (#5246).
 */
public final class PSWorkflowAssignmentState {

  private PSWorkflowAssignmentState() {}

  /**
   * @param fromStateId state the item was in when the action started
   * @param toStateId state the action moved the item to; {@code <= 0} when the action did not
   *     change state
   * @return {@code toStateId} when it is a different positive state, otherwise {@code fromStateId}
   */
  public static int stateIdForRoles(int fromStateId, int toStateId) {
    if (toStateId > 0 && toStateId != fromStateId) {
      return toStateId;
    }
    return fromStateId;
  }
}
