/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
package com.percussion.services.content.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import org.junit.jupiter.api.Test;

class PSAutoTranslationSetGuidTest {

  @Test
  void dummySetGuidIsRecognized() {
    assertTrue(
        PSAutoTranslation.isAutoTranslationsSetGuid(PSAutoTranslation.getAutoTranslationsGUID()));
    assertFalse(PSAutoTranslation.isAutoTranslationsSetGuid(null));
    assertFalse(
        PSAutoTranslation.isAutoTranslationsSetGuid(new PSGuid(0, PSTypeEnum.NODEDEF, 1033)));
  }

  @Test
  void persistentContentTypeIdStripsTypeBits() {
    assertEquals(1033L, PSAutoTranslation.persistentContentTypeId(1033L));
    long typed = new PSGuid(0, PSTypeEnum.NODEDEF, 1033).longValue();
    assertEquals(1033L, PSAutoTranslation.persistentContentTypeId(typed));
  }

  @Test
  void persistentKeyMatchesUuidAndTypedLong() {
    PSAutoTranslation raw = new PSAutoTranslation();
    raw.setContentTypeId(1033L);
    raw.setLocale("ar");
    PSAutoTranslation typed = new PSAutoTranslation();
    typed.setContentTypeId(new PSGuid(0, PSTypeEnum.NODEDEF, 1033).longValue());
    typed.setLocale("ar");
    assertEquals(raw.getPersistentKey(), typed.getPersistentKey());
  }
}
