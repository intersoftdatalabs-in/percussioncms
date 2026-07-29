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

package com.percussion.auditlog;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Audit event emitted when a workflow transition is applied to a content item. Carries the source
 * and destination state names, the affected content id and GUID, and the user who triggered the
 * transition.
 */
public class PSWorkflowEvent extends AbstractEvent {

  /** Tag identifying the integer content id of the item whose workflow was updated. */
  public static final String CONTENTID_TAG = "//percussion/contentid";

  /** Tag identifying the GUID of the item whose workflow was updated. */
  public static final String GUID_TAG = "//percussion/guid";

  /** Tag identifying the workflow state the item transitioned away from. */
  public static final String TRANSITIONFROM_TAG = "//percussion/transitionFrom";

  /** Tag identifying the workflow state the item transitioned to. */
  public static final String TRANSITIONTO_TAG = "//percussion/transitionTo";

  /** Enumerates the workflow transition actions recorded by {@link PSWorkflowEvent}. */
  public enum WorkflowEventActions {
    /** A workflow transition (update of state) was applied to a content item. */
    update
  }

  private int contentId;
  private String guid;
  private String transitionFrom;
  private String transitionTo;
  private WorkflowEventActions action;

  /**
   * Constructs a workflow event populated from the originating servlet request and metadata.
   *
   * @param transitionFrom the source workflow state name, never {@code null}.
   * @param transitionTo the destination workflow state name, never {@code null}.
   * @param action the workflow transition action, never {@code null}.
   * @param request the HTTP request that triggered the transition, never {@code null}.
   * @param content the integer content id of the affected item as a string, never {@code null}.
   * @param guid the GUID of the affected item, never {@code null}.
   * @param outcome the outcome of the transition, never {@code null}.
   */
  public PSWorkflowEvent(
      String transitionFrom,
      String transitionTo,
      WorkflowEventActions action,
      HttpServletRequest request,
      String content,
      String guid,
      String outcome) {

    this.setTargetUsername(request.getRemoteUser());
    this.setTransitionFrom(transitionFrom);
    this.setTransitionTo(transitionTo);
    this.setAction(action);
    this.setAgentName(request.getHeader("User-Agent"));
    this.setOutcome(outcome);
    this.setInitiatorIP(request.getRemoteAddr());
    this.setGuid(guid);
    this.setContentId(Integer.parseInt(content));
  }

  /**
   * Returns the action recorded for this event.
   *
   * @return the action, may be {@code null} when not yet set.
   */
  public WorkflowEventActions getAction() {
    return action;
  }

  /**
   * Sets the action recorded for this event.
   *
   * @param action the action to record, never {@code null}.
   */
  public void setAction(WorkflowEventActions action) {
    this.action = action;
  }

  /**
   * Returns the integer content id of the item whose workflow was updated.
   *
   * @return the content id.
   */
  public int getContentId() {
    return contentId;
  }

  /**
   * Sets the integer content id of the item whose workflow was updated.
   *
   * @param contentId the content id.
   */
  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  /**
   * Returns the GUID of the item whose workflow was updated.
   *
   * @return the GUID, may be {@code null} when not set.
   */
  public String getGuid() {
    return guid;
  }

  /**
   * Sets the GUID of the item whose workflow was updated.
   *
   * @param guid the GUID, may be {@code null}.
   */
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
   * Returns the source workflow state name recorded for this transition.
   *
   * @return the state name, may be {@code null} when not set.
   */
  public String getTransitionFrom() {
    return transitionFrom;
  }

  /**
   * Sets the source workflow state name for this transition.
   *
   * @param transitionFrom the source state name, may be {@code null}.
   */
  public void setTransitionFrom(String transitionFrom) {
    this.transitionFrom = transitionFrom;
  }

  /**
   * Returns the destination workflow state name recorded for this transition.
   *
   * @return the state name, may be {@code null} when not set.
   */
  public String getTransitionTo() {
    return transitionTo;
  }

  /**
   * Sets the destination workflow state name for this transition.
   *
   * @param transitionTo the destination state name, may be {@code null}.
   */
  public void setTransitionTo(String transitionTo) {
    this.transitionTo = transitionTo;
  }
}
