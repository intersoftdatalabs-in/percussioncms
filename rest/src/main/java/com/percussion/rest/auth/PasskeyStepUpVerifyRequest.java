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

/** Request payload to complete passkey step-up verification in an active session. */
@XmlRootElement(name = "PasskeyStepUpVerifyRequest")
@JsonRootName("PasskeyStepUpVerifyRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Verify a passkey assertion for step-up authentication")
public class PasskeyStepUpVerifyRequest {

  private String challengeTxId;
  private String credentialJson;

  public PasskeyStepUpVerifyRequest() {}

  public PasskeyStepUpVerifyRequest(String challengeTxId, String credentialJson) {
    this.challengeTxId = challengeTxId;
    this.credentialJson = credentialJson;
  }

  public String getChallengeTxId() {
    return challengeTxId;
  }

  public void setChallengeTxId(String challengeTxId) {
    this.challengeTxId = challengeTxId;
  }

  public String getCredentialJson() {
    return credentialJson;
  }

  public void setCredentialJson(String credentialJson) {
    this.credentialJson = credentialJson;
  }
}
