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
package com.percussion.services.assembly.jexl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.util.PSStopwatch;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** JEXL residual typing for combine() / percTimers (#3280). */
@Tag("UnitTest")
class PSAssemblerUtilsTypedTest {

  private final PSAssemblerUtils utils = new PSAssemblerUtils();

  @Test
  @DisplayName("combine(urlquery) collects repeated keys as String[] without unchecked List casts")
  void combineUrlQueryRepeatedKeys() {
    Map<String, String[]> input = new HashMap<>();
    input.put("keep", new String[] {"orig"});
    Map<String, Object> combined = utils.combine(input, "a=1&a=2&b=x");

    assertArrayEquals(new String[] {"orig"}, (String[]) combined.get("keep"));
    assertArrayEquals(new String[] {"1", "2"}, (String[]) combined.get("a"));
    assertArrayEquals(new String[] {"x"}, (String[]) combined.get("b"));
  }

  @Test
  @DisplayName("combine(urlquery) rejects malformed pairs")
  void combineUrlQueryMalformed() {
    assertThrows(
        IllegalArgumentException.class, () -> utils.combine(Map.of(), "ok=1&badpair"));
  }

  @Test
  @DisplayName("copyTimers keeps PSStopwatch entries and skips others")
  void copyTimersTyped() {
    PSStopwatch watch = new PSStopwatch();
    Map<Object, Object> raw = new HashMap<>();
    raw.put("render", watch);
    raw.put(1, watch);
    raw.put("other", "nope");

    HashMap<String, PSStopwatch> typed = PSAssemblerUtils.copyTimers(raw);
    assertEquals(1, typed.size());
    assertEquals(watch, typed.get("render"));
    assertTrue(PSAssemblerUtils.copyTimers(null).isEmpty());
  }

  @Test
  @DisplayName("timerStart writes percTimers onto the live $sys map")
  void timerStartWritesThroughSys() {
    IPSAssemblyItem item = mock(IPSAssemblyItem.class);
    Map<String, Object> sys = new HashMap<>();
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);
    when(item.getBindings()).thenReturn(bindings);

    utils.timerStart(item, "assemble");
    Object percTimers = sys.get("percTimers");
    assertTrue(percTimers instanceof Map<?, ?>);
    assertTrue(((Map<?, ?>) percTimers).containsKey("assemble"));

    utils.timerStop(item, "assemble");
    assertTrue(utils.timerElapsed(item, "assemble") >= 0.0);
    utils.timerReset(item);
    assertTrue(((Map<?, ?>) sys.get("percTimers")).isEmpty());
  }
}
