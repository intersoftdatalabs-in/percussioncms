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

/** Self-service MFA factor and step-up verification status for the signed-in user. */
@XmlRootElement(name = "ProfileMfaStatus")
@JsonRootName("ProfileMfaStatus")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Current user's MFA enrollment and step-up status")
public class ProfileMfaStatus {

  private String userName;
  private String mfaMode;
  private boolean mfaRequired;
  private boolean totpEnabled;
  private int passkeyCount;
  private long recoveryCodesRemaining;
  private boolean canDisableTotp;
  private boolean stepUpActive;
  private Instant stepUpExpiresAt;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getMfaMode() {
    return mfaMode;
  }

  public void setMfaMode(String mfaMode) {
    this.mfaMode = mfaMode;
  }

  public boolean isMfaRequired() {
    return mfaRequired;
  }

  public void setMfaRequired(boolean mfaRequired) {
    this.mfaRequired = mfaRequired;
  }

  public boolean isTotpEnabled() {
    return totpEnabled;
  }

  public void setTotpEnabled(boolean totpEnabled) {
    this.totpEnabled = totpEnabled;
  }

  public int getPasskeyCount() {
    return passkeyCount;
  }

  public void setPasskeyCount(int passkeyCount) {
    this.passkeyCount = passkeyCount;
  }

  public long getRecoveryCodesRemaining() {
    return recoveryCodesRemaining;
  }

  public void setRecoveryCodesRemaining(long recoveryCodesRemaining) {
    this.recoveryCodesRemaining = recoveryCodesRemaining;
  }

  public boolean isCanDisableTotp() {
    return canDisableTotp;
  }

  public void setCanDisableTotp(boolean canDisableTotp) {
    this.canDisableTotp = canDisableTotp;
  }

  public boolean isStepUpActive() {
    return stepUpActive;
  }

  public void setStepUpActive(boolean stepUpActive) {
    this.stepUpActive = stepUpActive;
  }

  public Instant getStepUpExpiresAt() {
    return stepUpExpiresAt;
  }

  public void setStepUpExpiresAt(Instant stepUpExpiresAt) {
    this.stepUpExpiresAt = stepUpExpiresAt;
  }
}
