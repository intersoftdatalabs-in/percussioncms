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
package com.percussion.data;

/**
 * Pure NEXTNUMBER / SYSID arithmetic used when seed rows occupy ids that the allocator would
 * otherwise emit.
 *
 * <p>{@code PSGuidManager.updateNextNumber} treats {@code NEXTNUMBER.NEXTNR} as the last issued
 * value and returns {@code NEXTNR + 1}. Seed {@code NEXTNR=1000} therefore allocates {@code 1001},
 * which collides with FastForward / cmsTableData {@code PSX_OBJECTACL.SYSID=1001} (Everyone on
 * CONTENTID=301) — H2 {@code 23505} / {@code PK_PSX_OBJECTACL} on folder save (#3282).
 */
public final class PSNextNumberAligner {

  private PSNextNumberAligner() {}

  /**
   * First id {@link com.percussion.services.guidmgr.IPSGuidManager#createId(String)} issues for the
   * given stored {@code NEXTNR}.
   */
  public static int firstAllocatedId(int storedNextNr) {
    return storedNextNr + 1;
  }

  /**
   * {@code true} when the next allocated id is already present as {@code maxUsedId} or below (dense
   * or gapped tables — we never reuse below the high-water mark).
   */
  public static boolean wouldCollide(int nextCandidate, int maxUsedId) {
    return maxUsedId >= nextCandidate;
  }

  /**
   * Smallest id strictly greater than every used id, and not less than {@code nextCandidate}.
   *
   * @param nextCandidate next id the allocator would issue
   * @param maxUsedId highest id already persisted, or {@code < 0} if the table is empty
   */
  public static int nextFreeId(int nextCandidate, int maxUsedId) {
    if (maxUsedId < 0) {
      return nextCandidate;
    }
    return Math.max(nextCandidate, maxUsedId + 1);
  }
}
