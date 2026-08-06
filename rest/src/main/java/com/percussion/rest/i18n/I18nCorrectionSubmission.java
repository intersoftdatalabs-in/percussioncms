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

package com.percussion.rest.i18n;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wire DTO matching {@code @mkd/language} {@code CorrectionSubmission} JSON (camelCase).
 *
 * <p>Independent of the GCM Java SDK so the {@code rest} module stays free of native/JNA deps.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Crowd-sourced i18n correction from the browser language client")
public class I18nCorrectionSubmission {

  private String currentText;
  private String proposedText;
  private String currentAriaLabel;
  private String proposedAriaLabel;
  private String ariaLabelledby;
  private String currentTitle;
  private String messageId;
  private String notes;
  private String email;
  private String locale;
  private I18nCorrectionSource source;
  private String submittedAt;

  public String getCurrentText() {
    return currentText;
  }

  public void setCurrentText(String currentText) {
    this.currentText = currentText;
  }

  public String getProposedText() {
    return proposedText;
  }

  public void setProposedText(String proposedText) {
    this.proposedText = proposedText;
  }

  public String getCurrentAriaLabel() {
    return currentAriaLabel;
  }

  public void setCurrentAriaLabel(String currentAriaLabel) {
    this.currentAriaLabel = currentAriaLabel;
  }

  public String getProposedAriaLabel() {
    return proposedAriaLabel;
  }

  public void setProposedAriaLabel(String proposedAriaLabel) {
    this.proposedAriaLabel = proposedAriaLabel;
  }

  public String getAriaLabelledby() {
    return ariaLabelledby;
  }

  public void setAriaLabelledby(String ariaLabelledby) {
    this.ariaLabelledby = ariaLabelledby;
  }

  public String getCurrentTitle() {
    return currentTitle;
  }

  public void setCurrentTitle(String currentTitle) {
    this.currentTitle = currentTitle;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public I18nCorrectionSource getSource() {
    return source;
  }

  public void setSource(I18nCorrectionSource source) {
    this.source = source;
  }

  public String getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(String submittedAt) {
    this.submittedAt = submittedAt;
  }
}
