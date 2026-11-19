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

/** Wire DTO summarizing a synchronous admin test auth email attempt. */
@XmlRootElement(name = "AuthMailTestResult")
@JsonRootName("AuthMailTestResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a synchronous test authentication email dispatch")
public class AuthMailTestResult {

  private boolean sent;
  private String recipient;
  private String source;
  private String message;

  public AuthMailTestResult() {}

  public AuthMailTestResult(boolean sent, String recipient, String source, String message) {
    this.sent = sent;
    this.recipient = recipient;
    this.source = source;
    this.message = message;
  }

  public boolean isSent() {
    return sent;
  }

  public void setSent(boolean sent) {
    this.sent = sent;
  }

  public String getRecipient() {
    return recipient;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
