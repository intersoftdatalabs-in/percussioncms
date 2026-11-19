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

/** Wire DTO summarizing an Administrator break-glass MFA recovery action result. */
@XmlRootElement(name = "AdminRecoveryResponse")
@JsonRootName("AdminRecoveryResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Administrator break-glass MFA recovery action result")
public class AdminRecoveryResponse {

  private boolean issued;
  private String targetUserName;
  private String recoveryMode;
  private String delivery;
  private String expiresAt;
  private String message;

  public AdminRecoveryResponse() {}

  public AdminRecoveryResponse(
      boolean issued,
      String targetUserName,
      String recoveryMode,
      String delivery,
      String expiresAt,
      String message) {
    this.issued = issued;
    this.targetUserName = targetUserName;
    this.recoveryMode = recoveryMode;
    this.delivery = delivery;
    this.expiresAt = expiresAt;
    this.message = message;
  }

  public boolean isIssued() {
    return issued;
  }

  public void setIssued(boolean issued) {
    this.issued = issued;
  }

  public String getTargetUserName() {
    return targetUserName;
  }

  public void setTargetUserName(String targetUserName) {
    this.targetUserName = targetUserName;
  }

  public String getRecoveryMode() {
    return recoveryMode;
  }

  public void setRecoveryMode(String recoveryMode) {
    this.recoveryMode = recoveryMode;
  }

  public String getDelivery() {
    return delivery;
  }

  public void setDelivery(String delivery) {
    this.delivery = delivery;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
