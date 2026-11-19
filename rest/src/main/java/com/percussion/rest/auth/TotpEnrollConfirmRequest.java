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

/** Request payload to confirm pending TOTP enrollment using a generated 6-digit code. */
@XmlRootElement(name = "TotpEnrollConfirmRequest")
@JsonRootName("TotpEnrollConfirmRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Confirm pending TOTP enrollment")
public class TotpEnrollConfirmRequest {

  private String totpId;
  private String code;

  public TotpEnrollConfirmRequest() {}

  public TotpEnrollConfirmRequest(String totpId, String code) {
    this.totpId = totpId;
    this.code = code;
  }

  public String getTotpId() {
    return totpId;
  }

  public void setTotpId(String totpId) {
    this.totpId = totpId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
