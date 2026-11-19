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
package com.percussion.services.auth.ratelimit.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSAuthRateLimiterTest {

  @Test
  void enforcesCapacityPerKeyAndIsolatesNamespacesAndKeys() {
    PSAuthRateLimiter limiter = new PSAuthRateLimiter();
    Duration window = Duration.ofHours(1);

    assertTrue(limiter.tryConsume("forgot_username_ip", "203.0.113.10", 2, window));
    assertTrue(limiter.tryConsume("forgot_username_ip", "203.0.113.10", 2, window));
    assertFalse(limiter.tryConsume("forgot_username_ip", "203.0.113.10", 2, window));

    // Different key in same namespace and different namespace for same key are unaffected
    assertTrue(limiter.tryConsume("forgot_username_ip", "203.0.113.11", 2, window));
    assertTrue(limiter.tryConsume("other_ns", "203.0.113.10", 2, window));
  }

  @Test
  void normalizesBlankKeysAndEvictsWhenBoundedMaxExceeded() {
    AtomicReference<Instant> nowRef = new AtomicReference<>(Instant.parse("2026-11-18T00:00:00Z"));
    Clock fakeClock =
        new Clock() {
          @Override
          public ZoneOffset getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return nowRef.get();
          }
        };

    PSAuthRateLimiter limiter = new PSAuthRateLimiter(fakeClock, 16, Duration.ofMinutes(10));
    assertTrue(limiter.tryConsume("forgot_username_ip", "   ", 1, Duration.ofMinutes(5)));
    assertFalse(limiter.tryConsume("forgot_username_ip", null, 1, Duration.ofMinutes(5)));

    for (int i = 0; i < 20; i++) {
      nowRef.set(nowRef.get().plusSeconds(1));
      assertTrue(limiter.tryConsume("burst", "ip-" + i, 5, Duration.ofMinutes(5)));
    }
  }
}
