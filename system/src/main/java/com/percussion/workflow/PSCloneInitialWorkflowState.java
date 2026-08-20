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
import com.percussion.util.PSPreparedStatement;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jdbc.PSConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Resolves a usable workflow {@code stateId} for a newly cloned item.
 *
 * <p>{@code PSCopyHandler} / {@code PSConditionalCloneHandler} insert a NewCopy
 * {@code CONTENTSTATUS} row, then check it in. Hibernate can load that row with
 * {@code CONTENTSTATEID} 0 or null (clone insert UDF miss). {@code
 * sys_wfPerformTransition} then fails with {@code stateId must be > 0} (#3667 /
 * #3662).
 *
 * <p>When the copy already has a state, it is left unchanged. Otherwise the
 * workflow's initial state is used (looked up by workflow app id, not a
 * hand-built GUID). If the copy has no workflow id, the system default
 * workflow is used.
 */
public final class PSCloneInitialWorkflowState {

  private static final Logger log = LogManager.getLogger(PSCloneInitialWorkflowState.class);

  private static final String ASSIGN_INITIAL_STATE_SQL =
      "UPDATE CONTENTSTATUS SET CONTENTSTATEID = ?, "
          + "WORKFLOWAPPID = CASE WHEN WORKFLOWAPPID IS NULL OR WORKFLOWAPPID <= 0 THEN ? ELSE WORKFLOWAPPID END "
          + "WHERE CONTENTID = ? AND (CONTENTSTATEID IS NULL OR CONTENTSTATEID <= 0)";

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

  /**
   * Writes the workflow initial state onto a just-inserted {@code CONTENTSTATUS} row (JDBC, same
   * table as the clone insert) and evicts the Hibernate summary so check-in does not see state 0.
   *
   * @return {@code true} when the item can be checked in with {@code stateId > 0}
   */
  public static boolean assignOnContentStatus(int contentId) {
    if (contentId <= 0) {
      return false;
    }
    int wf = 0;
    try {
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      if (cms != null) {
        var sum = cms.loadComponentSummary(contentId);
        if (sum != null) {
          wf = sum.getWorkflowAppId();
        }
      }
    } catch (RuntimeException e) {
      log.debug("Clone CONTENTSTATUS summary not in Hibernate yet for {}", contentId, e);
    }
    if (wf <= 0) {
      wf = defaultWorkflowAppId();
    }
    int initial = lookupInitialState(wf);
    if (initial <= 0) {
      log.warn(
          "Cannot assign workflow initial state for clone contentId={} workflowId={}",
          contentId,
          wf);
      return false;
    }
    Connection conn = null;
    PreparedStatement stmt = null;
    try {
      conn = PSConnectionHelper.getDbConnection();
      stmt = PSPreparedStatement.getPreparedStatement(conn, ASSIGN_INITIAL_STATE_SQL);
      stmt.setInt(1, initial);
      stmt.setInt(2, wf);
      stmt.setInt(3, contentId);
      stmt.executeUpdate();
    } catch (Exception e) {
      log.warn("Failed JDBC assign of clone workflow state for contentId={}", contentId, e);
      return false;
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException ignored) {
          // close
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException ignored) {
          // close
        }
      }
    }
    try {
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      if (cms != null) {
        cms.evictComponentSummaries(List.of(contentId));
      }
    } catch (RuntimeException e) {
      log.debug("Could not evict clone summary {}", contentId, e);
    }
    return true;
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
