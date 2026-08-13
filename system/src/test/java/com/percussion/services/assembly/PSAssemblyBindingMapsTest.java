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
package com.percussion.services.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Typed JEXL / assembly binding map helpers (#3280). */
@Tag("UnitTest")
class PSAssemblyBindingMapsTest {

  @Test
  @DisplayName("copyStringObjectMap copies string keys and skips others")
  void copySkipsNonStringKeys() {
    Map<Object, Object> raw = new HashMap<>();
    raw.put("k", "v");
    raw.put(1, "num");
    raw.put(null, "n");

    Map<String, Object> typed = PSAssemblyBindingMaps.copyStringObjectMap(raw);
    assertEquals(1, typed.size());
    assertEquals("v", typed.get("k"));
    typed.put("k2", "v2");
    assertNull(raw.get("k2"));
  }

  @Test
  @DisplayName("copyStringObjectMap returns null for non-maps")
  void copyNullForNonMap() {
    assertNull(PSAssemblyBindingMaps.copyStringObjectMap(null));
    assertNull(PSAssemblyBindingMaps.copyStringObjectMap("x"));
  }

  @Test
  @DisplayName("liveStringObjectMap writes through to the original $sys map")
  void liveWritesThrough() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("mimetype", "image/png");
    Map<String, Object> live = PSAssemblyBindingMaps.liveStringObjectMap(sys);
    live.put("binary", new byte[] {1});
    assertTrue(sys.get("binary") instanceof byte[]);
    assertEquals("image/png", live.get("mimetype"));
  }

  @Test
  @DisplayName("sysNestedMap returns live $sys.metadata")
  void sysNestedMetadata() {
    Map<String, Object> meta = new HashMap<>();
    meta.put("k", "v");
    Map<String, Object> sys = new HashMap<>();
    sys.put("metadata", meta);
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);

    Map<String, Object> nested = PSAssemblyBindingMaps.sysNestedMap(bindings, "metadata");
    nested.put("type", "page");
    assertEquals("page", meta.get("type"));
    assertSame(meta.get("k"), nested.get("k"));
  }
}
