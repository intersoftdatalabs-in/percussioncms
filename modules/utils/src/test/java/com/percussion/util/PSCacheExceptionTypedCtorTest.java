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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.error.IPSErrorCode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Issue #4153: additive {@link IPSErrorCode} constructors on {@link PSCacheException}. */
@Tag("UnitTest")
class PSCacheExceptionTypedCtorTest {

  private static final IPSErrorCode SAMPLE =
      new IPSErrorCode() {
        @Override
        public int numericCode() {
          return 1301;
        }

        @Override
        public boolean isAuditable() {
          return false;
        }
      };

  @Test
  void typedConstructorsRetainCodeAndSkipAudit() {
    PSCacheException noArgs = new PSCacheException(SAMPLE);
    assertSame(SAMPLE, noArgs.getTypedErrorCode());
    assertEquals(1301, noArgs.getErrorCode());
    assertFalse(noArgs.isAuditable());

    PSCacheException single = new PSCacheException(SAMPLE, "disk");
    assertSame(SAMPLE, single.getTypedErrorCode());
    assertEquals(1301, single.getErrorCode());

    PSCacheException array = new PSCacheException(SAMPLE, new Object[] {"a", "b"});
    assertSame(SAMPLE, array.getTypedErrorCode());

    RuntimeException cause = new RuntimeException("boom");
    PSCacheException causeOnly = new PSCacheException(SAMPLE, cause);
    assertSame(SAMPLE, causeOnly.getTypedErrorCode());
    assertSame(cause, causeOnly.getCause());

    PSCacheException withCause = new PSCacheException(SAMPLE, new Object[] {"msg"}, cause);
    assertSame(SAMPLE, withCause.getTypedErrorCode());
    assertSame(cause, withCause.getCause());
  }

  @Test
  void objectArgRejectsThrowableSoCauseIsNotSwallowed() {
    Object boxedCause = new RuntimeException("boom");
    assertThrows(
        IllegalArgumentException.class, () -> new PSCacheException(SAMPLE, boxedCause));
    assertSame(boxedCause, new PSCacheException(SAMPLE, (Throwable) boxedCause).getCause());
  }

  @Test
  void typedConstructorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCacheException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSCacheException((IPSErrorCode) null, "disk"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCacheException((IPSErrorCode) null, new RuntimeException("x")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSCacheException(
                (IPSErrorCode) null, new Object[] {"disk"}, new RuntimeException("x")));
  }
}
