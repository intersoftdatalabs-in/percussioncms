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
package com.percussion.rest.auditlog;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.time.Instant;

/** Wire DTO for one row from {@code PSX_SYSTEM_AUDIT_LOG}. */
@XmlRootElement(name = "SystemAuditLogEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System security audit log entry")
public class SystemAuditLogEntry {

  private String auditId;
  private Instant eventTime;
  private String moduleCode;
  private Integer messageCode;
  private String eventType;
  private String outcome;
  private String actor;
  private String target;
  private String sourceIp;
  private String sourceHost;
  private String userMessage;
  private String logMessage;
  private String correlationId;
  private String attributesJson;
  private String serverNode;

  public SystemAuditLogEntry() {}

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String auditId) {
    this.auditId = auditId;
  }

  public Instant getEventTime() {
    return eventTime;
  }

  public void setEventTime(Instant eventTime) {
    this.eventTime = eventTime;
  }

  public String getModuleCode() {
    return moduleCode;
  }

  public void setModuleCode(String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public Integer getMessageCode() {
    return messageCode;
  }

  public void setMessageCode(Integer messageCode) {
    this.messageCode = messageCode;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
  }

  public String getSourceHost() {
    return sourceHost;
  }

  public void setSourceHost(String sourceHost) {
    this.sourceHost = sourceHost;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getAttributesJson() {
    return attributesJson;
  }

  public void setAttributesJson(String attributesJson) {
    this.attributesJson = attributesJson;
  }

  public String getServerNode() {
    return serverNode;
  }

  public void setServerNode(String serverNode) {
    this.serverNode = serverNode;
  }
}
