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

/** Pending TOTP setup payload returned to the user's authenticated browser session. */
@XmlRootElement(name = "TotpEnrollStartResponse")
@JsonRootName("TotpEnrollStartResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "TOTP enrollment setup URI and manual Base32 secret")
public class TotpEnrollStartResponse {

  private String totpId;
  private String otpauthUri;
  private String manualSecret;
  private String issuer;
  private String accountName;
  private int digits;
  private int periodSeconds;

  public TotpEnrollStartResponse() {}

  public TotpEnrollStartResponse(
      String totpId,
      String otpauthUri,
      String manualSecret,
      String issuer,
      String accountName,
      int digits,
      int periodSeconds) {
    this.totpId = totpId;
    this.otpauthUri = otpauthUri;
    this.manualSecret = manualSecret;
    this.issuer = issuer;
    this.accountName = accountName;
    this.digits = digits;
    this.periodSeconds = periodSeconds;
  }

  public String getTotpId() {
    return totpId;
  }

  public void setTotpId(String totpId) {
    this.totpId = totpId;
  }

  public String getOtpauthUri() {
    return otpauthUri;
  }

  public void setOtpauthUri(String otpauthUri) {
    this.otpauthUri = otpauthUri;
  }

  public String getManualSecret() {
    return manualSecret;
  }

  public void setManualSecret(String manualSecret) {
    this.manualSecret = manualSecret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public int getDigits() {
    return digits;
  }

  public void setDigits(int digits) {
    this.digits = digits;
  }

  public int getPeriodSeconds() {
    return periodSeconds;
  }

  public void setPeriodSeconds(int periodSeconds) {
    this.periodSeconds = periodSeconds;
  }

  @Override
  public String toString() {
    return "TotpEnrollStartResponse[totpId="
        + totpId
        + ", accountName="
        + accountName
        + ", otpauthUri=[REDACTED], manualSecret=[REDACTED]]";
  }
}
