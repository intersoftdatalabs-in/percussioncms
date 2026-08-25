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
package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.percussion.data.PSConversionException;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.error.IPSErrorCode;
import com.percussion.server.PSRequestValidationException;
import org.junit.jupiter.api.Test;

/**
 * Additive {@link IPSErrorCode} constructors on extension-related exceptions (#3756). Leftover
 * catalog codes used by extensions-main remain non-auditable.
 */
class PSExtensionTypedErrorCodeCtorTest {

  @Test
  void extensionExceptionTypedArrayCtorRetainsCodeAndSkipsAudit() {
    Object[] args = {"expected", "actual"};
    PSExtensionException ex =
        new PSExtensionException(ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH, args);
    assertSame(ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH, ex.getTypedErrorCode());
    assertEquals(ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals(2, ex.getErrorArguments().length);
  }

  @Test
  void extensionExceptionTypedCauseCtorRetainsCode() {
    RuntimeException cause = new RuntimeException("scheme");
    Object[] args = {"variant", 1L, 2L};
    PSExtensionException ex =
        new PSExtensionException(ExtensionErrorCodes.SCHEME_CANT_BE_FOUND, cause, args);
    assertSame(ExtensionErrorCodes.SCHEME_CANT_BE_FOUND, ex.getTypedErrorCode());
    assertSame(cause, ex.getCause());
    assertFalse(ex.isAuditable());
  }

  @Test
  void extensionProcessingExceptionTypedCtors() {
    PSExtensionProcessingException noArgs =
        new PSExtensionProcessingException(XmlErrorCodes.XML_PROCESSING_ERROR);
    assertEquals(XmlErrorCodes.XML_PROCESSING_ERROR.numericCode(), noArgs.getErrorCode());
    assertFalse(noArgs.isAuditable());

    PSExtensionProcessingException single =
        new PSExtensionProcessingException(
            ExtensionErrorCodes.CATALOG_EXT_RESOURCE_ERROR, "missing");
    assertSame(ExtensionErrorCodes.CATALOG_EXT_RESOURCE_ERROR, single.getTypedErrorCode());

    Object[] args = {"authtype", "config"};
    PSExtensionProcessingException array =
        new PSExtensionProcessingException(
            ExtensionErrorCodes.AUTHTYPE_REGISTRATION_MISSING, args);
    assertEquals(2, array.getErrorArguments().length);
    assertFalse(array.isAuditable());
  }

  @Test
  void conversionExceptionTypedVarargsCtor() {
    PSConversionException ex =
        new PSConversionException(
            ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR,
            "sys_variantid",
            "VariantId is required");
    assertSame(
        ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR, ex.getTypedErrorCode());
    assertEquals(2, ex.getErrorArguments().length);
    assertFalse(ex.isAuditable());
  }

  @Test
  void requestValidationExceptionTypedCtor() {
    Object[] args = {"en-us", "123"};
    PSRequestValidationException ex =
        new PSRequestValidationException(ExtensionErrorCodes.TRANSLATION_ALREADY_EXISTS, args);
    assertSame(ExtensionErrorCodes.TRANSLATION_ALREADY_EXISTS, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void internalRequestCallExceptionTypedCtor() {
    PSInternalRequestCallException ex =
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION, "sys_cmpUserCommunities/lookup.xml");
    assertSame(DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void typedCtorsRejectNullCode() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSExtensionException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSExtensionProcessingException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSConversionException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSRequestValidationException((IPSErrorCode) null));
  }
}
