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

/** Response DTO listing the current user's active passkeys and deletion policy flag. */
@XmlRootElement(name = "PasskeyListResponse")
@JsonRootName("PasskeyListResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "List of enrolled passkeys and whether last passkey removal is permitted")
public class PasskeyListResponse {

  private List<PasskeySummary> passkeys = new ArrayList<>();
  private boolean canDeleteLastPasskey;

  public PasskeyListResponse() {}

  public PasskeyListResponse(List<PasskeySummary> passkeys, boolean canDeleteLastPasskey) {
    this.passkeys = passkeys != null ? new ArrayList<>(passkeys) : new ArrayList<>();
    this.canDeleteLastPasskey = canDeleteLastPasskey;
  }

  public List<PasskeySummary> getPasskeys() {
    return passkeys;
  }

  public void setPasskeys(List<PasskeySummary> passkeys) {
    this.passkeys = passkeys != null ? new ArrayList<>(passkeys) : new ArrayList<>();
  }

  public boolean isCanDeleteLastPasskey() {
    return canDeleteLastPasskey;
  }

  public void setCanDeleteLastPasskey(boolean canDeleteLastPasskey) {
    this.canDeleteLastPasskey = canDeleteLastPasskey;
  }
}
