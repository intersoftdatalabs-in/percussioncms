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

import com.percussion.cms.IPSConstants;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.services.workflow.data.PSTransition;
import java.util.Date;

/**
 * Pure field-by-field mapping from the workflow action inputs to a {@link PSContentStatusHistory}
 * entity. No DB access, no Spring calls, no service lookup.
 *
 * <p>This helper exists as a separate class (rather than a static method on {@link
 * PSContentStatusHistoryContext}) so that unit tests can exercise the mapping without dragging in
 * the legacy context class. As of #1561 Phase 4d-1d the context's class initializer no longer
 * touches the database (table name is inlined as an uppercase constant), but the helper is still
 * kept separate so test classes can load it without the rest of the legacy workflow context
 * surface.
 *
 * <p>Used by both the migrated {@code PSExitUpdateHistory} write path (#1561 Phases 2 and 3) and
 * the deprecated {@code PSContentStatusHistoryContext} write constructor so they produce identical
 * entities from identical inputs.
 *
 * <p>Transition-id / label rules: when the transition argument is {@code null} the helper behaves
 * as a check-in or check-out. It uses {@link IPSConstants#TRANSITIONID_CHECKINOUT} as the
 * transition id and labels the row {@code "CheckIn"} when no checkout user is set, or {@code
 * "CheckOut"} otherwise. When the transition argument is non-null the helper copies the transition
 * id and label from it.
 *
 * @see <a href="https://github.com/intersoftdatalabs-in/percussioncms/issues/1561">Issue #1561</a>
 */
public final class PSContentStatusHistoryEntityBuilder {

  private PSContentStatusHistoryEntityBuilder() {
    // utility class
  }

  /**
   * Legacy overload kept for binary compatibility with the deprecated {@code
   * PSContentStatusHistoryContext} write constructor (passes a raw {@link IPSTransitionsContext}
   * cursor). Delegates to the {@link PSTransition} overload by extracting id and label from the
   * cursor.
   */
  public static PSContentStatusHistory build(
      int contentStatusHistoryID,
      int workFlowID,
      int contentID,
      String sessionID,
      String actorName,
      int baseRevisionNum,
      String roleName,
      String transitionComment,
      IPSContentStatusContext contentStatusContext,
      IPSStatesContext statesContext,
      IPSTransitionsContext transitionContext) {
    PSTransition transition = null;
    if (transitionContext != null) {
      transition = new PSTransition();
      transition.setGUID(
          new com.percussion.services.guidmgr.data.PSGuid(
              com.percussion.services.catalog.PSTypeEnum.WORKFLOW_TRANSITION,
              transitionContext.getTransitionID()));
      transition.setLabel(transitionContext.getTransitionLabel());
    }
    return build(
        contentStatusHistoryID,
        workFlowID,
        contentID,
        sessionID,
        actorName,
        baseRevisionNum,
        roleName,
        transitionComment,
        contentStatusContext,
        statesContext,
        transition);
  }

  /**
   * Overload that accepts the Hibernate-managed {@link PSTransition} DTO returned by {@code
   * PSWorkflowService#loadWorkflowTransition(...)}. Used by {@code PSExitUpdateHistory} since #1561
   * Phase 3.
   */
  public static PSContentStatusHistory build(
      int contentStatusHistoryID,
      int workFlowID,
      int contentID,
      String sessionID,
      String actorName,
      int baseRevisionNum,
      String roleName,
      String transitionComment,
      IPSContentStatusContext contentStatusContext,
      IPSStatesContext statesContext,
      PSTransition transition) {
    if (null == contentStatusContext) {
      throw new IllegalArgumentException(
          "contentStatusContext is null in PSContentStatusHistoryEntityBuilder.");
    }
    if (null == statesContext) {
      throw new IllegalArgumentException(
          "statesContext is null in PSContentStatusHistoryEntityBuilder.");
    }

    PSContentStatusHistory entity = new PSContentStatusHistory();
    // -1 (or any non-positive value) lets saveContentStatusHistory allocate a new ID.
    entity.setId(contentStatusHistoryID <= 0 ? -1L : contentStatusHistoryID);
    entity.setWorkflowId(workFlowID);
    entity.setContentId(contentID);
    entity.setSessionId(sessionID);
    entity.setActor(actorName);
    entity.setRevision(baseRevisionNum);
    entity.setRoleName(roleName);
    entity.setTransitionComment(transitionComment);
    entity.setIsValidValue(statesContext.getIsValid() ? "Y" : "N");
    entity.setStateId(contentStatusContext.getContentStateID());
    entity.setStateName(statesContext.getStateName());
    entity.setTitle(contentStatusContext.getTitle());
    entity.setCheckoutUserName(contentStatusContext.getContentCheckedOutUserName());
    entity.setLastModifierName(contentStatusContext.getContentLastModifierName());
    entity.setLastModifiedDate(contentStatusContext.getContentLastModifiedDate());
    entity.setEventTime(new Date());

    if (null == transition) {
      // Check-in / check-out — no transition row.
      entity.setTransitionId(IPSConstants.TRANSITIONID_CHECKINOUT);
      entity.setTransitionLabel(
          contentStatusContext.getContentCheckedOutUserName() == null ? "CheckIn" : "CheckOut");
    } else {
      // PSTransition exposes the transition id via its GUID (workflow GUID type) and the
      // TRANSITIONLABEL via getLabel(). The legacy IPSTransitionsContext cursor exposed
      // them directly, which the overload above translates.
      entity.setTransitionId((int) transition.getGUID().longValue());
      entity.setTransitionLabel(transition.getLabel());
    }

    return entity;
  }
}
