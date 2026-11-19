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
package com.percussion.services.auth.readiness;

import java.util.Objects;

/** Single infrastructure readiness finding for Admin Auth &amp; MFA Readiness. */
public final class PSAuthInfraCheck {

  private final String code;
  private final String label;
  private final PSAuthCheckStatus status;
  private final String summary;
  private final String remediation;

  public PSAuthInfraCheck(
      String code,
      String label,
      PSAuthCheckStatus status,
      String summary,
      String remediation) {
    this.code = Objects.requireNonNull(code, "code");
    this.label = Objects.requireNonNull(label, "label");
    this.status = Objects.requireNonNull(status, "status");
    this.summary = summary == null ? "" : summary;
    this.remediation = remediation == null ? "" : remediation;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public PSAuthCheckStatus getStatus() {
    return status;
  }

  public String getSummary() {
    return summary;
  }

  public String getRemediation() {
    return remediation;
  }
}
