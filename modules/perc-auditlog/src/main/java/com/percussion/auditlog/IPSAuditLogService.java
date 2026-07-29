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

import com.ibm.cadf.middleware.AuditContext;
import com.ibm.cadf.model.Event;

/** Defines the interface for the audit log service */
public interface IPSAuditLogService {

  /**
   * Records a content lifecycle event (create, update, delete, publish/recycle) with the active
   * audit sink.
   *
   * @param event the fully-populated content event to log, never {@code null}.
   */
  public void logContentEvent(PSContentEvent event);

  /**
   * Records a workflow transition event (e.g., state changes) with the active audit sink.
   *
   * @param event the fully-populated workflow event to log, never {@code null}.
   */
  public void logWorkflowEvent(PSWorkflowEvent event);

  /**
   * Records an authentication lifecycle event (login, renew, revoke, logout) with the active audit
   * sink.
   *
   * @param event the fully-populated authentication event to log, never {@code null}.
   */
  public void logAuthenticationEvent(PSAuthenticationEvent event);

  /**
   * Records a user-management event (create, update, delete, disable, revoke) with the active audit
   * sink.
   *
   * @param event the fully-populated user-management event to log, never {@code null}.
   */
  public void logUserManagementEvent(PSUserManagementEvent event);

  /**
   * Builds an IBM CADF {@link Event} from a generic audit context, action name, and outcome.
   *
   * @param event the source audit context carrying resource and initiator metadata, never {@code
   *     null}.
   * @param action the action name emitted for the resulting event, never {@code null}.
   * @param outcome the outcome name emitted for the resulting event, never {@code null}.
   * @return the constructed CADF event, never {@code null}.
   */
  public Event createEvent(AuditContext event, String action, String outcome);
}
