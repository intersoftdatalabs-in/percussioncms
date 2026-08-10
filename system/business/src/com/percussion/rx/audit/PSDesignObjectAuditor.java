// REFACTORED: CP-JAVA11
package com.percussion.rx.audit;

import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.percussion.server.PSRequest;
import com.percussion.services.audit.PSDesignObjectAuditServiceLocator;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.audit.data.PSAuditLogEntry;
import com.percussion.services.audit.data.PSAuditLogEntry.AuditTypes;
import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;

/**
 * Configured with Spring AOP to write design-object audit events whenever a design object is
 * inserted, updated, or removed from the repository.
 *
 * <p>Writes the legacy {@link PSAuditLogEntry} table ({@code PSX_DESIGN_AUDIT_LOG}) and dual-writes
 * the same save/delete events through {@link PSSystemAuditLogger} / {@link DesignErrorCodes} into
 * the system audit path ({@code PSX_SYSTEM_AUDIT_LOG} + Log4j). When design auditing is disabled,
 * neither path is written.
 */
public class PSDesignObjectAuditor {

  private static final Logger log = LogManager.getLogger(PSDesignObjectAuditor.class);

  /**
   * Perform the audit of the method call specified in the supplied joinpoint. If auditing is not
   * enabled, simply returns, otherwise performs the audit as follows:
   *
   * <p>Only audits method signatures that start with "save" or "delete".
   *
   * <p>Only considers the first parameter of the method signature. This argument must be an
   * instance of {@link IPSCatalogIdentifier} or a collection of such instances.
   *
   * <p>For each object audited, a {@link PSAuditLogEntry} is inserted in the legacy repository and
   * a DESN system-audit dual-write is emitted.
   *
   * @param joinPoint The joinpoint, never <code>null</code>.
   * @throws Throwable If there are any errors.
   */
  public void audit(JoinPoint joinPoint) throws Throwable {
    if (joinPoint == null) {
      throw new IllegalArgumentException("joinPoint may not be null");
    }
    var args = joinPoint.getArgs();
    if (args == null || args.length == 0) {
      return;
    }
    if (!isAuditingEnabled()) {
      return;
    }
    var name = joinPoint.getSignature().getName();
    var auditData = createAuditData(name, args[0]);
    if (auditData.isEmpty()) {
      return;
    }
    var userName = resolveUserName();
    var auditDate = new Date();
    var svc = PSDesignObjectAuditServiceLocator.getAuditService();
    var entries = new ArrayList<PSAuditLogEntry>();
    for (var data : auditData) {
      var entry = svc.createAuditLogEntry();
      entry.setAction(data.mi_auditAction);
      // setDate(Date) is deprecated, but required for backward compatibility (see migration notes)
      entry.setDate(auditDate);
      entry.setObjectGUID(data.mi_objectGuid);
      entry.setUserName(userName);
      entries.add(entry);
      dualWriteSystemAudit(userName, data);
    }
    svc.saveAuditLogEntries(entries);
  }

  /**
   * Resolve the actor for the audit event. Blank / missing request context maps to {@code
   * "unknown"}.
   *
   * <p>Package-visible for unit tests.
   */
  String resolveUserName() {
    var userName = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
    if (StringUtils.isBlank(userName)) {
      var req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
      if (req != null && req.getUserSession() != null) {
        userName = req.getUserSession().getRealAuthenticatedUserEntry();
      }
    }
    if (StringUtils.isBlank(userName)) {
      return "unknown";
    }
    return userName;
  }

  /**
   * Dual-write one design audit event through {@link PSSystemAuditLogger}. Failures are logged and
   * swallowed so design operations are never blocked by the system audit sink.
   *
   * <p>Package-visible for unit tests.
   */
  void dualWriteSystemAudit(String userName, PSAuditData data) {
    if (data == null || data.mi_auditAction == null) {
      return;
    }
    try {
      DesignErrorCodes code =
          data.mi_auditAction == AuditTypes.DELETE
              ? DesignErrorCodes.DELETE
              : DesignErrorCodes.UPDATE;
      IPSGuid guid = data.mi_objectGuid;
      String guidStr = guid == null ? "" : String.valueOf(guid);
      String typeName = resolveTypeName(guid);
      // No separate design-object display name is available on the AOP path; reuse GUID.
      String name = guidStr;
      PSSystemAuditLogger.design(code, userName, typeName, name, guidStr);
    } catch (RuntimeException ex) {
      log.error(
          "Failed to dual-write design system audit for action={}: {}",
          data.mi_auditAction,
          ex.toString());
      log.debug("Design system audit dual-write failure details", ex);
    }
  }

