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

/** Outcome of verifying step-up MFA in an active session. */
@XmlRootElement(name = "MfaStepUpResponse")
@JsonRootName("MfaStepUpResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Outcome of step-up MFA verification")
public class MfaStepUpResponse {

  private boolean verified;
  private long expiresAtEpochMs;
  private String message;

  public MfaStepUpResponse() {}

  public MfaStepUpResponse(boolean verified, long expiresAtEpochMs, String message) {
    this.verified = verified;
    this.expiresAtEpochMs = expiresAtEpochMs;
    this.message = message;
  }

  public boolean isVerified() {
    return verified;
  }

  public void setVerified(boolean verified) {
    this.verified = verified;
  }

  public long getExpiresAtEpochMs() {
    return expiresAtEpochMs;
  }

  public void setExpiresAtEpochMs(long expiresAtEpochMs) {
    this.expiresAtEpochMs = expiresAtEpochMs;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
