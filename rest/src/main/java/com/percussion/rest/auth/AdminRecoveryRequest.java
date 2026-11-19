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

/** Wire DTO for initiating an audited Administrator break-glass MFA recovery action. */
@XmlRootElement(name = "AdminRecoveryRequest")
@JsonRootName("AdminRecoveryRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Administrator break-glass MFA recovery request")
public class AdminRecoveryRequest {

  private String targetUserName;
  private String verificationNote;
  private boolean resetImmediately;

  public AdminRecoveryRequest() {}

  public AdminRecoveryRequest(
      String targetUserName, String verificationNote, boolean resetImmediately) {
    this.targetUserName = targetUserName;
    this.verificationNote = verificationNote;
    this.resetImmediately = resetImmediately;
  }

  public String getTargetUserName() {
    return targetUserName;
  }

  public void setTargetUserName(String targetUserName) {
    this.targetUserName = targetUserName;
  }

  public String getVerificationNote() {
    return verificationNote;
  }

  public void setVerificationNote(String verificationNote) {
    this.verificationNote = verificationNote;
  }

  public boolean isResetImmediately() {
    return resetImmediately;
  }

  public void setResetImmediately(boolean resetImmediately) {
    this.resetImmediately = resetImmediately;
  }
}
