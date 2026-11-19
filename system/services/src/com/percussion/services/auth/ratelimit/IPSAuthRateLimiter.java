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
package com.percussion.services.auth.ratelimit;

import java.time.Duration;

/** In-memory token-bucket rate limiter for authentication, recovery, and email endpoints. */
public interface IPSAuthRateLimiter {

  /**
   * Attempts to consume one token for {@code (namespace, key)} within {@code capacity} per {@code
   * refillPeriod}.
   *
   * @return {@code true} if allowed, {@code false} if rate-limited.
   */
  boolean tryConsume(String namespace, String key, long capacity, Duration refillPeriod);
}
