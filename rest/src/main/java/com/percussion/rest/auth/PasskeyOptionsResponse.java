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

/** Response payload containing a WebAuthn challenge TX ID and serialized client options JSON. */
@XmlRootElement(name = "PasskeyOptionsResponse")
@JsonRootName("PasskeyOptionsResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "WebAuthn challenge TX ID and credential options JSON")
public class PasskeyOptionsResponse {

  private String challengeTxId;
  private String optionsJson;

  public PasskeyOptionsResponse() {}

  public PasskeyOptionsResponse(String challengeTxId, String optionsJson) {
    this.challengeTxId = challengeTxId;
    this.optionsJson = optionsJson;
  }

  public String getChallengeTxId() {
    return challengeTxId;
  }

  public void setChallengeTxId(String challengeTxId) {
    this.challengeTxId = challengeTxId;
  }

  public String getOptionsJson() {
    return optionsJson;
  }

  public void setOptionsJson(String optionsJson) {
    this.optionsJson = optionsJson;
  }
}
