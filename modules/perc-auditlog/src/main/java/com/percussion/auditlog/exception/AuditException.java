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

package com.percussion.auditlog.exception;

/**
 * Checked exception raised by the audit-log subsystem to signal that an audit event could not be
 * recorded or delivered to the configured sink. Carries either a message, a cause, or both,
 * mirroring the standard {@link Exception} triad.
 */
public class AuditException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs an audit exception with both a message and an underlying cause.
   *
   * @param message human-readable description of the failure, may be {@code null}.
   * @param e the underlying cause, may be {@code null}.
   */
  public AuditException(String message, Throwable e) {
    super(message, e);
  }

  /**
   * Constructs an audit exception with the given message.
   *
   * @param message human-readable description of the failure, may be {@code null}.
   */
  public AuditException(String message) {
    super(message);
  }

  /**
   * Constructs an audit exception wrapping an underlying cause.
   *
   * @param e the underlying cause, may be {@code null}.
   */
  public AuditException(Throwable e) {
    super(e);
  }
}
