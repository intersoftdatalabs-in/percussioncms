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
package com.intsof.percussioncms.auditlog;

/**
 * System-wide audit log entry point. Dual-writes auditable events to {@code server.log} and the
 * durable repository. Non-auditable codes are ignored for sinks (no audit row).
 */
public interface AuditLogService {

  /**
   * Log using the code's {@link SystemErrorCode#defaultOutcome()} and empty context when omitted.
   *
   * @return log id if an audit write was performed; empty id sentinel when skipped (not auditable)
   */
  AuditLogId log(SystemErrorCode code, Object... params);

  /** Log with context and default outcome from the code. */
  AuditLogId log(SystemErrorCode code, AuditContext context, Object... params);

  /** Log with explicit outcome. */
  AuditLogId log(
      SystemErrorCode code, AuditContext context, AuditOutcome outcome, Object... params);
}
