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

import static com.percussion.webservices.PSWebserviceUtils.getUserRoles;

import com.percussion.rest.auditlog.IAuditLogAdaptor;
import com.percussion.rest.auditlog.SystemAuditLogEntry;
import com.percussion.rest.auditlog.SystemAuditLogPage;
import com.percussion.security.IPSPrincipalAttribute;
import com.percussion.services.audit.PSSystemAuditLogPermission;
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
 * sitemanage apibridge for system audit log query API (#2618).
 *
 * <p>AuthZ: {@link PSSystemAuditLogPermission} — Admin always allowed, otherwise role property
 * {@code sys_securityAuditLogViewer}.
 */
@PSSiteManageBean
@Lazy
public class AuditLogAdaptor implements IAuditLogAdaptor {

  private static final Logger log = LogManager.getLogger(AuditLogAdaptor.class);

  private final PSSystemAuditLogRepository repository;
  private final Supplier<List<String>> userRolesSupplier;
  private final Function<String, Boolean> roleHasViewerProperty;

  @Autowired
  public AuditLogAdaptor(PSSystemAuditLogRepository repository) {
    this(
        repository,
        () -> getUserRoles(),
        AuditLogAdaptor::roleHasViewerPropertyFromRoleMgr);
  }

  /** Package-visible test constructor. */
  AuditLogAdaptor(
      PSSystemAuditLogRepository repository,
      Supplier<List<String>> userRolesSupplier,
      Function<String, Boolean> roleHasViewerProperty) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.userRolesSupplier = Objects.requireNonNull(userRolesSupplier, "userRolesSupplier");
    this.roleHasViewerProperty =
        Objects.requireNonNull(roleHasViewerProperty, "roleHasViewerProperty");
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
    requireViewer();
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
    return new SystemAuditLogPage(entries, total, safeOffset, safeLimit);
  }

  @Override
  public SystemAuditLogEntry findById(String auditId) {
    requireViewer();
    if (StringUtils.isBlank(auditId)) {
      throw new IllegalArgumentException("auditId is required");
    }
    Optional<PSSystemAuditLogEntry> found = repository.findById(auditId.trim());
    return found.map(AuditLogAdaptor::toDto).orElse(null);
  }

  private void requireViewer() {
    List<String> roles;
    try {
      roles = userRolesSupplier.get();
    } catch (Exception e) {
      log.debug("Could not resolve user roles for audit log access", e);
      throw new SecurityException("Unable to resolve user roles for audit log access");
    }
    boolean allowed =
        PSSystemAuditLogPermission.allows(
            roles, role -> Boolean.TRUE.equals(roleHasViewerProperty.apply(role)));
    if (!allowed) {
      throw new SecurityException(
          "Not authorized to view the system security audit log (Admin role or "
              + PSSystemAuditLogPermission.ROLE_PROPERTY
              + " required)");
    }
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
