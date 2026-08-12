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
package com.percussion.design.objectstore;

import com.percussion.error.IPSErrorCode;

/**
 * Utils-local typed peers for the structural object-store XML codes used from this module.
 *
 * <p>Modules that depend on {@code audit-log} should prefer {@code
 * com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes}. Utils cannot depend on
 * {@code audit-log} ({@code audit-log} depends on utils), so these {@link IPSErrorCode} values
 * carry the same numeric codes and non-auditable policy for typed {@link
 * com.percussion.error.PSException} construction without a circular Maven dependency.
 *
 * <p>Numeric codes are identical to the legacy {@link IPSObjectStoreErrors} ints and to the
 * matching {@code ObjectStoreErrorCodes} enum constants (2012 / 2014 / 2015).
 */
public enum ObjectStoreErrorCode implements IPSErrorCode {

  /** Peer of {@link IPSObjectStoreErrors#XML_ELEMENT_WRONG_TYPE} / ObjectStoreErrorCodes same. */
  XML_ELEMENT_WRONG_TYPE(IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE),

  /** Peer of {@link IPSObjectStoreErrors#XML_ELEMENT_INVALID_ATTR} / ObjectStoreErrorCodes same. */
  XML_ELEMENT_INVALID_ATTR(IPSObjectStoreErrors.XML_ELEMENT_INVALID_ATTR),

  /** Peer of {@link IPSObjectStoreErrors#XML_ELEMENT_INVALID_CHILD} / ObjectStoreErrorCodes same. */
  XML_ELEMENT_INVALID_CHILD(IPSObjectStoreErrors.XML_ELEMENT_INVALID_CHILD);

  private final int numericCode;

  ObjectStoreErrorCode(int numericCode) {
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
