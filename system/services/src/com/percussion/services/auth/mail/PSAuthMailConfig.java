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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.auth.mail;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Immutable snapshot of resolved transactional authentication mail settings.
 *
 * <p>Never emits {@link #getPassword()} in {@link #toString()}.
 */
public final class PSAuthMailConfig {

  public enum ConfigSource {
    SERVER_PROPERTIES,
    WORKFLOW_FALLBACK,
    UNCONFIGURED
  }

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String fromAddress;
  private final String fromName;
  private final boolean startTls;
  private final boolean startTlsRequired;
  private final boolean ssl;
  private final boolean sslCheckServerIdentity;
  private final int connectTimeoutMs;
  private final int socketTimeoutMs;
  private final ConfigSource source;
  private final boolean fallbackWorkflowSmtpEnabled;
  private final List<String> validationErrors;

  public PSAuthMailConfig(
      String host,
      int port,
      String username,
      String password,
      String fromAddress,
      String fromName,
      boolean startTls,
      boolean startTlsRequired,
      boolean ssl,
      boolean sslCheckServerIdentity,
      int connectTimeoutMs,
      int socketTimeoutMs,
      ConfigSource source,
      boolean fallbackWorkflowSmtpEnabled,
      List<String> validationErrors) {
    this.host = StringUtils.trimToEmpty(host);
    this.port = port;
    this.username = StringUtils.trimToEmpty(username);
    this.password = password == null ? "" : password;
    this.fromAddress = StringUtils.trimToEmpty(fromAddress);
    this.fromName = StringUtils.trimToEmpty(fromName);
    this.startTls = startTls;
    this.startTlsRequired = startTlsRequired;
    this.ssl = ssl;
    this.sslCheckServerIdentity = sslCheckServerIdentity;
    this.connectTimeoutMs = connectTimeoutMs;
    this.socketTimeoutMs = socketTimeoutMs;
    this.source = Objects.requireNonNull(source, "source");
    this.fallbackWorkflowSmtpEnabled = fallbackWorkflowSmtpEnabled;
    this.validationErrors =
        validationErrors == null ? List.of() : List.copyOf(validationErrors);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public String getFromName() {
    return fromName;
  }

  public boolean isStartTls() {
    return startTls;
  }

  public boolean isStartTlsRequired() {
    return startTlsRequired;
  }

  public boolean isSsl() {
    return ssl;
  }

  public boolean isSslCheckServerIdentity() {
    return sslCheckServerIdentity;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public int getSocketTimeoutMs() {
    return socketTimeoutMs;
  }

  public ConfigSource getSource() {
    return source;
  }

  public boolean isFallbackWorkflowSmtpEnabled() {
    return fallbackWorkflowSmtpEnabled;
  }

  public List<String> getValidationErrors() {
    return validationErrors;
  }

  public boolean hasCredentials() {
    return StringUtils.isNotBlank(username);
  }

  public boolean isConfigured() {
    return source != ConfigSource.UNCONFIGURED
        && StringUtils.isNotBlank(host)
        && port >= 1
        && port <= 65535
        && StringUtils.isNotBlank(fromAddress)
        && validationErrors.isEmpty();
  }

  @Override
  public String toString() {
    return "PSAuthMailConfig{host='"
        + host
        + "', port="
        + port
        + ", source="
        + source
        + ", fromAddress='"
        + fromAddress
        + "', hasCredentials="
        + hasCredentials()
        + ", password=[REDACTED]}";
  }
}
