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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.UnexpectedRollbackException;

/** pageChanged reload failures must not look like a failed page create (#4874). */
class PSPageChangeHandlerRollbackTest {

  @Test
  void unexpectedRollbackIsSilent() {
    assertTrue(
        PSPageChangeHandler.isSilentRollback(
            new UnexpectedRollbackException(
                "Transaction silently rolled back because it has been marked as rollback-only")));
    assertTrue(
        PSPageChangeHandler.isSilentRollback(
            new RuntimeException(
                "wrapped",
                new UnexpectedRollbackException(
                    "Transaction silently rolled back because it has been marked as rollback-only"))));
  }

  @Test
  void otherFailuresStayLoud() {
    assertFalse(PSPageChangeHandler.isSilentRollback(new IllegalStateException("missing page")));
    assertFalse(PSPageChangeHandler.isSilentRollback(new IllegalStateException("rollback-only")));
    assertFalse(PSPageChangeHandler.isSilentRollback(null));
  }
}
