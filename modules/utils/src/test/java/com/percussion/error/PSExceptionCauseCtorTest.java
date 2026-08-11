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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for the message+cause constructor after this-escape strip (issue #2969).
 */
@Tag("UnitTest")
public class PSExceptionCauseCtorTest {

  @Test
  public void messageCauseCtorSetsCodeZeroAndCause() {
    RuntimeException cause = new RuntimeException("root");
    PSException ex = new PSException("wrapped", cause);
    assertEquals(0, ex.getErrorCode());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void messageCauseCtorRejectsNullCause() {
    assertThrows(IllegalArgumentException.class, () -> new PSException("msg", null));
  }
}
