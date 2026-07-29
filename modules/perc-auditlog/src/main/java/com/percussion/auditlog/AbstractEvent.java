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

/**
 * Base {@link AuditContext} shared by every Percussion audit event. Pre-populates the observer and
 * target resource URIs with the {@code service/bss/cms} system observer and defaults the action
 * outcome to {@link PSActionOutcome#UNKNOWN}. Sub-classes typically mutate the action and the
 * outcome in their own constructors before passing the event to {@link PSAuditLogService}.
 */
public class AbstractEvent extends AuditContext {

  private static final String SYSTEM_OBSERVER = "service/bss/cms";
  private String outcome;

  /**
   * Returns the action outcome recorded for this audit event.
   *
   * @return the outcome value, typically one of {@link PSActionOutcome}, never {@code null} after
   *     construction.
   */
  public String getOutcome() {
    return outcome;
  }

  /**
   * Sets the action outcome for this audit event.
   *
   * @param outcome the outcome value, typically one of {@link PSActionOutcome}, never {@code null}.
   */
  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  /** Constructs an event pre-populated with the system observer and an {@code UNKNOWN} outcome. */
  public AbstractEvent() {

    // Set some defaults
    this.setOutcome(PSActionOutcome.UNKNOWN.name());
    this.setObserverName(SYSTEM_OBSERVER);
    this.setTargetName(SYSTEM_OBSERVER);
  }
}
