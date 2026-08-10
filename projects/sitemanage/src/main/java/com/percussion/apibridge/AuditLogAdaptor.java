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
package com.percussion.apibridge;

import static com.percussion.webservices.PSWebserviceUtils.getUserName;
import static com.percussion.webservices.PSWebserviceUtils.getUserRoles;

import com.percussion.rest.auditlog.IAuditLogAdaptor;
import com.percussion.rest.auditlog.SystemAuditLogEntry;
import com.percussion.rest.auditlog.SystemAuditLogExport;
import com.percussion.rest.auditlog.SystemAuditLogPage;
import com.percussion.security.IPSPrincipalAttribute;
import com.percussion.services.audit.PSSystemAuditLogPermission;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.security.PSRoleMgrLocator;
import com.percussion.system.utils.PSSiteManageBean;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * sitemanage apibridge for system audit log query (#2618) and export (#2715).
 *
 * <p>AuthZ: {@link PSSystemAuditLogPermission} — Admin always allowed, otherwise role property
 * {@code sys_securityAuditLogViewer}.
 *
 * <p>Phase 5 / #2716: list/detail success and explicit deny emit audit-of-audit events via {@link
 * PSSystemAuditLogger} (dual-write). Nested dual-write is suppressed by the audit service
 * reentrancy guard.
 */
@PSSiteManageBean
@Lazy
public class AuditLogAdaptor implements IAuditLogAdaptor {

  private static final Logger log = LogManager.getLogger(AuditLogAdaptor.class);

  private static final String ACTION_LIST = "list";
  private static final String ACTION_DETAIL = "detail";
  private static final String ACTION_EXPORT = "export";
  private static final String DENY_REASON = "forbidden";

  private final PSSystemAuditLogRepository repository;
  private final Supplier<List<String>> userRolesSupplier;
  private final Function<String, Boolean> roleHasViewerProperty;
  private final Supplier<String> currentUserSupplier;

  @Autowired
  public AuditLogAdaptor(PSSystemAuditLogRepository repository) {
    this(
        repository,
        () -> getUserRoles(),
        AuditLogAdaptor::roleHasViewerPropertyFromRoleMgr,
        () -> getUserName());
  }

  /** Package-visible test constructor. */
  AuditLogAdaptor(
      PSSystemAuditLogRepository repository,
      Supplier<List<String>> userRolesSupplier,
      Function<String, Boolean> roleHasViewerProperty) {
    this(repository, userRolesSupplier, roleHasViewerProperty, () -> "test-user");
  }

