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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed expanded-path option APIs used by the nav tree. */
public class PSExpandedOptionTest {

  @Test
  public void constructorAndGetPathsPreserveTypedPaths() {
    PSExpandedOption option =
        new PSExpandedOption(Arrays.asList("//Sites/Home", "//Sites/About"));
    Set<String> paths = option.getPaths();
    assertEquals(2, paths.size());
    assertTrue(paths.contains("//Sites/Home"));
    assertTrue(paths.contains("//Sites/About"));
  }

  @Test
  public void setPathsAddsPathsToExistingSet() {
    PSExpandedOption option = new PSExpandedOption(Arrays.asList("//a"));
    option.setPaths(Arrays.asList("//b", "//c"));
    Set<String> paths = option.getPaths();
    assertEquals(3, paths.size());
    assertTrue(paths.contains("//a"));
    assertTrue(paths.contains("//b"));
    assertTrue(paths.contains("//c"));
  }

  @Test
  public void setPathsRejectsNull() {
    PSExpandedOption option = new PSExpandedOption(Arrays.asList("//a"));
    assertThrows(IllegalArgumentException.class, () -> option.setPaths(null));
  }
}
