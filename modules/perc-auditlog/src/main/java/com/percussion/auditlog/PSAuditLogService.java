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
import com.ibm.cadf.middleware.AuditMiddleware;
import com.ibm.cadf.model.Event;
import com.ibm.cadf.util.Constants;
import com.percussion.auditlog.util.AuditPropertyLoader;
import com.percussion.auditlog.util.FileCreator;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Process-wide singleton that bridges the {@link IPSAuditEvent} domain events with the IBM CADF
 * {@link AuditMiddleware} used by the rest of the platform. Reads {@code
 * rxconfig/Server/audit-log.properties} on first use to determine whether to emit a JSON audit log
 * to disk via {@link FileCreator} in addition to forwarding events to the middleware.
 */
public class PSAuditLogService implements IPSAuditLogService {

  private static final Logger log = LogManager.getLogger(PSAuditLogService.class);

  private static AuditMiddleware middleware;
  private static Properties properties;
  private static final String CONFIG_FILE_BASE = "rxconfig/Server/audit-log.properties";
  private static Boolean isGenerateLog = false;

  /**
   * Creates an Audit Log Entry for a ContentEvent
   *
   * @param event A fully populated ContentEvent
   */
  public void logContentEvent(PSContentEvent event) {
    Event ae = createEvent((AuditContext) event, event.getAction().name(), event.getOutcome());

    if (event.getContentId() != null)
      ae.addTag(PSContentEvent.CONTENTID_TAG, String.valueOf(event.getContentId()));

    if (StringUtils.isNotEmpty(event.getGuid()))
      ae.addTag(PSContentEvent.GUID_TAG, event.getGuid());

    ae.setId("percussion:" + event.getGuid());

    auditLog(ae);
  }

  /**
   * Logs a Workflow Event.
   *
   * @param event the fully-populated workflow event to log, never {@code null}.
   */
  public void logWorkflowEvent(PSWorkflowEvent event) {
    Event ae = createEvent((AuditContext) event, event.getAction().name(), event.getOutcome());

    if (event.getContentId() != 0)
      ae.addTag(PSWorkflowEvent.CONTENTID_TAG, String.valueOf(event.getContentId()));

    if (StringUtils.isNotEmpty(event.getGuid()))
      ae.addTag(PSWorkflowEvent.GUID_TAG, event.getGuid());

    if (StringUtils.isNotEmpty(event.getTransitionFrom()))
      ae.addTag(PSWorkflowEvent.TRANSITIONFROM_TAG, event.getTransitionFrom());

    if (StringUtils.isNotEmpty(event.getTransitionTo()))
      ae.addTag(PSWorkflowEvent.TRANSITIONTO_TAG, event.getTransitionTo());

    ae.setId("percussion:" + event.getGuid());
    auditLog(ae);
  }

  /**
   * Logs an Authentication Event.
   *
   * @param event the fully-populated authentication event to log, never {@code null}.
   */
  public void logAuthenticationEvent(PSAuthenticationEvent event) {

    Event ae = createEvent((AuditContext) event, event.getAction().name(), event.getOutcome());
    auditLog(ae);
  }

  /**
   * Logs an event for User Management.
   *
   * @param event the fully-populated user-management event to log, never {@code null}.
   */
  public void logUserManagementEvent(PSUserManagementEvent event) {

    Event ae = createEvent((AuditContext) event, event.getAction().name(), event.getOutcome());
    auditLog(ae);
  }

  /**
   * Forwards the given CADF event to the configured middleware sink and, when audit-log file
   * generation is enabled, ensures the dated log file exists for the configured output directory.
   *
   * @param ae the CADF event to record, never {@code null}.
   */
  public void auditLog(Event ae) {
    try {
      if (isGenerateLog() && properties != null && properties.size() > 0) {
        generateLogFile(properties);
        middleware.audit(ae);
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Delegates to the underlying {@link AuditMiddleware} to build a CADF {@link Event} from the
   * supplied context, action, and outcome.
   *
   * @param event the source audit context, never {@code null}.
   * @param action the action name, never {@code null}.
   * @param outcome the outcome name, never {@code null}.
   * @return the constructed CADF event, never {@code null}.
   */
  public Event createEvent(AuditContext event, String action, String outcome) {
    return middleware.createEvent(action, outcome, event);
  }

  private PSAuditLogService() {
    try {
      middleware = new AuditMiddleware(Constants.AUDIT_FORMAT_TYPE_JSON);

      properties =
          AuditPropertyLoader.loadProperties(
              PathUtils.getRxDir(null) + File.separator + CONFIG_FILE_BASE);
      middleware.setProperties(properties);
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  private static PSAuditLogService instance;

  /**
   * Returns the process-wide singleton instance, constructing it on first access.
   *
   * @return the shared {@link PSAuditLogService} instance, never {@code null}.
   */
  public static synchronized PSAuditLogService getInstance() {
    if (instance == null) {
      // if instance is null, initialize
      instance = new PSAuditLogService();
      // AuditPropertyLoader.loadConfig();
    }

    return instance;
  }

  /**
   * Creates (or reuses) the dated audit-log file under the directory configured by the supplied
   * properties and forwards the resolved file path to the underlying middleware.
   *
   * @param properties the audit-log configuration bundle, never {@code null}.
   */
  public static void generateLogFile(Properties properties) {

    String fileName =
        FileCreator.generateFile(
            properties.getProperty("filePath"),
            properties.getProperty("fileName"),
            properties.getProperty("filePattern"),
            properties.getProperty("extension"));

    middleware.setOutputFilePath(fileName);
  }

  /**
   * Indicates whether the configured properties request a file-based audit log to be generated.
   *
   * @return {@code true} when {@code generateLog} is set to {@code true} in the loaded properties,
   *     {@code false} otherwise.
   */
  public static Boolean isGenerateLog() {
    if ("true".equalsIgnoreCase(properties.getProperty("generateLog"))) {
      isGenerateLog = true;
    }
    return isGenerateLog;
  }
}
