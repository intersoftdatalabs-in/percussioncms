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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable audit record ready for dual sinks (already redacted messages). */
public final class AuditRecord {

  private final AuditLogId logId;
  private final Instant eventTime;
  private final SystemErrorCode code;
  private final AuditOutcome outcome;
  private final String actor;
  private final String target;
  private final String sourceIp;
  private final String sourceHost;
  private final String sessionIdHash;
  private final String correlationId;
  private final String userMessage;
  private final String logMessage;
  private final String formattedLine;
  private final Map<String, String> attributes;
  private final String serverNode;

  private AuditRecord(Builder builder) {
    this.logId = Objects.requireNonNull(builder.logId, "logId");
    this.eventTime = Objects.requireNonNull(builder.eventTime, "eventTime");
    this.code = Objects.requireNonNull(builder.code, "code");
    this.outcome = Objects.requireNonNull(builder.outcome, "outcome");
    this.actor = builder.actor;
    this.target = builder.target;
    this.sourceIp = builder.sourceIp;
    this.sourceHost = builder.sourceHost;
    this.sessionIdHash = builder.sessionIdHash;
    this.correlationId = builder.correlationId;
    this.userMessage = Objects.requireNonNull(builder.userMessage, "userMessage");
    this.logMessage = Objects.requireNonNull(builder.logMessage, "logMessage");
    this.formattedLine = Objects.requireNonNull(builder.formattedLine, "formattedLine");
    this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    this.serverNode = builder.serverNode;
  }

  public static Builder builder() {
    return new Builder();
  }

  public AuditLogId logId() {
    return logId;
  }

  public Instant eventTime() {
    return eventTime;
  }

  public SystemErrorCode code() {
    return code;
  }

  public AuditOutcome outcome() {
    return outcome;
  }

  public Optional<String> actor() {
    return Optional.ofNullable(actor);
  }

  public Optional<String> target() {
    return Optional.ofNullable(target);
  }

  public Optional<String> sourceIp() {
    return Optional.ofNullable(sourceIp);
  }

  public Optional<String> sourceHost() {
    return Optional.ofNullable(sourceHost);
  }

  public Optional<String> sessionIdHash() {
    return Optional.ofNullable(sessionIdHash);
  }

  public Optional<String> correlationId() {
    return Optional.ofNullable(correlationId);
  }

  public String userMessage() {
    return userMessage;
  }

  public String logMessage() {
    return logMessage;
  }

  /** Canonical line: {@code [PUB-1001]-[uuid] message…} */
  public String formattedLine() {
    return formattedLine;
  }

  public Map<String, String> attributes() {
    return attributes;
  }

  public Optional<String> serverNode() {
    return Optional.ofNullable(serverNode);
  }

  public static final class Builder {
    private AuditLogId logId;
    private Instant eventTime;
    private SystemErrorCode code;
    private AuditOutcome outcome;
    private String actor;
    private String target;
    private String sourceIp;
    private String sourceHost;
    private String sessionIdHash;
    private String correlationId;
    private String userMessage;
    private String logMessage;
    private String formattedLine;
    private Map<String, String> attributes = Map.of();
    private String serverNode;

    public Builder logId(AuditLogId logId) {
      this.logId = logId;
      return this;
    }

    public Builder eventTime(Instant eventTime) {
      this.eventTime = eventTime;
      return this;
    }

    public Builder code(SystemErrorCode code) {
      this.code = code;
      return this;
    }

    public Builder outcome(AuditOutcome outcome) {
      this.outcome = outcome;
      return this;
    }

    public Builder actor(String actor) {
      this.actor = actor;
      return this;
    }

    public Builder target(String target) {
      this.target = target;
      return this;
    }

    public Builder sourceIp(String sourceIp) {
      this.sourceIp = sourceIp;
      return this;
    }

    public Builder sourceHost(String sourceHost) {
      this.sourceHost = sourceHost;
      return this;
    }

    public Builder sessionIdHash(String sessionIdHash) {
      this.sessionIdHash = sessionIdHash;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder userMessage(String userMessage) {
      this.userMessage = userMessage;
      return this;
    }

    public Builder logMessage(String logMessage) {
      this.logMessage = logMessage;
      return this;
    }

    public Builder formattedLine(String formattedLine) {
      this.formattedLine = formattedLine;
      return this;
    }

    public Builder attributes(Map<String, String> attributes) {
      this.attributes = attributes == null ? Map.of() : attributes;
      return this;
    }

    public Builder serverNode(String serverNode) {
      this.serverNode = serverNode;
      return this;
    }

    public AuditRecord build() {
      return new AuditRecord(this);
    }
  }
}
