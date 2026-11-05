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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.webservices.IPSWebserviceErrors;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSInvalidStateException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4881: {@link SlotRelationshipAdaptor#httpStatusForMoveFailure(Exception)} must match the
 * typed {@link WebserviceErrorCodes} checkout-conflict codes instead of the bare {@code
 * IPSWebserviceErrors} ints, while keeping the HTTP mapping unchanged.
 *
 * <p>The production throw sites already construct typed exceptions ({@code PSContentWs} throws
 * {@link PSInvalidStateException} with {@link WebserviceErrorCodes#ITEM_NOT_CHECKED_OUT}; {@code
 * PSWebserviceUtils} throws {@link PSErrorException} with {@link
 * WebserviceErrorCodes#ITEM_NOT_CHECKOUT_BY_USER}), so the adaptor has to recognize the typed
 * numeric codes. Because {@code PSErrorException} also keeps its legacy int constructors, the
 * legacy-int path must keep mapping to 409 as well.
 */
@Tag("UnitTest")
class SlotRelationshipAdaptorTypedErrorCodeTest {

  @Test
  void typedCheckoutByUserCodeMapsTo409() {
    PSErrorException ex =
        new PSErrorException(WebserviceErrorCodes.ITEM_NOT_CHECKOUT_BY_USER, "held", "stack");

    assertEquals(409, SlotRelationshipAdaptor.httpStatusForMoveFailure(ex));
  }

  @Test
  void typedItemNotCheckedOutCodeMapsTo409() {
    PSErrorException ex =
        new PSErrorException(WebserviceErrorCodes.ITEM_NOT_CHECKED_OUT, "locked", "stack");

    assertEquals(409, SlotRelationshipAdaptor.httpStatusForMoveFailure(ex));
  }

  /** The real production throw site for a checkout conflict is a typed {@code PSInvalidStateException}. */
  @Test
  void typedInvalidStateExceptionMapsTo409() {
    PSInvalidStateException ex =
        new PSInvalidStateException(
            WebserviceErrorCodes.ITEM_NOT_CHECKED_OUT, "locked", "stack");

    assertEquals(409, SlotRelationshipAdaptor.httpStatusForMoveFailure(ex));
  }

  /** Legacy int-constructed exceptions keep their 409 mapping. */
  @Test
  void legacyIntConstructedCheckoutConflictStillMapsTo409() {
    PSErrorException ex =
        new PSErrorException(IPSWebserviceErrors.ITEM_NOT_CHECKOUT_BY_USER, "held", "stack");

    assertEquals(409, SlotRelationshipAdaptor.httpStatusForMoveFailure(ex));
  }

  /** The typed catalog must preserve the legacy numeric codes for the mapping to stay correct. */
  @Test
  void typedCodesPreserveLegacyNumericValues() {
    assertEquals(
        IPSWebserviceErrors.ITEM_NOT_CHECKED_OUT,
        WebserviceErrorCodes.ITEM_NOT_CHECKED_OUT.numericCode());
    assertEquals(
        IPSWebserviceErrors.ITEM_NOT_CHECKOUT_BY_USER,
        WebserviceErrorCodes.ITEM_NOT_CHECKOUT_BY_USER.numericCode());
  }

  /** A non-checkout webservice error is not a conflict. */
  @Test
  void unrelatedTypedCodeIsNotConflict() {
    PSErrorException ex =
        new PSErrorException(WebserviceErrorCodes.OBJECT_NOT_FOUND, "gone", "stack");

    assertNotEquals(409, SlotRelationshipAdaptor.httpStatusForMoveFailure(ex));
  }
}
