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
package com.percussion.searchmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** GH-3617: Explorer root {@code /} / {@code //} must not be a search folder filter. */
class PSSearchFolderScopeTest {

  @Test
  void rootAndBlankAreUnscoped() {
    assertNull(PSSearchFolderScope.forSearch(null));
    assertNull(PSSearchFolderScope.forSearch(""));
    assertNull(PSSearchFolderScope.forSearch("   "));
    assertNull(PSSearchFolderScope.forSearch("/"));
    assertNull(PSSearchFolderScope.forSearch("//"));
    assertNull(PSSearchFolderScope.forSearch("///"));
    assertNull(PSSearchFolderScope.forSearch(" // "));
  }

  @Test
  void realFoldersAreKept() {
    assertEquals("//Sites", PSSearchFolderScope.forSearch("//Sites"));
    assertEquals("/Sites", PSSearchFolderScope.forSearch("/Sites"));
    assertEquals("//Sites/Demo", PSSearchFolderScope.forSearch("//Sites/Demo"));
    assertEquals("//Folders/$System$/Assets", PSSearchFolderScope.forSearch("//Folders/$System$/Assets"));
  }
}
