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
package com.percussion.util;

import com.percussion.error.IPSErrorCode;

/**
 * Utils-local typed peers for leftover {@link IPSUtilErrors} production sites (#3859 / parent
 * #2616).
 *
 * <p>Modules that depend on {@code audit-log} should prefer {@code
 * com.intsof.percussioncms.auditlog.codes.UtilErrorCodes}. Utils cannot depend on {@code
 * audit-log} ({@code audit-log} depends on utils), so these {@link IPSErrorCode} values carry the
 * same numeric codes and non-auditable policy for typed {@link
 * com.percussion.error.PSException} construction without a circular Maven dependency.
 *
 * <p>Numeric codes are identical to the legacy {@link IPSUtilErrors} ints and to the matching
 * {@code UtilErrorCodes} enum constants.
 */
public enum UtilErrorCode implements IPSErrorCode {

  /** Peer of {@link IPSUtilErrors#BASE64_ENCODING_EXCEPTION} / UtilErrorCodes same. */
  BASE64_ENCODING_EXCEPTION(IPSUtilErrors.BASE64_ENCODING_EXCEPTION),

  /** Peer of {@link IPSUtilErrors#BASE64_DECODING_EXCEPTION} / UtilErrorCodes same. */
  BASE64_DECODING_EXCEPTION(IPSUtilErrors.BASE64_DECODING_EXCEPTION),

  /** Peer of {@link IPSUtilErrors#COLLECTION_CLASS_NOT_FOUND} / UtilErrorCodes same. */
  COLLECTION_CLASS_NOT_FOUND(IPSUtilErrors.COLLECTION_CLASS_NOT_FOUND),

  /** Peer of {@link IPSUtilErrors#PURGABLE_TEMP_DIR_IS_FILE} / UtilErrorCodes same. */
  PURGABLE_TEMP_DIR_IS_FILE(IPSUtilErrors.PURGABLE_TEMP_DIR_IS_FILE),

  /** Peer of {@link IPSUtilErrors#RECEIVE_DATA_ERROR} / UtilErrorCodes same. */
  RECEIVE_DATA_ERROR(IPSUtilErrors.RECEIVE_DATA_ERROR),

  /** Peer of {@link IPSUtilErrors#POST_DATA_ERROR} / UtilErrorCodes same. */
  POST_DATA_ERROR(IPSUtilErrors.POST_DATA_ERROR);

  private final int numericCode;

  UtilErrorCode(int numericCode) {
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
