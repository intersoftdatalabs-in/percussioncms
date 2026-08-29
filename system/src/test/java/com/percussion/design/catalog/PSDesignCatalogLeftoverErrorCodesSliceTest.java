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
package com.percussion.design.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.IPSServerErrors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3969 (parent #2616): leftover {@code com.percussion.design.catalog} production sites throw
 * typed {@code CatalogErrorCodes} / {@code ServerErrorCodes} (not bare {@code IPS*Errors} ints).
 * Dual-write is skipped: leftover catalog protocol codes and {@code RESPONSE_SEND_ERROR} are
 * non-auditable.
 */
@Tag("UnitTest")
class PSDesignCatalogLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSCatalogErrors.REQ_DOC_MISSING, CatalogErrorCodes.REQ_DOC_MISSING.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_MISSING_GENERIC,
        CatalogErrorCodes.REQ_DOC_MISSING_GENERIC.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_ROOT_MISSING_GENERIC,
        CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_INVALID_TYPE,
        CatalogErrorCodes.REQ_DOC_INVALID_TYPE.numericCode());
    assertEquals(
        IPSCatalogErrors.NO_REQ_HANDLER_FOUND,
        CatalogErrorCodes.NO_REQ_HANDLER_FOUND.numericCode());
    assertEquals(
        IPSCatalogErrors.CATALOG_EXCEPTION, CatalogErrorCodes.CATALOG_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.RESPONSE_SEND_ERROR, ServerErrorCodes.RESPONSE_SEND_ERROR.numericCode());

    leftoverNonAuditable(CatalogErrorCodes.REQ_DOC_MISSING);
    leftoverNonAuditable(CatalogErrorCodes.REQ_DOC_MISSING_GENERIC);
    leftoverNonAuditable(CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC);
    leftoverNonAuditable(CatalogErrorCodes.REQ_DOC_INVALID_TYPE);
    leftoverNonAuditable(CatalogErrorCodes.NO_REQ_HANDLER_FOUND);
    leftoverNonAuditable(CatalogErrorCodes.CATALOG_EXCEPTION);
    leftoverNonAuditable(ServerErrorCodes.RESPONSE_SEND_ERROR);
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() {
    Object[] missingArgs = {"data", "Column", "PSXColumnCatalog"};
    PSIllegalArgumentException missingDoc =
        new PSIllegalArgumentException(CatalogErrorCodes.REQ_DOC_MISSING, missingArgs);
    assertSame(CatalogErrorCodes.REQ_DOC_MISSING, missingDoc.getTypedErrorCode());
    assertEquals(IPSCatalogErrors.REQ_DOC_MISSING, missingDoc.getErrorCode());
    assertFalse(missingDoc.isAuditable());

    Object[] invalidTypeArgs = {"PSXColumnCatalog", "WrongRoot"};
    PSIllegalArgumentException invalidType =
        new PSIllegalArgumentException(CatalogErrorCodes.REQ_DOC_INVALID_TYPE, invalidTypeArgs);
    assertSame(CatalogErrorCodes.REQ_DOC_INVALID_TYPE, invalidType.getTypedErrorCode());
    assertEquals(IPSCatalogErrors.REQ_DOC_INVALID_TYPE, invalidType.getErrorCode());
    assertFalse(invalidType.isAuditable());

    PSIllegalArgumentException missingGeneric =
        new PSIllegalArgumentException(CatalogErrorCodes.REQ_DOC_MISSING_GENERIC);
    assertSame(CatalogErrorCodes.REQ_DOC_MISSING_GENERIC, missingGeneric.getTypedErrorCode());
    assertEquals(IPSCatalogErrors.REQ_DOC_MISSING_GENERIC, missingGeneric.getErrorCode());
    assertFalse(missingGeneric.isAuditable());

    PSIllegalArgumentException missingRoot =
        new PSIllegalArgumentException(CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC);
    assertSame(CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC, missingRoot.getTypedErrorCode());
    assertEquals(IPSCatalogErrors.REQ_DOC_ROOT_MISSING_GENERIC, missingRoot.getErrorCode());
    assertFalse(missingRoot.isAuditable());

    Object[] noHandlerArgs = {"UnknownCatalogRoot"};
    PSIllegalArgumentException noHandler =
        new PSIllegalArgumentException(CatalogErrorCodes.NO_REQ_HANDLER_FOUND, noHandlerArgs);
    assertSame(CatalogErrorCodes.NO_REQ_HANDLER_FOUND, noHandler.getTypedErrorCode());
    assertEquals(IPSCatalogErrors.NO_REQ_HANDLER_FOUND, noHandler.getErrorCode());
    assertFalse(noHandler.isAuditable());
  }

  @Test
  void typedProductionCtorsRejectNullCode() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSIllegalArgumentException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSIllegalArgumentException((IPSErrorCode) null, "arg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSIllegalArgumentException((IPSErrorCode) null, new Object[] {"arg"}));
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }
}
