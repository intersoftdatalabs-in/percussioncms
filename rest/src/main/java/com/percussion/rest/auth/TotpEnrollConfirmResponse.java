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

/** Outcome of confirming a user's authenticator app enrollment (includes initial recovery codes). */
@XmlRootElement(name = "TotpEnrollConfirmResponse")
@JsonRootName("TotpEnrollConfirmResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Outcome of TOTP enrollment confirmation")
public class TotpEnrollConfirmResponse {

  private boolean enrolled;
  private List<String> recoveryCodes = new ArrayList<>();
  private String message;

  public TotpEnrollConfirmResponse() {}

  public TotpEnrollConfirmResponse(boolean enrolled, List<String> recoveryCodes, String message) {
    this.enrolled = enrolled;
    this.recoveryCodes = recoveryCodes != null ? new ArrayList<>(recoveryCodes) : new ArrayList<>();
    this.message = message;
  }

  public boolean isEnrolled() {
    return enrolled;
  }

  public void setEnrolled(boolean enrolled) {
    this.enrolled = enrolled;
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
