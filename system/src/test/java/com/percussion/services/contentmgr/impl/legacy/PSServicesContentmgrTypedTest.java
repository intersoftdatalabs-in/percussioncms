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
package com.percussion.services.contentmgr.impl.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for typed {@code com.percussion.services.contentmgr} query residual
 * (issue #3210 residual of #2022 after publisher batch #3188).
 */
@Tag("UnitTest")
@DisplayName("services.contentmgr package generics")
class PSServicesContentmgrTypedTest {

  @Test
  @DisplayName("asStringObjectMap copies keys as strings and skips null keys")
  void asStringObjectMapCopiesAndSkipsNullKeys() {
    Map<Object, Object> raw = new LinkedHashMap<>();
    raw.put("sys_contentid", 42);
    raw.put(IPSHtmlParameters.SYS_FOLDERID, 7);
    raw.put(null, "ignored");

    Map<String, Object> typed = PSContentRepository.asStringObjectMap(raw);
    assertEquals(42, typed.get("sys_contentid"));
    assertEquals(7, typed.get(IPSHtmlParameters.SYS_FOLDERID));
    assertEquals(2, typed.size());
    assertTrue(typed.containsKey(IPSHtmlParameters.SYS_FOLDERID));
  }

  @Test
  @DisplayName("asStringObjectMap returns empty map for null input")
  void asStringObjectMapNullSafe() {
    Map<String, Object> typed = PSContentRepository.asStringObjectMap(null);
    assertTrue(typed.isEmpty());
    typed.put("x", 1);
    assertNull(PSContentRepository.asStringObjectMap(null).get("x"));
  }

  @Test
  @DisplayName("asStringObjectMap does not alias the source map")
  void asStringObjectMapDefensiveCopy() {
    Map<Object, Object> raw = new HashMap<>();
    raw.put("k", "v");
    Map<String, Object> typed = PSContentRepository.asStringObjectMap(raw);
    typed.put("k", "changed");
    assertEquals("v", raw.get("k"));
  }

  @Test
  @DisplayName("remapFolderId moves sys_folderid to rx:sys_folderid")
  void remapFolderIdMovesLegacyKey() {
    Map<Object, Object> raw = new LinkedHashMap<>();
    raw.put(IPSHtmlParameters.SYS_FOLDERID, 99);
    raw.put("rx:sys_contentid", 5);

    Map<String, Object> remapped = PSContentRepository.remapFolderId(raw);
    assertNull(remapped.get(IPSHtmlParameters.SYS_FOLDERID));
    assertEquals(99, remapped.get(IPSContentPropertyConstants.RX_SYS_FOLDERID));
    assertEquals(5, remapped.get("rx:sys_contentid"));
  }

  @Test
  @DisplayName("remapFolderId leaves rows without folder id unchanged")
  void remapFolderIdNoFolder() {
    Map<Object, Object> raw = new HashMap<>();
    raw.put("rx:sys_contentid", 8);
    Map<String, Object> remapped = PSContentRepository.remapFolderId(raw);
    assertEquals(8, remapped.get("rx:sys_contentid"));
    assertNull(remapped.get(IPSContentPropertyConstants.RX_SYS_FOLDERID));
  }
}
