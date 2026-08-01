/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.i18n;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response after accepting an i18n correction. */
@Schema(description = "Result of submitting an i18n correction")
public class I18nCorrectionResult {

  private String status;
  private String gcmMessageId;

  public I18nCorrectionResult() {}

  public I18nCorrectionResult(String status, String gcmMessageId) {
    this.status = status;
    this.gcmMessageId = gcmMessageId;
  }

  public static I18nCorrectionResult ok(String gcmMessageId) {
    return new I18nCorrectionResult("ok", gcmMessageId);
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getGcmMessageId() {
    return gcmMessageId;
  }

  public void setGcmMessageId(String gcmMessageId) {
    this.gcmMessageId = gcmMessageId;
  }
}
