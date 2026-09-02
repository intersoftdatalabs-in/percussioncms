/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Typed {@link IPSErrorCode} constructors on {@link PSInternalError} and {@link PSNonFatalError}
 * retain the catalog code, numeric code, and non-auditable flag (PR #4163).
 */
@Tag("UnitTest")
class PSLogErrorTypedConstructorSliceTest {

  @Test
  void internalErrorTypedConstructorsRetainCodeAndSkipAudit() {
    PSInternalError array =
        new PSInternalError(
            ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE, new Object[] {"disk"});
    assertSame(ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE, array.getTypedErrorCode());
    assertEquals(
        ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE.numericCode(), array.getErrorCode());
    assertFalse(array.isAuditable());
    assertFalse(ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE.isAuditable());

    PSInternalError single =
        new PSInternalError(ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE, "disk");
    assertSame(ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE, single.getTypedErrorCode());
    assertEquals(
        ServerErrorCodes.CACHE_STORE_TO_DISK_FAILURE.numericCode(), single.getErrorCode());
    assertFalse(single.isAuditable());
  }

  @Test
  void nonFatalErrorTypedConstructorsRetainCodeAndSkipAudit() {
    PSNonFatalError array =
        new PSNonFatalError(ServerErrorCodes.NO_AUTHORIZATION, new Object[] {"jdoe"});
    assertSame(ServerErrorCodes.NO_AUTHORIZATION, array.getTypedErrorCode());
    assertEquals(ServerErrorCodes.NO_AUTHORIZATION.numericCode(), array.getErrorCode());
    assertEquals(ServerErrorCodes.NO_AUTHORIZATION.isAuditable(), array.isAuditable());

    PSNonFatalError single = new PSNonFatalError(ServerErrorCodes.NO_AUTHORIZATION, "jdoe");
    assertSame(ServerErrorCodes.NO_AUTHORIZATION, single.getTypedErrorCode());
    assertEquals(ServerErrorCodes.NO_AUTHORIZATION.numericCode(), single.getErrorCode());
    assertEquals(ServerErrorCodes.NO_AUTHORIZATION.isAuditable(), single.isAuditable());
  }

  @Test
  void typedConstructorsRejectNullCode() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSInternalError((IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class, () -> new PSInternalError((IPSErrorCode) null, "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSNonFatalError((IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class, () -> new PSNonFatalError((IPSErrorCode) null, "x"));
  }
}
