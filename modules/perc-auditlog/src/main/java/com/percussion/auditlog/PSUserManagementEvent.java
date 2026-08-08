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
 * Audit event emitted for user-management administrative actions (create, update, delete, disable,
 * revoke) initiated through the CMS console. Callers may attach additional tags at the time the
 * event is constructed.
 *
 * <p>This class is {@code final}; the constructor assigns the action field directly and uses {@code
 * final} parent setters after {@code super()} completes (no {@code this-escape}).
 */
public final class PSUserManagementEvent extends AbstractEvent {
  // Add any user specific tags here that would be useful to an auditor

  /** Enumerates the user-management lifecycle actions recorded by {@link PSUserManagementEvent}. */
  public enum UserEventActions {
    /** A new user account was created. */
    create,
    /** An existing user account was updated. */
    update,
    /** An existing user account was removed. */
    delete,
    /** An existing user account was disabled. */
    disable,
    /** Permissions previously granted to a user were revoked. */
    revoke
  }

  private UserEventActions action;

  /**
   * Returns the action recorded for this event.
   *
   * @return the action, may be {@code null} when not yet set.
   */
  public UserEventActions getAction() {
    return action;
  }

  /**
   * Sets the action recorded for this event.
   *
   * @param action the action to record, never {@code null}.
   */
  public void setAction(UserEventActions action) {
    this.action = action;
  }

  /**
   * Constructs a user-management event populated from the originating servlet request.
   *
   * <p>Own fields are assigned directly; parent CADF fields use {@code final} setters after {@code
   * super()} completes.
   *
   * @param request the HTTP request that triggered the event, never {@code null}.
   * @param action the user-management action being recorded, never {@code null}.
   * @param outcome the outcome of the action, never {@code null}.
   */
  public PSUserManagementEvent(
      HttpServletRequest request, UserEventActions action, PSActionOutcome outcome) {
    super();
    this.action = action;
    setIniatorName(request.getRemoteUser());
    setInitiatorIP(request.getRemoteAddr());
    setTargetName(request.getRemoteUser());
    setOutcome(outcome.name());
    setAgentName(request.getHeader("User-Agent"));
  }
}
