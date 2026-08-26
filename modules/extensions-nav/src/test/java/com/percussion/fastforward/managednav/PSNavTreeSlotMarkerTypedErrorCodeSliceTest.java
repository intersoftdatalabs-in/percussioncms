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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.cms.PSCmsException;
import com.percussion.error.PSNotFoundException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Leftover nav production throw sites now construct typed {@code *ErrorCodes} (issue #3770).
 * Authtype / internal-request paths require live server config; the typed exceptions they wrap
 * are asserted here.
 */
@Tag("UnitTest")
class PSNavTreeSlotMarkerTypedErrorCodeSliceTest {

  @Test
  void invalidAuthtypeCmsExceptionIsTypedAndNonAuditable() {
    Object[] args = {"3", "authtypes.xml"};
    PSCmsException ex = new PSCmsException(CmsErrorCodes.INVALID_AUTHTYPE, args);
    assertSame(CmsErrorCodes.INVALID_AUTHTYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void missingInternalRequestNotFoundExceptionIsTypedAndNonAuditable() {
    Object[] args = {"sys_casSupport/casSupport_0", "No request handler found."};
    PSNotFoundException ex =
        new PSNotFoundException(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, args);
    assertSame(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
