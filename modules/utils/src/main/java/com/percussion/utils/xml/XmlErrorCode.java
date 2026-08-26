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
package com.percussion.utils.xml;

import com.percussion.error.IPSErrorCode;

/**
 * Utils-local typed peers for leftover {@link IPSXmlErrors} production sites (#3859 / parent
 * #2616).
 *
 * <p>Modules that depend on {@code audit-log} should prefer {@code
 * com.intsof.percussioncms.auditlog.codes.XmlErrorCodes}. Utils cannot depend on {@code
 * audit-log} ({@code audit-log} depends on utils), so these {@link IPSErrorCode} values carry the
 * same package-local numeric codes and non-auditable policy for typed {@link
 * PSInvalidXmlException} construction without a circular Maven dependency.
 *
 * <p>Numeric codes are identical to the legacy {@link IPSXmlErrors} ints ({@code 1–6}) and to the
 * matching {@code XmlErrorCodes} enum constants.
 */
public enum XmlErrorCode implements IPSErrorCode {

  /** Peer of {@link IPSXmlErrors#XML_ELEMENT_MISSING} / XmlErrorCodes same. */
  XML_ELEMENT_MISSING(IPSXmlErrors.XML_ELEMENT_MISSING),

  /** Peer of {@link IPSXmlErrors#XML_ELEMENT_INVALID_VALUE} / XmlErrorCodes same. */
  XML_ELEMENT_INVALID_VALUE(IPSXmlErrors.XML_ELEMENT_INVALID_VALUE),

  /** Peer of {@link IPSXmlErrors#XML_TWO_ROOT_ELEMENTS} / XmlErrorCodes same. */
  XML_TWO_ROOT_ELEMENTS(IPSXmlErrors.XML_TWO_ROOT_ELEMENTS),

  /** Peer of {@link IPSXmlErrors#XML_ELEMENT_INVALID_ATTR} / XmlErrorCodes same. */
  XML_ELEMENT_INVALID_ATTR(IPSXmlErrors.XML_ELEMENT_INVALID_ATTR),

  /** Peer of {@link IPSXmlErrors#XML_ELEMENT_ATTR_INVALID_VAL} / XmlErrorCodes same. */
  XML_ELEMENT_ATTR_INVALID_VAL(IPSXmlErrors.XML_ELEMENT_ATTR_INVALID_VAL),

  /** Peer of {@link IPSXmlErrors#XML_RESTORE_ERROR} / XmlErrorCodes same. */
  XML_RESTORE_ERROR(IPSXmlErrors.XML_RESTORE_ERROR);

  private final int numericCode;

  XmlErrorCode(int numericCode) {
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
