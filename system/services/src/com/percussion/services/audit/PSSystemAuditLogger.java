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
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UserManagementErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Thin facade for system and sitemanage code to emit system-wide audit events without depending on
 * Spring at the call site. Uses {@link DefaultAuditLogService.Holder} (Log4j + memory until JPA
 * repository registers). Production API is {@code com.intsof.percussioncms.auditlog} only (CADF /
 * former file-JSON audit types removed in Phase 2c).
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

  /**
   * Session near-timeout revoke (formerly {@code PSAuthenticationEvent} with action {@code
   * revoke}).
   */
  public static void sessionRevoke(HttpServletRequest request, String username) {
    String actor = nullToEmpty(username);
    String ip = clientIp(request);
    log(
        AuthenticationErrorCodes.SESSION_REVOKE,
        context(request, actor),
        AuditOutcome.SUCCESS,
        actor,
        ip);
  }

  /** Content create / update / delete / recycle / schedule dual-write. */
  public static void content(
      ContentErrorCodes code,
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    String g = nullToEmpty(guid);
    String cid = nullToEmpty(contentId);
    String p = nullToEmpty(path);
    AuditOutcome o = outcome != null ? outcome : code.defaultOutcome();
    log(code, contentContext(request, g), o, g, cid, p);
  }

  public static void contentCreate(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.CREATE, request, outcome, guid, contentId, path);
  }

  public static void contentUpdate(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.UPDATE, request, outcome, guid, contentId, path);
  }

  public static void contentDelete(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.DELETE, request, outcome, guid, contentId, path);
  }

  public static void contentRecycle(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.RECYCLE, request, outcome, guid, contentId, path);
  }

  public static void pagePublishSchedule(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.PAGE_PUBLISH_SCHEDULE, request, outcome, guid, contentId, path);
  }

  public static void pageRemovalSchedule(
      HttpServletRequest request,
      AuditOutcome outcome,
      String guid,
      String contentId,
      String path) {
    content(ContentErrorCodes.PAGE_REMOVAL_SCHEDULE, request, outcome, guid, contentId, path);
  }

  /**
   * User-management dual-write. {@code targetUser} is the account being acted on; actor comes from
   * the request remote user when present.
   */
  public static void userManagement(
      UserManagementErrorCodes code,
      HttpServletRequest request,
      AuditOutcome outcome,
      String targetUser,
      String activity) {
    userManagement(code, request, outcome, targetUser, activity, null);
  }

  /**
   * User-management dual-write with an optional explicit actor (e.g. {@code "system"} for automated
   * password re-encryption during authentication when {@link HttpServletRequest#getRemoteUser()} is
   * not yet the operator of record).
   *
   * @param actorOverride when non-blank, used as the audit actor instead of {@code
   *     request.getRemoteUser()}
   */
  public static void userManagement(
      UserManagementErrorCodes code,
      HttpServletRequest request,
      AuditOutcome outcome,
      String targetUser,
      String activity,
      String actorOverride) {
    String actor =
        (actorOverride != null && !actorOverride.isBlank())
            ? actorOverride.trim()
            : remoteUser(request);
    String target = nullToEmpty(targetUser);
    if (target.isEmpty()) {
      target = actor;
    }
    AuditOutcome o = outcome != null ? outcome : code.defaultOutcome();
    AuditContext ctx =
        AuditContext.builder()
            .actor(actor)
            .target(target)
            .sourceIp(clientIp(request))
            .sourceHost(sourceHost(request))
            .attribute("activity", nullToEmpty(activity))
            .build();
    if (code == UserManagementErrorCodes.CREATE || code == UserManagementErrorCodes.DELETE) {
      log(code, ctx, o, actor, target);
    } else {
      log(code, ctx, o, actor, target, nullToEmpty(activity));
    }
  }

  public static void userCreate(
      HttpServletRequest request, AuditOutcome outcome, String targetUser) {
    userManagement(UserManagementErrorCodes.CREATE, request, outcome, targetUser, "");
  }

  public static void userUpdate(
      HttpServletRequest request, AuditOutcome outcome, String targetUser, String activity) {
    userManagement(UserManagementErrorCodes.UPDATE, request, outcome, targetUser, activity);
  }

  /**
   * User update with an explicit actor (use {@code "system"} for automated security maintenance).
   */
  public static void userUpdate(
      HttpServletRequest request,
      AuditOutcome outcome,
      String targetUser,
      String activity,
      String actor) {
    userManagement(UserManagementErrorCodes.UPDATE, request, outcome, targetUser, activity, actor);
  }

  public static void userDelete(
      HttpServletRequest request, AuditOutcome outcome, String targetUser) {
    userManagement(UserManagementErrorCodes.DELETE, request, outcome, targetUser, "");
  }

  /** Workflow transition dual-write. */
  public static void workflowTransition(
      HttpServletRequest request,
      AuditOutcome outcome,
      String contentId,
      String guid,
      String fromState,
      String toState) {
    String cid = nullToEmpty(contentId);
    String g = nullToEmpty(guid);
    String from = nullToEmpty(fromState);
    String to = nullToEmpty(toState);
    AuditOutcome o = outcome != null ? outcome : AuditOutcome.SUCCESS;
    AuditContext ctx =
        AuditContext.builder()
            .actor(remoteUser(request))
            .target(g.isEmpty() ? cid : g)
            .sourceIp(clientIp(request))
            .sourceHost(sourceHost(request))
            .attribute("contentId", cid)
            .attribute("fromState", from)
            .attribute("toState", to)
            .build();
    log(WorkflowErrorCodes.TRANSITION, ctx, o, cid, g, from, to);
  }

  /**
   * Design-object lifecycle dual-write (DESN-2901..2903). Used by {@code PSDesignObjectAuditor} so
   * design save/delete events land in {@code PSX_SYSTEM_AUDIT_LOG} (and Log4j) alongside the legacy
   * design audit table.
   *
   * <p>Message params match {@link DesignErrorCodes} templates: {@code type}, {@code name}, {@code
   * guid}.
   *
   * @param code one of {@link DesignErrorCodes#CREATE}, {@link DesignErrorCodes#UPDATE}, or {@link
   *     DesignErrorCodes#DELETE}
   * @param actor authenticated operator; blank becomes {@code "unknown"}
   * @param type design object type label (e.g. {@code CONTENT_LIST})
   * @param name design object name when known; may be blank
   * @param guid string form of the design object GUID
   */
  public static void design(
      DesignErrorCodes code, String actor, String type, String name, String guid) {
    if (code == null) {
      return;
    }
    String a = (actor == null || actor.isBlank()) ? "unknown" : actor.trim();
    String t = nullToEmpty(type);
    String n = nullToEmpty(name);
    String g = nullToEmpty(guid);
    AuditOutcome o = code.defaultOutcome() != null ? code.defaultOutcome() : AuditOutcome.SUCCESS;
    AuditContext ctx =
        AuditContext.builder()
            .actor(a)
            .target(g.isEmpty() ? t : g)
            .attribute("type", t)
            .attribute("name", n)
            .build();
    log(code, ctx, o, t, n, g);
  }

  /** Design object create (DESN-2901). */
  public static void designCreate(String actor, String type, String name, String guid) {
    design(DesignErrorCodes.CREATE, actor, type, name, guid);
  }

  /** Design object update/save (DESN-2902). */
  public static void designUpdate(String actor, String type, String name, String guid) {
    design(DesignErrorCodes.UPDATE, actor, type, name, guid);
  }

  /** Design object delete (DESN-2903). */
  public static void designDelete(String actor, String type, String name, String guid) {
    design(DesignErrorCodes.DELETE, actor, type, name, guid);
  }

  private static AuditContext contentContext(HttpServletRequest request, String guid) {
    return AuditContext.builder()
        .actor(remoteUser(request))
        .target(nullToEmpty(guid))
        .sourceIp(clientIp(request))
        .sourceHost(sourceHost(request))
        .build();
  }

  private static AuditContext context(HttpServletRequest request, String actor) {
    AuditContext.Builder b = AuditContext.builder().actor(actor);
    if (request != null) {
      b.sourceIp(clientIp(request)).sourceHost(request.getRemoteHost());
    }
    return b.build();
  }

  private static String remoteUser(HttpServletRequest request) {
    if (request == null) {
      return "";
    }
    return nullToEmpty(request.getRemoteUser());
  }

  private static String sourceHost(HttpServletRequest request) {
    if (request == null) {
      return "";
    }
    return nullToEmpty(request.getRemoteHost());
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
