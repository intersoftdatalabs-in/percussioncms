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
package com.percussion.services.audit;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditLogService;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Thin facade for system code to emit system-wide audit events without depending on Spring at the
 * call site. Uses {@link DefaultAuditLogService.Holder} (Log4j + memory until JPA repository
 * registers).
 *
 * <p>Phase 2b: use {@link #logLegacyIfAuditable(int, AuditContext, Object...)} for legacy {@code
 * IPS*Errors} ints so non-auditable codes never dual-write.
 */
public final class PSSystemAuditLogger {

  private PSSystemAuditLogger() {}

  public static AuditLogService service() {
    return DefaultAuditLogService.Holder.get();
  }

  public static void log(
      SystemErrorCode code, AuditContext context, AuditOutcome outcome, Object... params) {
    service().log(code, context == null ? AuditContext.empty() : context, outcome, params);
  }

  /**
   * Resolve a legacy {@code IPS*Errors} int via {@link LegacyErrorCodeRegistry} and dual-write only
   * when the catalog marks the code {@code isAuditable}. Unregistered or non-auditable codes are a
   * no-op (returns the skipped audit id).
   */
  public static AuditLogId logLegacyIfAuditable(
      int legacyErrorCode, AuditContext context, Object... params) {
    return LegacyErrorCodeRegistry.logIfAuditable(
        service(), legacyErrorCode, context == null ? AuditContext.empty() : context, params);
  }

  /**
   * Same as {@link #logLegacyIfAuditable(int, AuditContext, Object...)} with an explicit outcome.
   */
  public static AuditLogId logLegacyIfAuditable(
      int legacyErrorCode, AuditContext context, AuditOutcome outcome, Object... params) {
    return LegacyErrorCodeRegistry.logIfAuditable(
        service(),
        legacyErrorCode,
        context == null ? AuditContext.empty() : context,
        outcome,
        params);
  }

  public static void loginSuccess(HttpServletRequest request, String username) {
    String actor = nullToEmpty(username);
    String ip = clientIp(request);
    log(
        AuthenticationErrorCodes.LOGIN_SUCCESS,
        context(request, actor),
        AuditOutcome.SUCCESS,
        actor,
        ip);
  }

  public static void loginFailure(HttpServletRequest request, String username, String reason) {
    String actor = nullToEmpty(username);
    String ip = clientIp(request);
    log(
        AuthenticationErrorCodes.LOGIN_FAILURE,
        context(request, actor),
        AuditOutcome.FAILURE,
        actor,
        nullToEmpty(reason),
        ip);
  }

  public static void logout(HttpServletRequest request, String username) {
    String actor = nullToEmpty(username);
    String ip = clientIp(request);
    log(
        AuthenticationErrorCodes.LOGOUT,
        context(request, actor),
        AuditOutcome.SUCCESS,
        actor,
        ip);
  }

  private static AuditContext context(HttpServletRequest request, String actor) {
    AuditContext.Builder b = AuditContext.builder().actor(actor);
    if (request != null) {
      b.sourceIp(clientIp(request)).sourceHost(request.getRemoteHost());
    }
    return b.build();
  }

  private static String clientIp(HttpServletRequest request) {
    if (request == null) {
      return "";
    }
    return nullToEmpty(request.getRemoteAddr());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
