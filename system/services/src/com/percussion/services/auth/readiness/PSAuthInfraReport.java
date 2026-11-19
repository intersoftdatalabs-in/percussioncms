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
package com.percussion.services.auth.readiness;

import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Backend infrastructure readiness snapshot for Authentication &amp; MFA. */
public final class PSAuthInfraReport {

  private final Instant generatedAt;
  private final String mfaMode;
  private final int passwordMinLength;
  private final boolean mailConfigured;
  private final ConfigSource mailSource;
  private final String clusterKeyFingerprint;
  private final List<PSAuthInfraCheck> checks;
  private final Map<String, Boolean> schemaTables;

  public PSAuthInfraReport(
      Instant generatedAt,
      String mfaMode,
      int passwordMinLength,
      boolean mailConfigured,
      ConfigSource mailSource,
      String clusterKeyFingerprint,
      List<PSAuthInfraCheck> checks,
      Map<String, Boolean> schemaTables) {
    this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    this.mfaMode = mfaMode == null ? "optional" : mfaMode;
    this.passwordMinLength = passwordMinLength;
    this.mailConfigured = mailConfigured;
    this.mailSource = Objects.requireNonNull(mailSource, "mailSource");
    this.clusterKeyFingerprint = clusterKeyFingerprint == null ? "" : clusterKeyFingerprint;
    this.checks = checks == null ? List.of() : List.copyOf(checks);
    this.schemaTables = schemaTables == null ? Map.of() : Map.copyOf(schemaTables);
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public String getMfaMode() {
    return mfaMode;
  }

  public int getPasswordMinLength() {
    return passwordMinLength;
  }

  public boolean isMailConfigured() {
    return mailConfigured;
  }

  public ConfigSource getMailSource() {
    return mailSource;
  }

  public String getClusterKeyFingerprint() {
    return clusterKeyFingerprint;
  }

  public List<PSAuthInfraCheck> getChecks() {
    return checks;
  }

  public Map<String, Boolean> getSchemaTables() {
    return schemaTables;
  }
}
