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
package com.percussion.utils.container.jboss;

import com.percussion.error.IPSErrorCode;

/**
 * Utils-local typed peer for leftover {@link IPSJBossErrors} production sites (#3859 / parent
 * #2616).
 *
 * <p>Modules that depend on {@code audit-log} should prefer {@code
 * com.intsof.percussioncms.auditlog.codes.JBossErrorCodes}. Utils cannot depend on {@code
 * audit-log} ({@code audit-log} depends on utils), so this {@link IPSErrorCode} carries the same
 * numeric code and non-auditable policy without a circular Maven dependency.
 *
 * <p>Numeric code is identical to {@link IPSJBossErrors#APP_POLICY_ELEMENT_MISSING} and to {@code
 * JBossErrorCodes.APP_POLICY_ELEMENT_MISSING}.
 */
public enum JBossErrorCode implements IPSErrorCode {

  /** Peer of {@link IPSJBossErrors#APP_POLICY_ELEMENT_MISSING} / JBossErrorCodes same. */
  APP_POLICY_ELEMENT_MISSING(IPSJBossErrors.APP_POLICY_ELEMENT_MISSING);

  private final int numericCode;

  JBossErrorCode(int numericCode) {
    this.numericCode = numericCode;
  }

  @Override
  public int numericCode() {
    return numericCode;
  }

  @Override
  public boolean isAuditable() {
    return false;
  }
}
