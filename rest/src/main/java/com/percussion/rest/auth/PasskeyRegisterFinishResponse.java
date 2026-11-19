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
import java.util.ArrayList;
import java.util.List;

/** Outcome of finishing passkey enrollment (includes initial recovery codes if none existed). */
@XmlRootElement(name = "PasskeyRegisterFinishResponse")
@JsonRootName("PasskeyRegisterFinishResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Outcome of passkey registration")
public class PasskeyRegisterFinishResponse {

  private boolean registered;
  private PasskeySummary passkey;
  private List<String> recoveryCodes = new ArrayList<>();
  private String message;

  public PasskeyRegisterFinishResponse() {}

  public PasskeyRegisterFinishResponse(
      boolean registered, PasskeySummary passkey, List<String> recoveryCodes, String message) {
    this.registered = registered;
    this.passkey = passkey;
    this.recoveryCodes = recoveryCodes != null ? new ArrayList<>(recoveryCodes) : new ArrayList<>();
    this.message = message;
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

  public PasskeySummary getPasskey() {
    return passkey;
  }

  public void setPasskey(PasskeySummary passkey) {
    this.passkey = passkey;
  }

  public List<String> getRecoveryCodes() {
    return recoveryCodes;
  }

  public void setRecoveryCodes(List<String> recoveryCodes) {
    this.recoveryCodes = recoveryCodes != null ? new ArrayList<>(recoveryCodes) : new ArrayList<>();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
