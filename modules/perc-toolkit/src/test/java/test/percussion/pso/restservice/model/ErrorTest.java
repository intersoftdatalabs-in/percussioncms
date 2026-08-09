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
package test.percussion.pso.restservice.model;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.restservice.model.Error;
import com.percussion.pso.restservice.model.Error.ErrorCode;
import org.junit.jupiter.api.Test;

/** Constructor field-assignment coverage for {@link Error} (this-escape free ctors). */
public class ErrorTest {

  @Test
  void defaultConstructorUsesUnknownError() {
    Error e = new Error();
    assertEquals(ErrorCode.UNKNOWN_ERROR, e.getErrorCode());
    assertNull(e.getErrorMessage());
    assertNull(e.getContentId());
  }

  @Test
  void codeAndMessageConstructor() {
    Error e = new Error(ErrorCode.NOT_FOUND, "missing");
    assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
    assertEquals("missing", e.getErrorMessage());
  }

  @Test
  void codeContentIdAndMessageConstructor() {
    Error e = new Error(ErrorCode.ASSEMBLY_ERROR, 99, "boom");
    assertEquals(ErrorCode.ASSEMBLY_ERROR, e.getErrorCode());
    assertEquals(Integer.valueOf(99), e.getContentId());
    assertEquals("boom", e.getErrorMessage());
  }

  @Test
  void codeOnlyConstructor() {
    Error e = new Error(ErrorCode.SKIP);
    assertEquals(ErrorCode.SKIP, e.getErrorCode());
  }
}
