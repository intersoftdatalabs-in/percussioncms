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

/** Public-facing summary of an enrolled WebAuthn passkey. */
@XmlRootElement(name = "PasskeySummary")
@JsonRootName("PasskeySummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of an enrolled WebAuthn passkey")
public class PasskeySummary {

  private String passkeyId;
  private String label;
  private String aaguid;
  private List<String> transports = new ArrayList<>();
  private boolean backupEligible;
  private boolean backupState;
  private Instant createdAt;
  private Instant lastUsedAt;

  public PasskeySummary() {}

  public PasskeySummary(
      String passkeyId,
      String label,
      String aaguid,
      List<String> transports,
      boolean backupEligible,
      boolean backupState,
      Instant createdAt,
      Instant lastUsedAt) {
    this.passkeyId = passkeyId;
    this.label = label;
    this.aaguid = aaguid;
    this.transports = transports != null ? new ArrayList<>(transports) : new ArrayList<>();
    this.backupEligible = backupEligible;
    this.backupState = backupState;
    this.createdAt = createdAt;
    this.lastUsedAt = lastUsedAt;
  }

  public String getPasskeyId() {
    return passkeyId;
  }

  public void setPasskeyId(String passkeyId) {
    this.passkeyId = passkeyId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getAaguid() {
    return aaguid;
  }

  public void setAaguid(String aaguid) {
    this.aaguid = aaguid;
  }

  public List<String> getTransports() {
    return transports;
  }

  public void setTransports(List<String> transports) {
    this.transports = transports != null ? new ArrayList<>(transports) : new ArrayList<>();
  }

  public boolean isBackupEligible() {
    return backupEligible;
  }

  public void setBackupEligible(boolean backupEligible) {
    this.backupEligible = backupEligible;
  }

  public boolean isBackupState() {
    return backupState;
  }

  public void setBackupState(boolean backupState) {
    this.backupState = backupState;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
