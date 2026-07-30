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

/**
 * Common contract for Percussion audit event objects. Allows callers to retrieve the strongly-typed
 * action associated with an event without knowing the concrete subtype.
 */
public interface IPSAuditEvent {

  /**
   * Returns the action recorded for this event.
   *
   * @param <T> the concrete action enum type declared by an implementing event class.
   * @return the action, never {@code null}.
   */
  public <T> T getAction();
}
