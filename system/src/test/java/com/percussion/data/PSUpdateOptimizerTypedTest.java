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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSUpdateOptimizer} dependency helpers (cross-dependent table
 * detection) after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSUpdateOptimizerTypedTest {

  @Test
  void getCrossDependentTableDetectsMutualDependency() throws Exception {
    Method m =
        PSUpdateOptimizer.class.getDeclaredMethod(
            "getCrossDependentTable", String.class, Map.class, List.class);
    m.setAccessible(true);

    Map<String, List<String>> dependencyMap = new HashMap<>();
    dependencyMap.put("A", new ArrayList<>(List.of("B")));
    dependencyMap.put("B", new ArrayList<>(List.of("A")));

    String cross = (String) m.invoke(null, "A", dependencyMap, dependencyMap.get("A"));
    assertEquals("B", cross);
  }

  @Test
  void getCrossDependentTableReturnsNullWhenNoCycle() throws Exception {
    Method m =
        PSUpdateOptimizer.class.getDeclaredMethod(
            "getCrossDependentTable", String.class, Map.class, List.class);
    m.setAccessible(true);

    Map<String, List<String>> dependencyMap = new HashMap<>();
    dependencyMap.put("A", new ArrayList<>(List.of("B")));
    dependencyMap.put("B", new ArrayList<>(List.of("C")));

    Object cross = m.invoke(null, "A", dependencyMap, dependencyMap.get("A"));
    assertEquals(null, cross);
  }

  @Test
  void joinLoginAndExecutionPlansPreservesOrder() {
    List<IPSExecutionStep> logins = new ArrayList<>();
    List<IPSExecutionStep> exec = new ArrayList<>();
    IPSExecutionStep login =
        data -> {
          /* login */
        };
    IPSExecutionStep step =
        data -> {
          /* step */
        };
    logins.add(login);
    exec.add(step);

    IPSExecutionStep[] plan = PSOptimizer.joinLoginAndExecutionPlans(logins, exec);
    assertNotNull(plan);
    assertEquals(2, plan.length);
    assertSame(login, plan[0]);
    assertSame(step, plan[1]);
  }
}
