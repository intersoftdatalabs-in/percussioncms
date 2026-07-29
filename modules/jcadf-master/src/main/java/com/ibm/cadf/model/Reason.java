/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Reason} attached to an event describing either a reason code ({@code reasonType} +
 * {@code reasonCode}) or a policy decision ({@code policyType} + {@code policyId}). {@link
 * #isValid()} accepts whichever pair is fully populated.
 */
public class Reason extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The reason-type tag, may be {@code null}. */
  private String reasonType;

  /** The reason code value, may be {@code null}. */
  private String reasonCode;

  /** The policy-type tag, may be {@code null}. */
  private String policyType;

  /** The policy id, may be {@code null}. */
  private String policyId;

  /**
   * Constructs a reason with the supplied reason and policy fields. Either the reason pair or the
   * policy pair (or both) may be populated; {@link #isValid()} enforces which combinations are
   * accepted downstream.
   *
   * @param reasonType the reason-type tag, may be {@code null}.
   * @param reasonCode the reason code value, may be {@code null}.
   * @param policyType the policy-type tag, may be {@code null}.
   * @param policyId the policy id, may be {@code null}.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Reason(String reasonType, String reasonCode, String policyType, String policyId)
      throws CADFException {
    super();
    this.reasonType = reasonType;
    this.reasonCode = reasonCode;
    this.policyType = policyType;
    this.policyId = policyId;
  }

  /**
   * Returns the reason-type tag.
   *
   * @return the reason type, may be {@code null}.
   */
  public String getReasonType() {
    return reasonType;
  }

  /**
   * Sets the reason-type tag.
   *
   * @param reasonType the reason type, may be {@code null}.
   */
  public void setReasonType(String reasonType) {
    this.reasonType = reasonType;
  }

  /**
   * Returns the reason code value.
   *
   * @return the reason code, may be {@code null}.
   */
  public String getReasonCode() {
    return reasonCode;
  }

  /**
   * Sets the reason code value.
   *
   * @param reasonCode the reason code, may be {@code null}.
   */
  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  /**
   * Returns the policy-type tag.
   *
   * @return the policy type, may be {@code null}.
   */
  public String getPolicyType() {
    return policyType;
  }

  /**
   * Sets the policy-type tag.
   *
   * @param policyType the policy type, may be {@code null}.
   */
  public void setPolicyType(String policyType) {
    this.policyType = policyType;
  }

  /**
   * Returns the policy id.
   *
   * @return the policy id, may be {@code null}.
   */
  public String getPolicyId() {
    return policyId;
  }

  /**
   * Sets the policy id.
   *
   * @param policyId the policy id, may be {@code null}.
   */
  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  /**
   * Validates that one of the two possible (reasonType,reasonCode) or (policyType,policyId) pairs
   * is fully populated.
   *
   * @return {@code true} when at least one pair is complete.
   */
  @Override
  public boolean isValid() {
    return (StringUtils.isNotEmpty(reasonType) && StringUtils.isNotEmpty(reasonCode))
        || (StringUtils.isNotEmpty(policyType) && StringUtils.isNotEmpty(policyId));
  }
}