  /** Package-visible test constructor with explicit actor. */
  AuditLogAdaptor(
      PSSystemAuditLogRepository repository,
      Supplier<List<String>> userRolesSupplier,
      Function<String, Boolean> roleHasViewerProperty,
      Supplier<String> currentUserSupplier) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.userRolesSupplier = Objects.requireNonNull(userRolesSupplier, "userRolesSupplier");
    this.roleHasViewerProperty =
        Objects.requireNonNull(roleHasViewerProperty, "roleHasViewerProperty");
    this.currentUserSupplier =
        Objects.requireNonNull(currentUserSupplier, "currentUserSupplier");
  }

  @Override
  public SystemAuditLogPage query(
      String fromIso,
      String toIso,
      String moduleCode,
      String eventType,
      String outcome,
      String actor,
      int offset,
      int limit) {
    requireViewer(ACTION_LIST, summarizeFilters(fromIso, toIso, moduleCode, eventType, outcome, actor));
    Instant from = parseInstant(fromIso, "from");
    Instant to = parseInstant(toIso, "to");
    int safeOffset = Math.max(0, offset);
    int safeLimit = PSSystemAuditLogRepository.clampLimit(limit);

    List<PSSystemAuditLogEntry> rows =
        repository.findEntries(
            from, to, moduleCode, eventType, outcome, actor, safeOffset, safeLimit);
    long total = repository.countEntries(from, to, moduleCode, eventType, outcome, actor);

    List<SystemAuditLogEntry> entries = new ArrayList<>(rows.size());
    for (PSSystemAuditLogEntry row : rows) {
      entries.add(toDto(row));
    }
    emitViewerSuccess(
        ACTION_LIST,
        summarizeFilters(fromIso, toIso, moduleCode, eventType, outcome, actor)
            + ",offset="
            + safeOffset
            + ",limit="
            + safeLimit);
    return new SystemAuditLogPage(entries, total, safeOffset, safeLimit);
  }

  @Override
  public SystemAuditLogEntry findById(String auditId) {
    requireViewer(ACTION_DETAIL, StringUtils.defaultString(auditId));
    if (StringUtils.isBlank(auditId)) {
      throw new IllegalArgumentException("auditId is required");
    }
    String id = auditId.trim();
    Optional<PSSystemAuditLogEntry> found = repository.findById(id);
    emitViewerSuccess(ACTION_DETAIL, "auditId=" + id);
    return found.map(AuditLogAdaptor::toDto).orElse(null);
  }

  @Override
  public List<SystemAuditLogEntry> export(
      String fromIso,
      String toIso,
      String moduleCode,
      String eventType,
      String outcome,
      String actor,
      int maxRows) {
    requireViewer(
        ACTION_EXPORT,
        summarizeFilters(fromIso, toIso, moduleCode, eventType, outcome, actor));
    Instant from = parseInstant(fromIso, "from");
    Instant to = parseInstant(toIso, "to");
    int cap = SystemAuditLogExport.clampMaxRows(maxRows);

    // Page through repository (query hard-cap 200) until export cap or end of result set.
    List<SystemAuditLogEntry> all = new ArrayList<>(Math.min(cap, 256));
    int offset = 0;
    while (all.size() < cap) {
      int pageSize =
          Math.min(PSSystemAuditLogRepository.MAX_PAGE_SIZE, cap - all.size());
      List<PSSystemAuditLogEntry> rows =
          repository.findEntries(
              from, to, moduleCode, eventType, outcome, actor, offset, pageSize);
      if (rows == null || rows.isEmpty()) {
        break;
      }
      for (PSSystemAuditLogEntry row : rows) {
        all.add(toDto(row));
      }
      offset += rows.size();
      if (rows.size() < pageSize) {
        break;
      }
    }
    emitViewerSuccess(
        ACTION_EXPORT,
        summarizeFilters(fromIso, toIso, moduleCode, eventType, outcome, actor)
            + ",maxRows="
            + cap
            + ",count="
            + all.size());
    return all;
  }

  private void requireViewer(String action, String detail) {
    List<String> roles;
    try {
      roles = userRolesSupplier.get();
    } catch (Exception e) {
      log.debug("Could not resolve user roles for audit log access", e);
      emitViewerDenied(action, "role-resolution-failed");
      throw new SecurityException("Unable to resolve user roles for audit log access");
    }
    boolean allowed =
        PSSystemAuditLogPermission.allows(
            roles, role -> Boolean.TRUE.equals(roleHasViewerProperty.apply(role)));
    if (!allowed) {
      emitViewerDenied(action, DENY_REASON);
      throw new SecurityException(
          "Not authorized to view the system security audit log (Admin role or "
              + PSSystemAuditLogPermission.ROLE_PROPERTY
              + " required)");
    }
  }

  private void emitViewerSuccess(String action, String detail) {
    try {
      PSSystemAuditLogger.auditViewerAccess(resolveActor(), action, detail);
    } catch (RuntimeException e) {
      log.debug("Audit-of-audit viewer success emit failed", e);
    }
  }

  private void emitViewerDenied(String action, String reason) {
    try {
      PSSystemAuditLogger.auditViewerAccessDenied(resolveActor(), action, reason);
    } catch (RuntimeException e) {
      log.debug("Audit-of-audit viewer deny emit failed", e);
    }
  }

  private String resolveActor() {
    try {
      String user = currentUserSupplier.get();
      if (user != null && !user.isBlank()) {
        return user.trim();
      }
    } catch (Exception e) {
      log.debug("Could not resolve current user for audit-of-audit", e);
    }
    return "unknown";
  }

  static String summarizeFilters(
      String fromIso,
      String toIso,
      String moduleCode,
      String eventType,
      String outcome,
      String actor) {
    StringBuilder sb = new StringBuilder();
    appendFilter(sb, "from", fromIso);
    appendFilter(sb, "to", toIso);
    appendFilter(sb, "module", moduleCode);
    appendFilter(sb, "eventType", eventType);
    appendFilter(sb, "outcome", outcome);
    appendFilter(sb, "actor", actor);
    return sb.length() == 0 ? "none" : sb.toString();
  }

  private static void appendFilter(StringBuilder sb, String name, String value) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    if (sb.length() > 0) {
      sb.append(',');
    }
    sb.append(name).append('=').append(escapeFilterValue(value.trim()));
  }

  /**
   * Quote filter values that contain commas, equals, or quotes so the comma-delimited
   * {@link #summarizeFilters} summary stays unambiguous for operators and parsers.
   */
  static String escapeFilterValue(String value) {
    if (value.indexOf(',') < 0 && value.indexOf('=') < 0 && value.indexOf('"') < 0) {
      return value;
    }
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }


  static Instant parseInstant(String raw, String fieldName) {
    if (StringUtils.isBlank(raw)) {
      return null;
    }
    try {
      return Instant.parse(raw.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          fieldName + " is not a valid ISO-8601 instant: " + raw, e);
    }
  }

  static SystemAuditLogEntry toDto(PSSystemAuditLogEntry e) {
    SystemAuditLogEntry dto = new SystemAuditLogEntry();
    dto.setAuditId(e.getAuditId());
    dto.setEventTime(e.getEventTimeInstant());
    dto.setModuleCode(e.getModuleCode());
    dto.setMessageCode(e.getMessageCode());
    dto.setEventType(e.getEventType());
    dto.setOutcome(e.getOutcome());
    dto.setActor(e.getActor());
    dto.setTarget(e.getTarget());
    dto.setSourceIp(e.getSourceIp());
    dto.setSourceHost(e.getSourceHost());
    dto.setUserMessage(e.getUserMessage());
    dto.setLogMessage(e.getLogMessage());
    dto.setCorrelationId(e.getCorrelationId());
    dto.setAttributesJson(e.getAttributesJson());
    dto.setServerNode(e.getServerNode());
    return dto;
  }

  static boolean roleHasViewerPropertyFromRoleMgr(String roleName) {
    try {
      IPSRoleMgr roleMgr = PSRoleMgrLocator.getRoleManager();
      Set<IPSPrincipalAttribute> attrs = roleMgr.getRoleAttributes(roleName);
      if (attrs == null || attrs.isEmpty()) {
        return false;
      }
      for (IPSPrincipalAttribute attr : attrs) {
        if (attr == null || attr.getName() == null) {
          continue;
        }
        if (PSSystemAuditLogPermission.ROLE_PROPERTY.equalsIgnoreCase(attr.getName())) {
          return PSSystemAuditLogPermission.isTruthyPropertyValue(attr.getValues());
        }
      }
      return false;
    } catch (Exception e) {
      LogManager.getLogger(AuditLogAdaptor.class)
          .debug("Failed to load role attributes for {}", roleName, e);
      return false;
    }
  }
}
