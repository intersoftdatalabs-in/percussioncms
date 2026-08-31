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
package com.percussion.services.filter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.services.filter.impl.PSFilterManager.FilterNameLookup;
import org.junit.jupiter.api.Test;

/**
 * Lookup classification for {@link PSFilterManager#findFiltersByName(String)}.
 * A miss on an exact name must not list the full catalog (AS-07 create 409).
 */
class PSFilterManagerFindByNameTest {

  @Test
  void blankAndPercentListAll() {
    assertEquals(FilterNameLookup.ALL, PSFilterManager.classifyFilterNameLookup(null));
    assertEquals(FilterNameLookup.ALL, PSFilterManager.classifyFilterNameLookup(""));
    assertEquals(FilterNameLookup.ALL, PSFilterManager.classifyFilterNameLookup("   "));
    assertEquals(FilterNameLookup.ALL, PSFilterManager.classifyFilterNameLookup("%"));
  }

  @Test
  void percentPatternUsesLike() {
    assertEquals(FilterNameLookup.LIKE, PSFilterManager.classifyFilterNameLookup("Test%"));
    assertEquals(FilterNameLookup.LIKE, PSFilterManager.classifyFilterNameLookup("%preview%"));
  }

  @Test
  void concreteNameIsExact() {
    assertEquals(FilterNameLookup.EXACT, PSFilterManager.classifyFilterNameLookup("preview"));
    assertEquals(FilterNameLookup.EXACT, PSFilterManager.classifyFilterNameLookup("qa4060abc"));
    assertEquals(FilterNameLookup.EXACT, PSFilterManager.classifyFilterNameLookup("has_underscore"));
  }
}
