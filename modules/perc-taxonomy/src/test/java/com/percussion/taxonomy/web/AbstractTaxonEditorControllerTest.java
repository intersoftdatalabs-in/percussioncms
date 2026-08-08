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
package com.percussion.taxonomy.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed helpers on {@link AbstractTaxonEditorController}. */
public class AbstractTaxonEditorControllerTest {

  @Test
  public void collectionToHashmap_identityKeys() {
    HashMap<String, String> map =
        AbstractTaxonEditorController.collection_to_hashmap(Arrays.asList("a", "b", "c"));
    assertEquals(3, map.size());
    assertEquals("a", map.get("a"));
    assertEquals("b", map.get("b"));
    assertEquals("c", map.get("c"));
  }

  @Test
  public void collectionToHashmap_empty() {
    HashMap<Integer, Integer> map =
        AbstractTaxonEditorController.collection_to_hashmap(Arrays.asList());
    assertTrue(map.isEmpty());
  }
}
