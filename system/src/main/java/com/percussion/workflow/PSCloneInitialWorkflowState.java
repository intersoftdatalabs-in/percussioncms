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

import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import java.sql.SQLException;
import java.util.function.IntUnaryOperator;

/**
 * Resolves a usable workflow {@code stateId} for a newly cloned item.
 *
 * <p>{@code PSCopyHandler} / {@code PSConditionalCloneHandler} insert a NewCopy
 * {@code CONTENTSTATUS} row. Hibernate can load that row with {@code
 * CONTENTSTATEID} 0 or null (clone insert UDF miss). {@code
 * sys_wfPerformTransition} then fails with {@code stateId must be > 0} (#3667 /
 * #3662). Clone auto-checkin is skipped; this helper still coerces remaining 0s
 * on {@code loadFromHibernate} and {@code commit()}.
 *
 * <p>When the copy already has a state, it is left unchanged. Otherwise the
 * workflow's initial state is used (looked up by workflow app id, not a
 * hand-built GUID). If the copy has no workflow id, the system default
 * workflow is used.
 */
public final class PSCloneInitialWorkflowState {

  private PSCloneInitialWorkflowState() {}

  /**
   * @param workflowId workflow application id; {@code <= 0} skips lookup unless
   *     a later production overload fills the default workflow
   * @param stateId current {@code CONTENTSTATEID}, {@code <= 0} if unset
   * @param initialStateByWorkflowId maps workflow app id → initial state id
   * @return {@code stateId} when already {@code > 0}, else the looked-up
   *     initial state when that is {@code > 0}, else the original {@code
   *     stateId}
   */
  public static int coerceStateId(
      int workflowId, int stateId, IntUnaryOperator initialStateByWorkflowId) {
    if (stateId > 0) {
      return stateId;
    }
    if (workflowId <= 0 || initialStateByWorkflowId == null) {
      return stateId;
    }
    int initial = initialStateByWorkflowId.applyAsInt(workflowId);
    return initial > 0 ? initial : stateId;
  }

  /**
   * Production lookup: {@link IPSCmsObjectMgr#loadWorkflowAppContext(int)} plus
   * the system default workflow when {@code workflowId} is unset.
   */
  public static int coerceStateId(int workflowId, int stateId) {
    int wf = workflowId > 0 ? workflowId : defaultWorkflowAppId();
    return coerceStateId(wf, stateId, PSCloneInitialWorkflowState::lookupInitialState);
  }

  static int lookupInitialState(int workflowAppId) {
    if (workflowAppId <= 0) {
      return 0;
    }
    try {
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      if (cms == null) {
        return 0;
      }
      return cms.loadWorkflowAppContext(workflowAppId)
          .map(PSCloneInitialWorkflowState::initialStateOf)
          .orElse(0);
    } catch (RuntimeException e) {
      return 0;
    }
  }

  private static int initialStateOf(IPSWorkflowAppsContext ctx) {
    try {
      return ctx.getWorkFlowInitialStateID();
    } catch (SQLException e) {
      return 0;
    }
  }

  static int defaultWorkflowAppId() {
    try {
      IPSGuid id =
          PSWorkflowServiceLocator.getWorkflowServiceSafely()
              .map(IPSWorkflowService::getDefaultWorkflowId)
              .orElse(null);
      return id == null ? 0 : id.getUUID();
    } catch (RuntimeException e) {
      return 0;
    }
  }
}
