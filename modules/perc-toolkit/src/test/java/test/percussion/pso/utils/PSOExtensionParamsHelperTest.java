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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.server.IPSRequestContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/** Coverage for map/slot constructors and parameter resolution after private init helpers. */
public class PSOExtensionParamsHelperTest {

  private static PSOExtensionParamsHelper mapHelper(Map<String, String> params) {
    return new PSOExtensionParamsHelper(params, (IPSRequestContext) null, (Logger) null);
  }

  @Test
  void mapConstructorReturnsExtensionParameters() {
    Map<String, String> params = new HashMap<>();
    params.put("sys_contentid", "42");
    PSOExtensionParamsHelper helper = mapHelper(params);
    assertEquals("42", helper.getParameter("sys_contentid"));
    assertEquals("42", helper.getRequiredParameter("sys_contentid"));
  }

  @Test
  void optionalParameterFallsBackToDefault() {
    PSOExtensionParamsHelper helper = mapHelper(Map.of());
    assertEquals("fallback", helper.getOptionalParameter("missing", "fallback"));
  }

  @Test
  void requiredParameterThrowsWhenMissing() {
    PSOExtensionParamsHelper helper = mapHelper(Map.of());
    assertThrows(IllegalArgumentException.class, () -> helper.getRequiredParameter("nope"));
  }

  @Test
  void slotConstructorReadsSelectorsFirst() {
    Map<String, Object> args = Map.of("fromArgs", "a");
    Map<String, Object> selectors = new HashMap<>();
    selectors.put("fromArgs", "selectorWins");
    selectors.put("onlySelector", "s");
    PSOExtensionParamsHelper helper =
        new PSOExtensionParamsHelper(args, selectors, (Logger) null);
    assertEquals("selectorWins", helper.getParameter("fromArgs"));
    assertEquals("s", helper.getParameter("onlySelector"));
  }

  @Test
  void paramToNumberAndBoolean() {
    PSOExtensionParamsHelper helper = mapHelper(Map.of("n", "7", "b", "true"));
    assertEquals(7, helper.getRequiredParameterAsNumber("n").intValue());
    assertTrue(helper.getRequiredParameterAsBoolean("b"));
  }
}
