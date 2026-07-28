/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
import java.util.Date;

/**
 * Pure field-by-field mapping from the workflow action inputs to a {@link PSContentStatusHistory}
 * entity. No DB access, no Spring calls, no service lookup.
 *
 * <p>This helper exists as a separate class (rather than a static method on
 * {@link PSContentStatusHistoryContext}) because loading {@code PSContentStatusHistoryContext}
 * triggers its {@code <clinit>} which calls
 * {@code PSConnectionMgr.getQualifiedIdentifier("CONTENTSTATUSHISTORY")} and therefore requires a
 * live database. Keeping the mapping helper in a separate class lets unit tests exercise the
 * mapping without booting a real DB.
 *
 * <p>Used by both the migrated {@code PSExitUpdateHistory} write path (#1561 Phase 2) and the
 * deprecated {@code PSContentStatusHistoryContext} write constructor so they produce identical
 * entities from identical inputs.
 *
 * <p>Transition-id / label rules: when {@code transitionContext} is {@code null} the helper
 * behaves as a check-in or check-out. It uses {@link IPSConstants#TRANSITIONID_CHECKINOUT} as the
 * transition id and labels the row {@code "CheckIn"} when no checkout user is set, or {@code
 * "CheckOut"} otherwise. When {@code transitionContext} is non-null the helper copies the
 * transition id and label from it.
 *
 * @see <a href="https://github.com/intersoftdatalabs-in/percussioncms/issues/1561">Issue #1561</a>
 */
public final class PSContentStatusHistoryEntityBuilder {

  private PSContentStatusHistoryEntityBuilder() {
    // utility class
  }

  /**
   * Builds a {@link PSContentStatusHistory} entity from the supplied inputs. The caller hands the
   * returned entity to {@code IPSSystemService#saveContentStatusHistory}.
   *
   * @param contentStatusHistoryID the desired id; pass a non-positive value to let
   *     {@code saveContentStatusHistory} allocate a new id via the global GUID manager.
   * @param workFlowID id of the workflow for this entry, written to {@code WORKFLOWAPPID}.
   * @param contentID id of the content item acted on, written to {@code CONTENTID}.
   * @param sessionID session id of the actor.
   * @param actorName user name that performed the action, written to {@code ACTOR}.
   * @param baseRevisionNum revision number of the content item for the action.
   * @param roleName comma-separated role names of assignees for the post-action state.
   * @param transitionComment descriptive comment for the action.
   * @param contentStatusContext the source of {@code TITLE}, {@code STATEID}, {@code CHECKOUTUSERNAME},
   *     {@code LASTMODIFIERNAME} and {@code LASTMODIFIEDDATE}. Must not be {@code null}.
   * @param statesContext the source of {@code STATENAME} and {@code VALID}. Must not be {@code null}.
   * @param transitionContext the transition context; pass {@code null} for check-in / check-out.
   * @return a populated entity with {@code EVENTTIME} set to the current time and {@code ID} set to
   *     the supplied value when positive, otherwise {@code -1L}.
   * @throws IllegalArgumentException if {@code contentStatusContext} or {@code statesContext} is
   *     {@code null}.
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

    if (null == transitionContext) {
      entity.setTransitionId(IPSConstants.TRANSITIONID_CHECKINOUT);
      if (null == contentStatusContext.getContentCheckedOutUserName()) {
        entity.setTransitionLabel("CheckIn");
      } else {
        entity.setTransitionLabel("CheckOut");
      }
    } else {
      entity.setTransitionId(transitionContext.getTransitionID());
      entity.setTransitionLabel(transitionContext.getTransitionLabel());
    }

    return entity;
  }
}