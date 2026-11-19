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

/** Wire DTO for one infrastructure readiness check. */
@XmlRootElement(name = "AuthInfraCheck")
@JsonRootName("AuthInfraCheck")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication and MFA infrastructure readiness check")
public class AuthInfraCheck {

  private String code;
  private String label;
  private String status;
  private String summary;
  private String remediation;

  public AuthInfraCheck() {}

  public AuthInfraCheck(
      String code, String label, String status, String summary, String remediation) {
    this.code = code;
    this.label = label;
    this.status = status;
    this.summary = summary;
    this.remediation = remediation;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getRemediation() {
    return remediation;
  }

  public void setRemediation(String remediation) {
    this.remediation = remediation;
  }
}
