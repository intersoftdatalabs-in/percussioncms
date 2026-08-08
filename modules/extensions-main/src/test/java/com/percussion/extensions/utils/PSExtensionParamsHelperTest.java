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
package com.percussion.extensions.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.IPSRequestContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/** Behavioral unit tests for {@link PSExtensionParamsHelper} (javac warning cleanup #2029). */
public class PSExtensionParamsHelperTest {

  private static PSExtensionParamsHelper helperFromMap(Map<String, String> params) {
    return new PSExtensionParamsHelper(params, (IPSRequestContext) null, (Logger) null);
  }

  @Test
  public void mapConstructor_returnsExtensionParameters() {
    Map<String, String> params = new HashMap<>();
    params.put("foo", "bar");
    PSExtensionParamsHelper helper = helperFromMap(params);

    assertEquals("bar", helper.getParameter("foo"));
    assertNull(helper.getParameter("missing"));
    assertEquals("bar", helper.getRequiredParameter("foo"));
    assertEquals("fallback", helper.getOptionalParameter("missing", "fallback"));
  }

  @Test
  public void requiredParameter_missingThrows() {
    Map<String, String> params = new HashMap<>();
    PSExtensionParamsHelper helper = helperFromMap(params);

    assertThrows(IllegalArgumentException.class, () -> helper.getRequiredParameter("required"));
  }

  @Test
  public void paramToBoolean_acceptsTrueFalse() {
    Map<String, String> params = new HashMap<>();
    params.put("flag", "true");
    params.put("off", "false");
    PSExtensionParamsHelper helper = helperFromMap(params);

    assertTrue(helper.getRequiredParameterAsBoolean("flag"));
    assertFalse(helper.getRequiredParameterAsBoolean("off"));
    assertTrue(helper.paramToBoolean("x", "yes"));
    assertFalse(helper.paramToBoolean("x", "no"));
  }

  @Test
  public void paramToNumber_parsesInteger() {
    Map<String, String> params = new HashMap<>();
    params.put("count", "42");
    PSExtensionParamsHelper helper = helperFromMap(params);

    assertEquals(42, helper.getRequiredParameterAsNumber("count").intValue());
    assertEquals(7, helper.paramToNumber("n", "7").intValue());
  }

  @Test
  public void slotConstructor_rejectsNullArgsOrSelectors() {
    Map<String, Object> selectors = new HashMap<>();
    Map<String, Object> args = new HashMap<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> new PSExtensionParamsHelper((Map<String, ? extends Object>) null, selectors, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSExtensionParamsHelper(args, (Map<String, Object>) null, null));
  }

  @Test
  public void slotConstructor_prefersSelectorsOverArguments() {
    Map<String, Object> args = new HashMap<>();
    args.put("name", "fromArgs");
    Map<String, Object> selectors = new HashMap<>();
    selectors.put("name", "fromSelectors");

    PSExtensionParamsHelper helper = new PSExtensionParamsHelper(args, selectors, null);
    assertEquals("fromSelectors", helper.getParameter("name"));
  }
}
