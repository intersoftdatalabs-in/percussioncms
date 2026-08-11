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
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Override
    protected String getResourceBundleBaseName() {
      return "com.percussion.utils.xml.PSXmlErrorStringBundle";
    }
  }

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
}
