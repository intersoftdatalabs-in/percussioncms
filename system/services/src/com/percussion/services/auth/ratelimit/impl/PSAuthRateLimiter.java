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

import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.system.utils.PSBaseBean;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;

/** Bucket4j-backed per-key token bucket rate limiter with bounded map eviction. */
@PSBaseBean("sys_authRateLimiter")
public class PSAuthRateLimiter implements IPSAuthRateLimiter {

  static final int DEFAULT_MAX_ENTRIES = 10_000;
  static final Duration DEFAULT_STALE_AGE = Duration.ofHours(2);

  private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maxEntries;
  private final Duration staleAge;

  public PSAuthRateLimiter() {
    this(Clock.systemUTC(), DEFAULT_MAX_ENTRIES, DEFAULT_STALE_AGE);
  }

  /** Package-visible constructor for deterministic unit tests. */
  PSAuthRateLimiter(Clock clock, int maxEntries, Duration staleAge) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.maxEntries = Math.max(16, maxEntries);
    this.staleAge = Objects.requireNonNull(staleAge, "staleAge");
  }

  @Override
  public boolean tryConsume(String namespace, String key, long capacity, Duration refillPeriod) {
    long safeCapacity = Math.max(1L, capacity);
    Duration safePeriod =
        (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative())
            ? Duration.ofHours(1)
            : refillPeriod;
    String compositeKey = buildCompositeKey(namespace, key, safeCapacity, safePeriod);
    Instant now = clock.instant();

    evictIfNeeded(now);
    BucketEntry entry =
        buckets.compute(
            compositeKey,
            (k, existing) ->
                existing != null ? existing.touch(now) : newBucketEntry(safeCapacity, safePeriod, now));
    return entry.bucket().tryConsume(1L);
  }

  private static String buildCompositeKey(
      String namespace, String key, long capacity, Duration period) {
    String ns = StringUtils.defaultIfBlank(namespace, "default").trim().toLowerCase(Locale.ROOT);
    String normKey = StringUtils.defaultIfBlank(key, "_unknown").trim().toLowerCase(Locale.ROOT);
    return ns + "|" + normKey + "|" + capacity + "|" + period.toMillis();
  }

  private static BucketEntry newBucketEntry(long capacity, Duration period, Instant now) {
    Bandwidth limit = Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build();
    Bucket bucket = Bucket.builder().addLimit(limit).build();
    return new BucketEntry(bucket, now);
  }

  private void evictIfNeeded(Instant now) {
    if (buckets.size() < maxEntries) {
      return;
    }
    Instant cutoff = now.minus(staleAge);
    Iterator<Map.Entry<String, BucketEntry>> it = buckets.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, BucketEntry> e = it.next();
      if (e.getValue().lastAccess().isBefore(cutoff)) {
        it.remove();
      }
    }
    if (buckets.size() < maxEntries) {
      return;
    }
    int excess = buckets.size() - maxEntries + 1;
    Iterator<String> keyIt = buckets.keySet().iterator();
    while (excess > 0 && keyIt.hasNext()) {
      keyIt.next();
      keyIt.remove();
      excess--;
    }
  }

  private record BucketEntry(Bucket bucket, Instant lastAccess) {
    BucketEntry touch(Instant now) {
      return new BucketEntry(bucket, now);
    }
  }
}
