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

package com.percussion.rest.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link FolderNotFoundException} after the this-escape real fix (cause
 * attached via {@link RestExceptionBase}'s cause-aware constructor).
 */
class FolderNotFoundExceptionTest {

  @Test
  void noArgConstructorSetsFolderNotFoundCodeAndDefaultStatus() {
    FolderNotFoundException ex = new FolderNotFoundException();

    assertEquals(RestErrorCode.FOLDER_NOT_FOUND, ex.getErrorCode());
    assertEquals(Status.INTERNAL_SERVER_ERROR, ex.getStatus());
    assertNull(ex.getCause());
  }

  @Test
  void causeConstructorPreservesCauseAndErrorCode() {
    RuntimeException cause = new RuntimeException("missing folder");
    FolderNotFoundException ex = new FolderNotFoundException(cause);

    assertEquals(RestErrorCode.FOLDER_NOT_FOUND, ex.getErrorCode());
    assertSame(cause, ex.getCause());
  }
}
