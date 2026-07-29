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

package com.ibm.cadf.middleware;

import com.ibm.cadf.CADFTaxonomy;
import com.ibm.cadf.CADFTaxonomy.OUTCOME;
import com.ibm.cadf.EventFactory;
import com.ibm.cadf.auditlogger.AuditLogger;
import com.ibm.cadf.auditlogger.AuditLoggerFactory;
import com.ibm.cadf.cfg.Config;
import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.model.CADFType;
import com.ibm.cadf.model.Credential;
import com.ibm.cadf.model.EndPoint;
import com.ibm.cadf.model.Event;
import com.ibm.cadf.model.Host;
import com.ibm.cadf.model.Identifier;
import com.ibm.cadf.model.Resource;
import com.ibm.cadf.util.Constants;
import com.ibm.cadf.util.StringUtil;
import java.util.Properties;

/**
 * Thin facade that wires the singleton {@link Config}, an {@link AuditLogger} resolved through
 * {@link AuditLoggerFactory}, and {@link EventFactory} into a single entry point. Callers supply a
 * coarse-grained {@link AuditContext}, and this class materializes the fully populated CADF {@link
 * Event} (initiator, target, observer, outcome) before forwarding it to the configured logger.
 */
public class AuditMiddleware {

  private Config config;

  private AuditLogger auditLogger;

  /**
   * Constructs a middleware bound to the singleton {@link Config} and the audit logger for the
   * requested output format.
   *
   * @param type the audit format identifier (e.g., {@link Constants#AUDIT_FORMAT_TYPE_JSON} or
   *     {@link Constants#AUDIT_FORMAT_TYPE_CSV}); an unrecognized value falls back to CSV.
   */
  public AuditMiddleware(String type) {
    config = Config.getInstance();
    auditLogger = AuditLoggerFactory.getAuditLogger(type);
  }

  /**
   * Replaces the configuration properties used to resolve type URIs and action labels.
   *
   * @param properties the new properties to merge into the existing configuration, never {@code
   *     null}.
   */
  public void setProperties(Properties properties) {
    config.setProperties(properties);
  }

  /**
   * Sets the destination file path on the underlying audit logger.
   *
   * @param filePath absolute path of the file the logger should write to, never {@code null}.
   */
  public void setOutputFilePath(String filePath) {
    auditLogger.setOutputFilePath(filePath);
  }

  /**
   * Forwards an already-built CADF event to the underlying audit logger.
   *
   * @param event the event to record, never {@code null}.
   * @throws CADFException when the logger cannot persist the event.
   */
  public void audit(Event event) throws CADFException {

    auditLogger.audit(event);
  }

  /**
   * Builds a fully-populated CADF event from a coarse-grained {@link AuditContext}, the action
   * label, and an outcome string. Falls back to {@link CADFTaxonomy#UNKNOWN} when the action cannot
   * be resolved and to {@code UNKNOWN} when the outcome string is not a known {@link
   * CADFTaxonomy.OUTCOME} name.
   *
   * @param action the action label whose property value identifies the action in CADF taxonomy,
   *     never {@code null}.
   * @param status the desired outcome name (e.g., {@code "SUCCESS"}); may be unrecognized.
   * @param ctx the audit context carrying initiator, target, and observer metadata, never {@code
   *     null}.
   * @return a new {@link Event} populated with initiator / target / observer resources, never
   *     {@code null}.
   * @throws CADFException when the event cannot be assembled.
   */
  public Event createEvent(String action, String status, com.ibm.cadf.middleware.AuditContext ctx)
      throws CADFException {
    String actionVal = config.getProperty(action);
    if (StringUtil.isEmpty(actionVal)) {
      actionVal = CADFTaxonomy.UNKNOWN;
    }

    // Constructing the initiator resource - it should be logged user into the storage platform
    String initiatorId = ctx.getTargetUsername();
    String initiatorTypeURI = config.getProperty(Constants.INITIATOR_TYPE_URI);
    Resource initiator = new Resource(initiatorId);
    initiator.setTypeURI(initiatorTypeURI);
    // Get the storage platform logged in username
    initiator.setName(ctx.getIniatorName());
    Host host = new Host();
    host.setAddress(ctx.getInitiatorIP());
    host.setAgent(ctx.getAgentName());
    initiator.setHost(host);

    // Constructing the target resource
    String targetTypeURI = config.getProperty(Constants.TARGET_TYPE_URI);
    String targetId = Identifier.generateUniqueId();
    Resource target = new Resource(targetId);
    target.setTypeURI(targetTypeURI);
    target.setName(ctx.getTargetName());

    if (!StringUtil.isEmpty(ctx.getPath())) {
      target.setId(ctx.getPath());
    }
    if (!StringUtil.isEmpty(ctx.getTargetUsername())) {
      // Set credentials
      Credential credential = new Credential(ctx.getTargetUsername());
      target.setCredential(credential);
    }

    // Set addresses
    if (!StringUtil.isEmpty(ctx.getTargetUrl())) {
      EndPoint endpoint = new EndPoint(ctx.getTargetUrl());
      endpoint.setName(ctx.getTargetEndpointName());
      target.addAddress(endpoint);
    }

    // Constructing the observer resource.
    String objserverTypeURI = config.getProperty(Constants.OBSERVER_TYPE_URI);
    String observerId = Identifier.generateUniqueId();
    String observername = ctx.getObserverName();
    Resource observer = new Resource(observerId);
    observer.setTypeURI(objserverTypeURI);
    observer.setName(observername);

    // Create an event
    // The default outcome is success
    String outcome = CADFTaxonomy.OUTCOME.SUCCESS.name();
    try {
      OUTCOME outcomeEnum = CADFTaxonomy.OUTCOME.valueOf(status);
      outcome = outcomeEnum.value;

    } catch (IllegalArgumentException e) {
      // If there is no valid status set unknown
      outcome = CADFTaxonomy.OUTCOME.UNKNOWN.value;
    }
    String activityType = CADFType.EVENTTYPE.EVENTTYPE_ACTIVITY.name();
    if (ctx.getActivity() != null
        && ctx.getActivity().equalsIgnoreCase(CADFType.EVENTTYPE.EVENTTYPE_REVOKE.value)) {
      activityType = CADFType.EVENTTYPE.EVENTTYPE_REVOKE.name();
    }

    return EventFactory.getEventInstance(
        activityType,
        Identifier.generateUniqueId(),
        actionVal,
        outcome,
        initiator,
        null,
        target,
        null,
        observer,
        null);
  }
}
