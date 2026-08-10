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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSSearch#parseParameters(String, Map)} (rawtypes/unchecked
 * cleanup for issue #2311).
 */
public class PSSearchParseParametersTest {

  @Test
  public void nullUrlReturnsEmptyMapWhenParamsNull() {
    Map<String, String> result = PSSearch.parseParameters(null, null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void nullUrlReturnsSameMapWhenSupplied() {
    Map<String, String> params = new HashMap<>();
    params.put("keep", "me");
    Map<String, String> result = PSSearch.parseParameters(null, params);
    assertSame(params, result);
    assertEquals("me", result.get("keep"));
  }

  @Test
  public void emptyUrlOrNoQueryLeavesMapEmpty() {
    assertTrue(PSSearch.parseParameters("", null).isEmpty());
    assertTrue(PSSearch.parseParameters("/app/resource.xml", null).isEmpty());
  }

  @Test
  public void parsesQueryPairsIntoTypedMap() {
    Map<String, String> result =
        PSSearch.parseParameters("/sys/app.xml?sys_contentid=10&sys_revision=1", null);
    assertEquals(2, result.size());
    assertEquals("10", result.get("sys_contentid"));
    assertEquals("1", result.get("sys_revision"));
  }

  @Test
  public void mergesIntoSuppliedMapAndSkipsTokensWithoutEquals() {
    Map<String, String> params = new HashMap<>();
    params.put("existing", "value");
    Map<String, String> result = PSSearch.parseParameters("x?a=1&bare&b=two", params);
    assertSame(params, result);
    assertEquals("value", result.get("existing"));
    assertEquals("1", result.get("a"));
    assertEquals("two", result.get("b"));
    assertEquals(3, result.size());
  }
}
