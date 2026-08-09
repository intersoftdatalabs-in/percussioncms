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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Contextual metadata for an audit / error emit (actor, target, network origin, correlation).
 *
 * <p>Values may still contain sensitive data; {@link com.intsof.percussioncms.auditlog.redact.AuditRedactor}
 * runs before sinks.
 */
public final class AuditContext {

  private final String actor;
  private final String target;
  private final String sourceIp;
  private final String sourceHost;
  private final String sessionIdHash;
  private final String correlationId;
  private final Map<String, String> attributes;

  private AuditContext(Builder builder) {
    this.actor = builder.actor;
    this.target = builder.target;
    this.sourceIp = builder.sourceIp;
    this.sourceHost = builder.sourceHost;
    this.sessionIdHash = builder.sessionIdHash;
    this.correlationId = builder.correlationId;
    this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static AuditContext empty() {
    return builder().build();
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

  public Map<String, String> attributes() {
    return attributes;
  }

  public static final class Builder {
    private String actor;
    private String target;
    private String sourceIp;
    private String sourceHost;
    private String sessionIdHash;
    private String correlationId;
    private final Map<String, String> attributes = new LinkedHashMap<>();

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

    public Builder attribute(String key, String value) {
      Objects.requireNonNull(key, "key");
      if (value != null) {
        attributes.put(key, value);
      }
      return this;
    }

    public AuditContext build() {
      return new AuditContext(this);
    }
  }
}
