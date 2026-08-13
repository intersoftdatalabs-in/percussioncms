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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the H2 qa-up {@code PK_PSX_OBJECTACL} / SYSID 1001 collision (#3282) and proves the
 * aligner yields a free id.
 */
@Tag("UnitTest")
class PSNextNumberAlignerTest {

  /** cmsTableData NEXTNUMBER.PSX_OBJECTACL before the #3282 seed bump. */
  private static final int SEEDED_NEXTNR = 1000;

  /** Highest SYSID in cmsTableData PSX_OBJECTACL (Everyone / admin1 on 301–303). */
  private static final int SEEDED_MAX_SYSID = 1006;

  @Test
  @DisplayName("NEXTNR=1000 allocates 1001 which is already Everyone on CONTENTID=301")
  void seedNextNumberCollidesWithEveryoneSysid1001() {
    int next = PSNextNumberAligner.firstAllocatedId(SEEDED_NEXTNR);
    assertEquals(1001, next);
    assertTrue(
        PSNextNumberAligner.wouldCollide(next, SEEDED_MAX_SYSID),
        "allocator 1001 must collide with seed SYSID 1001–1006");
  }

  @Test
  @DisplayName("aligned next id is past every seed ACL SYSID")
  void nextFreeIdSkipsSeededObjectAclRange() {
    int next = PSNextNumberAligner.firstAllocatedId(SEEDED_NEXTNR);
    int free = PSNextNumberAligner.nextFreeId(next, SEEDED_MAX_SYSID);
    assertEquals(1007, free);
    assertFalse(PSNextNumberAligner.wouldCollide(free, SEEDED_MAX_SYSID));
  }

  @Test
  void emptyTableLeavesCandidateUnchanged() {
    assertEquals(1001, PSNextNumberAligner.nextFreeId(1001, -1));
    assertFalse(PSNextNumberAligner.wouldCollide(1001, -1));
  }

  @Test
  void candidateAlreadyPastHighWaterIsUnchanged() {
    assertEquals(2001, PSNextNumberAligner.nextFreeId(2001, 1006));
    assertFalse(PSNextNumberAligner.wouldCollide(2001, 1006));
  }
}
