/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
package com.percussion.services.locking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.LockErrorCodes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.utils.guid.IPSGuid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Bulk ctor must accept null result slots (failed ids). {@link List#copyOf} NPEs on nulls. */
class PSLockExceptionBulkCtorTest {

  @Test
  void bulkCtorPreservesNullResultSlots() {
    IPSGuid guid = new PSGuid(PSTypeEnum.AUTO_TRANSLATIONS, 0);
    PSLockException inner =
        new PSLockException(
            LockErrorCodes.LOCK_EXTENSION_LOCKED_BY_SOMEBODY_ELSE,
            guid.longValue(),
            "other",
            30_000L);
    Map<IPSGuid, PSLockException> errors = new HashMap<>();
    errors.put(guid, inner);

    PSLockException bulk = new PSLockException(Arrays.asList((PSObjectLock) null), errors);

    assertEquals(1, bulk.getResults().size());
    assertNull(bulk.getResults().get(0));
    assertEquals(0, bulk.getSuccessCount());
    assertEquals(1, bulk.getErrorCount());
    assertSame(inner, bulk.getErrors().get(guid));
    assertEquals("other", bulk.getErrors().values().iterator().next().getLocker());
  }

  @Test
  void bulkCtorRejectsEmptyErrors() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSLockException(List.of(), Map.of()));
  }

  @Test
  void bulkOperationFailedFactoryAllowsNullSlots() {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 1033L);
    Map<IPSGuid, PSLockException> errors = new HashMap<>();
    errors.put(guid, PSLockException.lockNotFound(guid.longValue()));
    PSLockException bulk =
        PSLockException.bulkOperationFailed(Arrays.asList((PSObjectLock) null), errors);
    assertNotNull(bulk.getResults());
    assertNull(bulk.getResults().get(0));
  }
}