  /**
   * Map a design object GUID type ordinal to a stable label for audit messages.
   *
   * <p>Package-visible for unit tests.
   */
  static String resolveTypeName(IPSGuid guid) {
    if (guid == null) {
      return "";
    }
    try {
      PSTypeEnum typeEnum = PSTypeEnum.valueOf(guid.getType());
      if (typeEnum != null) {
        return typeEnum.name();
      }
    } catch (RuntimeException ignored) {
      // fall through
    }
    return String.valueOf(guid.getType());
  }

  /**
   * Worker method of {@link #audit(JoinPoint)}, see that method for a description of the auditing
   * logic. This method determines the resulting action(s) and guid(s) from the supplied argument and
   * method name. This method is not intended to be called directly, and is public to allow for unit
   * testing.
   *
   * @param methodName The name of the method being audited, used to determine the audited action,
   *     may be <code>null</code> in which case an empty collection is returned.
   * @param arg The argument from which to extract one or more guids for the audited action, may be
   *     <code>null</code> in which case an empty collection is returned.
   * @return The resulting list of audit data, never <code>null</code>, may be empty.
   */
  public Collection<PSAuditData> createAuditData(String methodName, Object arg) {
    var dataList = new ArrayList<PSAuditData>();
    if (methodName == null) {
      return dataList;
    }
    AuditTypes type;
    if (methodName.startsWith("delete")) {
      type = AuditTypes.DELETE;
    } else if (methodName.startsWith("save")) {
      type = AuditTypes.SAVE;
    } else {
      return dataList;
    }
    Collection<?> argCollection = null;
    if (arg instanceof IPSCatalogIdentifier || arg instanceof IPSGuid) {
      argCollection = new ArrayList<>();
      ((ArrayList<Object>) argCollection).add(arg);
    }
    if (arg instanceof Collection) {
      argCollection = (Collection<?>) arg;
    }
    if (argCollection == null) {
      return dataList;
    }
    for (var object : argCollection) {
      IPSGuid guid;
      if (object instanceof IPSGuid) {
        guid = (IPSGuid) object;
      } else if (object instanceof IPSCatalogIdentifier) {
        var id = (IPSCatalogIdentifier) object;
        guid = id.getGUID();
      } else {
        continue;
      }
      var data = new PSAuditData();
      data.mi_objectGuid = guid;
      data.mi_auditAction = type;
      dataList.add(data);
    }
    return dataList;
  }

  /**
   * Check the audit service to determine if auditing is enabled, caching the result for use by
   * future invocations of this method.
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   */
  private boolean isAuditingEnabled() {
    if (m_auditEnabled == null) {
      var svc = PSDesignObjectAuditServiceLocator.getAuditService();
      var config = svc.getConfig();
      m_auditEnabled = config.isEnabled();
    }
    return m_auditEnabled;
  }

  /**
   * Saves the enabled setting from the audit config, <code>null</code> until the config is checked
   * for the first time, immutable after that. Package-visible for unit tests so the cache can be
   * reset or forced.
   */
  Boolean m_auditEnabled = null;

  /**
   * Simple data structure to hold the guid and audit action, package access for unit testing.
   */
  class PSAuditData {
    /**
     * The guid of the object modified by the method being audited, may be <code>null</code>.
     */
    private IPSGuid mi_objectGuid;

    /**
     * The action being performed by the method being audited, may be <code>null</code>.
     */
    private AuditTypes mi_auditAction;

    /**
     * Get the guid
     *
     * @return The guid, may be <code>null</code>.
     */
    IPSGuid getGuid() {
      return mi_objectGuid;
    }

    /**
     * Get the action.
     *
     * @return The action, may be <code>null</code>.
     */
    AuditTypes getAction() {
      return mi_auditAction;
    }
  }
}
