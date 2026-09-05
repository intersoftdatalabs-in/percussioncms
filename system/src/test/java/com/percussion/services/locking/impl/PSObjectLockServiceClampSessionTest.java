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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.services.locking.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSObjectLockService#clampLockSession(String)}. */
public class PSObjectLockServiceClampSessionTest {

  @Test
  public void leavesShortSessionUnchanged() {
    assertEquals("abc", PSObjectLockService.clampLockSession("abc"));
    assertEquals(
        "x".repeat(PSObjectLockService.LOCK_SESSION_DB_MAX),
        PSObjectLockService.clampLockSession("x".repeat(PSObjectLockService.LOCK_SESSION_DB_MAX)));
  }

  @Test
  public void truncatesJettySizedSessionToColumnLimit() {
    String jettyId = "6cc9464256d7da4f1857aca7295e2548328f2e1997e6749c34f21a6085df344a";
    assertEquals(64, jettyId.length());
    String clamped = PSObjectLockService.clampLockSession(jettyId);
    assertEquals(PSObjectLockService.LOCK_SESSION_DB_MAX, clamped.length());
    assertEquals(jettyId.substring(0, 50), clamped);
  }

  @Test
  public void nullPassthrough() {
    assertNull(PSObjectLockService.clampLockSession(null));
  }
}
