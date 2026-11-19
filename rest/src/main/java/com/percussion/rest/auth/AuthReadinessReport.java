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
package com.percussion.rest.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full administrator readiness report combining infrastructure prerequisites and account coverage.
 *
 * <p>Wire root {@code AuthReadinessReport} matches {@code WRAP_ROOT_VALUE}/{@code
 * UNWRAP_ROOT_VALUE} in {@code JacksonContextResolver}.
 */
@XmlRootElement(name = "AuthReadinessReport")
@JsonRootName("AuthReadinessReport")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication, MFA, and account recovery readiness report")
public class AuthReadinessReport {

  private Instant generatedAt;
  private String mfaMode;
  private int passwordMinLength;
  private boolean mailConfigured;
  private String mailSource;
  private String clusterKeyFingerprint;
  private String overallStatus;
  private List<AuthInfraCheck> infraChecks = new ArrayList<>();
  private AuthAccountReadinessSummary accountSummary = new AuthAccountReadinessSummary();
  private List<AuthAccountReadinessItem> accountIssues = new ArrayList<>();

  public AuthReadinessReport() {}

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }

  public String getMfaMode() {
    return mfaMode;
  }

  public void setMfaMode(String mfaMode) {
    this.mfaMode = mfaMode;
  }

  public int getPasswordMinLength() {
    return passwordMinLength;
  }

  public void setPasswordMinLength(int passwordMinLength) {
    this.passwordMinLength = passwordMinLength;
  }

  public boolean isMailConfigured() {
    return mailConfigured;
  }

  public void setMailConfigured(boolean mailConfigured) {
    this.mailConfigured = mailConfigured;
  }

  public String getMailSource() {
    return mailSource;
  }

  public void setMailSource(String mailSource) {
    this.mailSource = mailSource;
  }

  public String getClusterKeyFingerprint() {
    return clusterKeyFingerprint;
  }

  public void setClusterKeyFingerprint(String clusterKeyFingerprint) {
    this.clusterKeyFingerprint = clusterKeyFingerprint;
  }

  public String getOverallStatus() {
    return overallStatus;
  }

  public void setOverallStatus(String overallStatus) {
    this.overallStatus = overallStatus;
  }

  public List<AuthInfraCheck> getInfraChecks() {
    return infraChecks;
  }

  public void setInfraChecks(List<AuthInfraCheck> infraChecks) {
    this.infraChecks = infraChecks != null ? infraChecks : new ArrayList<>();
  }

  public AuthAccountReadinessSummary getAccountSummary() {
    return accountSummary;
  }

  public void setAccountSummary(AuthAccountReadinessSummary accountSummary) {
    this.accountSummary =
        accountSummary != null ? accountSummary : new AuthAccountReadinessSummary();
  }

  public List<AuthAccountReadinessItem> getAccountIssues() {
    return accountIssues;
  }

  public void setAccountIssues(List<AuthAccountReadinessItem> accountIssues) {
    this.accountIssues = accountIssues != null ? accountIssues : new ArrayList<>();
  }
}
