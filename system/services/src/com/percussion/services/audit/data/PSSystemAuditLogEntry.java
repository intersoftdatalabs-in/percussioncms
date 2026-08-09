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
package com.percussion.services.audit.data;

import com.intsof.percussioncms.auditlog.AuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** JPA entity for {@code PSX_SYSTEM_AUDIT_LOG} (system-wide dual-write durable store). */
@Entity
@Table(name = "PSX_SYSTEM_AUDIT_LOG")
public class PSSystemAuditLogEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final int MSG_MAX = 4000;

  @Id
  @Column(name = "AUDIT_ID", nullable = false, length = 36)
  private String auditId;

  @Column(name = "EVENT_TIME", nullable = false)
  private Date eventTime;

  @Column(name = "MODULE_CODE", nullable = false, length = 16)
  private String moduleCode;

  @Column(name = "MESSAGE_CODE", nullable = false)
  private int messageCode;

  @Column(name = "EVENT_TYPE", length = 64)
  private String eventType;

  @Column(name = "OUTCOME", nullable = false, length = 32)
  private String outcome;

  @Column(name = "ACTOR", length = 255)
  private String actor;

  @Column(name = "TARGET", length = 512)
  private String target;

  @Column(name = "SOURCE_IP", length = 64)
  private String sourceIp;

  @Column(name = "SOURCE_HOST", length = 255)
  private String sourceHost;

  @Column(name = "SESSION_ID_HASH", length = 128)
  private String sessionIdHash;

  @Column(name = "USER_MESSAGE", nullable = false, length = MSG_MAX)
  private String userMessage;

  @Column(name = "LOG_MESSAGE", nullable = false, length = MSG_MAX)
  private String logMessage;

  @Column(name = "CORRELATION_ID", length = 128)
  private String correlationId;

  @Lob
  @Column(name = "ATTRIBUTES_JSON")
  private String attributesJson;

  @Column(name = "SERVER_NODE", length = 255)
  private String serverNode;

  public static PSSystemAuditLogEntry from(AuditRecord record) {
    Objects.requireNonNull(record, "record");
    PSSystemAuditLogEntry e = new PSSystemAuditLogEntry();
    e.auditId = record.logId().value();
    e.eventTime = Date.from(record.eventTime());
    e.moduleCode = record.code().module().code();
    e.messageCode = record.code().numericCode();
    e.eventType =
        record.code().eventType() == null ? null : record.code().eventType().name();
    e.outcome = record.outcome().name();
    e.actor = truncate(record.actor().orElse(null), 255);
    e.target = truncate(record.target().orElse(null), 512);
    e.sourceIp = truncate(record.sourceIp().orElse(null), 64);
    e.sourceHost = truncate(record.sourceHost().orElse(null), 255);
    e.sessionIdHash = truncate(record.sessionIdHash().orElse(null), 128);
    e.userMessage = truncate(record.userMessage(), MSG_MAX);
    e.logMessage = truncate(record.logMessage(), MSG_MAX);
    e.correlationId = truncate(record.correlationId().orElse(null), 128);
    e.attributesJson = attributesToJson(record.attributes());
    e.serverNode = truncate(record.serverNode().orElse(null), 255);
    return e;
  }

  static String attributesToJson(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    }
    // Minimal JSON object without pulling Jackson into this mapping path
    return attributes.entrySet().stream()
        .map(
            en ->
                "\""
                    + jsonEscape(en.getKey())
                    + "\":\""
                    + jsonEscape(en.getValue() == null ? "" : en.getValue())
                    + "\"")
        .collect(Collectors.joining(",", "{", "}"));
  }

  static String jsonEscape(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(s.length() + 8);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> out.append("\\\\");
        case '"' -> out.append("\\\"");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }

  static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String auditId) {
    this.auditId = auditId;
  }

  public Date getEventTime() {
    return eventTime;
  }

  public Instant getEventTimeInstant() {
    return eventTime == null ? null : eventTime.toInstant();
  }

  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  public String getModuleCode() {
    return moduleCode;
  }

  public void setModuleCode(String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public int getMessageCode() {
    return messageCode;
  }

  public void setMessageCode(int messageCode) {
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

  public String getSessionIdHash() {
    return sessionIdHash;
  }

  public void setSessionIdHash(String sessionIdHash) {
    this.sessionIdHash = sessionIdHash;
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
