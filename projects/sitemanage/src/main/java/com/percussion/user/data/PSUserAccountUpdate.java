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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.user.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Self-service account update payload for the signed-in user only (issue #2395).
 *
 * <p>Does not accept a user name — the server always applies changes to the current session user
 * (no IDOR). Only fields the product may persist for self-service are included (currently email
 * for {@link PSUserProviderType#INTERNAL} users).
 */
@XmlRootElement(name = "UserAccountUpdate")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("UserAccountUpdate")
public class PSUserAccountUpdate {

  private String email = "";

  /** Email address to store for the current user. Never {@code null}; may be empty. */
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email == null ? "" : email;
  }
}
