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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the pure session-state helpers extracted from
 * {@link PSCESessionManager#run()} (issue #5774). Headless-safe: covers only the static,
 * UI-free decision logic — no applet, dialog, or scheduler is touched.
 */
public class PSCESessionManagerTest {

  @Test
  public void expiredWhenNoTimeRemains() {
    assertTrue(PSCESessionManager.isExpired(0));
    assertTrue(PSCESessionManager.isExpired(-1));
    assertFalse(PSCESessionManager.isExpired(1));
  }

  @Test
  public void warningOnlyInsideWarningWindow() {
    assertTrue(PSCESessionManager.isWarningState(1_000, 60_000));
    assertTrue(PSCESessionManager.isWarningState(60_000, 60_000));
    assertFalse(PSCESessionManager.isWarningState(61_000, 60_000));
    assertFalse(PSCESessionManager.isWarningState(0, 60_000));
    assertFalse(PSCESessionManager.isWarningState(1_000, 0));
  }

  @Test
  public void sessionCheckResourceRequestsExtensionOnlyWhenBanked() {
    assertEquals(
        "/Rhythmyx/sessioncheck", PSCESessionManager.sessionCheckResource(false));
    assertEquals(
        "/Rhythmyx/sessioncheck?extendSession=true",
        PSCESessionManager.sessionCheckResource(true));
  }

  @Test
  public void fallbackStatusContinuesPreviousCountdown() {
    long now = 1_000_000L;
    PSCESessionManager.SessionStatus status =
        PSCESessionManager.fallbackStatus(now + 5_000L, now + 60_000L, now);

    assertEquals(5_000L, status.expiry());
    assertEquals(60_000L, status.warning());
  }
}
