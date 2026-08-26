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
package com.percussion.utils.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.error.IPSErrorCode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSBaseExceptionCauseTest {

  /** Minimal concrete subclass for constructor coverage. */
  private static final class TestBaseException extends PSBaseException {
    private static final long serialVersionUID = 1L;

    TestBaseException(int msgCode, Throwable cause, Object... args) {
      super(msgCode, cause, args);
    }

    TestBaseException(String message, Throwable cause) {
      super(message, cause);
    }

    TestBaseException(IPSErrorCode code, Object... args) {
      super(code, args);
    }

    TestBaseException(IPSErrorCode code, Throwable cause, Object... args) {
      super(code, cause, args);
    }

    @Override
    protected String getResourceBundleBaseName() {
      return "com.percussion.utils.xml.PSXmlErrorStringBundle";
    }
  }

  private static final IPSErrorCode SAMPLE_CODE =
      new IPSErrorCode() {
        @Override
        public int numericCode() {
          return 8;
        }

        @Override
        public boolean isAuditable() {
          return true;
        }
      };

  @Test
  public void msgCodeCauseCtorInstallsCauseWithoutInitCause() {
    RuntimeException cause = new RuntimeException("root");
    TestBaseException ex = new TestBaseException(1, cause, "arg");
    assertSame(cause, ex.getCause());
    assertEquals(1, ex.getErrorCode());
  }

  @Test
  public void messageCauseCtorInstallsCauseAndArgs() {
    IllegalStateException cause = new IllegalStateException("boom");
    TestBaseException ex = new TestBaseException("hello", cause);
    assertSame(cause, ex.getCause());
    assertEquals(0, ex.getErrorCode());
  }

  @Test
  public void typedCtorRetainsCodeAndAuditability() {
    TestBaseException ex = new TestBaseException(SAMPLE_CODE, "arg");
    assertEquals(8, ex.getErrorCode());
    assertSame(SAMPLE_CODE, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());

    RuntimeException cause = new RuntimeException("root");
    TestBaseException withCause = new TestBaseException(SAMPLE_CODE, cause, "x");
    assertSame(cause, withCause.getCause());
    assertSame(SAMPLE_CODE, withCause.getTypedErrorCode());

    assertThrows(IllegalArgumentException.class, () -> new TestBaseException((IPSErrorCode) null));
    TestBaseException legacy = new TestBaseException(1, cause, "arg");
    assertFalse(legacy.isAuditable());
  }
}
