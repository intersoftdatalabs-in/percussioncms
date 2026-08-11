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
package com.percussion.services.guidmgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.services.catalog.PSTypeEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSGuidTest {

  @Test
  public void assembleFromHostTypeUuid() {
    PSGuid guid = new PSGuid(1001L, PSTypeEnum.INTERNAL, 42L);
    assertEquals(1001L, guid.getHostId());
    assertEquals(PSTypeEnum.INTERNAL.getOrdinal(), guid.getType());
    assertEquals(42, guid.getUUID());
    assertEquals("1001-0-42", guid.toString());
  }

  @Test
  public void stringCtorWithType() {
    PSGuid guid = new PSGuid(PSTypeEnum.INTERNAL, "1001-0-7");
    assertEquals(1001L, guid.getHostId());
    assertEquals(7, guid.getUUID());
  }

  @Test
  public void typeMismatchRejected() {
    // INTERNAL ordinal is 0 (treated as "unset"); use a non-zero type so mismatch is detectable
    PSGuid typed = new PSGuid(5L, PSTypeEnum.NODEDEF, 1L);
    assertThrows(
        IllegalArgumentException.class, () -> new PSGuid(PSTypeEnum.TEMPLATE, typed.longValue()));
  }
}
