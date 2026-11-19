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

/** Aggregated account readiness counters across CMS accounts. */
@XmlRootElement(name = "AuthAccountReadinessSummary")
@JsonRootName("AuthAccountReadinessSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated account email and MFA readiness counts")
public class AuthAccountReadinessSummary {

  private int totalAccounts;
  private int internalAccounts;
  private int directoryAccounts;
  private int readyCount;
  private int missingEmailCount;
  private int malformedEmailCount;
  private int duplicateEmailCount;
  private int unverifiedEmailCount;
  private int mfaEnrolledCount;
  private int totpEnrolledCount;
  private int passkeyEnrolledCount;

  public AuthAccountReadinessSummary() {}

  public int getTotalAccounts() {
    return totalAccounts;
  }

  public void setTotalAccounts(int totalAccounts) {
    this.totalAccounts = totalAccounts;
  }

  public int getInternalAccounts() {
    return internalAccounts;
  }

  public void setInternalAccounts(int internalAccounts) {
    this.internalAccounts = internalAccounts;
  }

  public int getDirectoryAccounts() {
    return directoryAccounts;
  }

  public void setDirectoryAccounts(int directoryAccounts) {
    this.directoryAccounts = directoryAccounts;
  }

  public int getReadyCount() {
    return readyCount;
  }

  public void setReadyCount(int readyCount) {
    this.readyCount = readyCount;
  }

  public int getMissingEmailCount() {
    return missingEmailCount;
  }

  public void setMissingEmailCount(int missingEmailCount) {
    this.missingEmailCount = missingEmailCount;
  }

  public int getMalformedEmailCount() {
    return malformedEmailCount;
  }

  public void setMalformedEmailCount(int malformedEmailCount) {
    this.malformedEmailCount = malformedEmailCount;
  }

  public int getDuplicateEmailCount() {
    return duplicateEmailCount;
  }

  public void setDuplicateEmailCount(int duplicateEmailCount) {
    this.duplicateEmailCount = duplicateEmailCount;
  }

  public int getUnverifiedEmailCount() {
    return unverifiedEmailCount;
  }

  public void setUnverifiedEmailCount(int unverifiedEmailCount) {
    this.unverifiedEmailCount = unverifiedEmailCount;
  }

  public int getMfaEnrolledCount() {
    return mfaEnrolledCount;
  }

  public void setMfaEnrolledCount(int mfaEnrolledCount) {
    this.mfaEnrolledCount = mfaEnrolledCount;
  }

  public int getTotpEnrolledCount() {
    return totpEnrolledCount;
  }

  public void setTotpEnrolledCount(int totpEnrolledCount) {
    this.totpEnrolledCount = totpEnrolledCount;
  }

  public int getPasskeyEnrolledCount() {
    return passkeyEnrolledCount;
  }

  public void setPasskeyEnrolledCount(int passkeyEnrolledCount) {
    this.passkeyEnrolledCount = passkeyEnrolledCount;
  }
}
