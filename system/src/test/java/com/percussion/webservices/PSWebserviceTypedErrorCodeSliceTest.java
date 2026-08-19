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
package com.percussion.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3585 (parent #2616): leftover webservice production call sites must throw typed {@link
 * WebserviceErrorCodes} via IPSErrorCode-aware {@link PSErrorException} constructors — not bare
 * {@code IPSWebserviceErrors} ints.
 */
@Tag("UnitTest")
public class PSWebserviceTypedErrorCodeSliceTest {

  @Test
  public void accessControlRetainsTypedAuditableCode() {
    PSErrorException ex =
        new PSErrorException(
            WebserviceErrorCodes.ACCESS_CONTROL_ERROR, "denied", "stack");
    assertEquals(WebserviceErrorCodes.ACCESS_CONTROL_ERROR.numericCode(), ex.getCode());
    assertSame(WebserviceErrorCodes.ACCESS_CONTROL_ERROR, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
  }

  @Test
  public void invalidSessionRetainsTypedAuditableCode() {
    PSErrorException ex =
        new PSErrorException(WebserviceErrorCodes.INVALID_SESSION, "bad session", "stack");
    assertEquals(3, ex.getCode());
    assertSame(WebserviceErrorCodes.INVALID_SESSION, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
  }

  @Test
  public void saveFailedRetainsTypedNonAuditableCode() {
    PSErrorException ex =
        new PSErrorException(WebserviceErrorCodes.SAVE_FAILED, "save failed", "stack");
    assertEquals(6, ex.getCode());
    assertSame(WebserviceErrorCodes.SAVE_FAILED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void legacyIntConstructionHasNoTypedCode() {
    PSErrorException ex = new PSErrorException(6, "save failed", "stack");
    assertEquals(6, ex.getCode());
    assertNull(ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSErrorException(null, "msg", "stack"));
  }

  @Test
  public void createErrorMessageAcceptsTypedCode() {
    String msg =
        PSWebserviceErrors.createErrorMessage(
            WebserviceErrorCodes.OBJECT_NOT_FOUND, "PSAclImpl", 42L);
    assertFalse(msg.isBlank());
  }

  @Test
  public void notAuthorizedLockSubclassRetainsTypedCode() {
    PSLockErrorException ex =
        new PSLockErrorException(
            WebserviceErrorCodes.CREATE_LOCK_FAILED, "lock failed", "stack", "alice", 5L);
    assertEquals(5, ex.getCode());
    assertSame(WebserviceErrorCodes.CREATE_LOCK_FAILED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals("alice", ex.getLocker());
    assertEquals(5L, ex.getRemainingTime());
  }
}
