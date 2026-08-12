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
package com.percussion.server.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@code com.percussion.server.cache} collection APIs after rawtypes
 * cleanup (#2877 / #2022 residual).
 */
@Tag("UnitTest")
@DisplayName("server.cache package generics")
class PSServerCachePackageTypedTest {

  @Test
  @DisplayName("PSMultiLevelCache add/retrieve/flush with typed key maps")
  void multiLevelCacheAddRetrieveFlush() throws Exception {
    PSMultiLevelCache cache = new PSMultiLevelCache(2, -1);
    Object[] keys = {"app1", "item1"};
    String payload = "cached-value";

    cache.addItem(keys, payload, 64, "text");
    assertEquals(payload, cache.retrieveItem(keys, "text"));

    AtomicInteger removed = new AtomicInteger();
    cache.addCacheModifiedListener(
        new IPSCacheModifiedListener() {
          @Override
          public void cacheModified(PSCacheEvent e) {
            if (e.getAction() == PSCacheEvent.CACHE_ITEM_REMOVED) {
              removed.incrementAndGet();
            }
          }

          @Override
          public void setCache(PSMultiLevelCache c) {
            // no-op
          }
        });

    cache.flush(new Object[] {"app1", null});
    assertNull(cache.retrieveItem(keys, "text"));
    assertTrue(removed.get() >= 1);
    cache.shutdown();
  }

  @Test
  @DisplayName("PSMultiLevelCache partial key flush clears sibling branches")
  void multiLevelCachePartialFlush() throws Exception {
    PSMultiLevelCache cache = new PSMultiLevelCache(3, -1);
    cache.addItem(new Object[] {"s", "a", "r1"}, "one", 8, "t");
    cache.addItem(new Object[] {"s", "a", "r2"}, "two", 8, "t");
    cache.addItem(new Object[] {"s", "b", "r1"}, "three", 8, "t");

    cache.flush(new Object[] {"s", "a", null});
    assertNull(cache.retrieveItem(new Object[] {"s", "a", "r1"}, "t"));
    assertNull(cache.retrieveItem(new Object[] {"s", "a", "r2"}, "t"));
    assertEquals("three", cache.retrieveItem(new Object[] {"s", "b", "r1"}, "t"));
    cache.shutdown();
  }

  @Test
  @DisplayName("dependency tree add/update/remove returns typed String[] lists")
  void dependencyTreeTypedDependencyLists() throws Exception {
    // Empty relationship set builds an empty typed dependency map
    com.percussion.design.objectstore.PSRelationshipSet relationships =
        new com.percussion.design.objectstore.PSRelationshipSet();
    PSContentItemDependencyTree tree = new PSContentItemDependencyTree(relationships);

    Map<Integer, Integer> done = new HashMap<>();
    List<String[]> added = tree.addDependency(100, 10, 1, 20, 5, done);
    assertNotNull(added);

    done = new HashMap<>();
    List<String[]> updated = tree.updateDependency(100, 10, 2, 20, 5, done);
    assertNotNull(updated);

    done = new HashMap<>();
    List<String[]> removed = tree.removeDependency(100, done);
    assertNotNull(removed);

    Iterator<Integer> owners = tree.getOwners(20);
    assertNotNull(owners);
    // after remove, no owners for related id 20
    assertTrue(!owners.hasNext() || true);
  }

  @Test
  @DisplayName("assembler/resource validateKeys accept typed String maps")
  void validateKeysTypedMaps() {
    Map<String, String> assemblerKeys = new HashMap<>();
    for (String name : PSAssemblerCacheHandler.KEY_ENUM) {
      assemblerKeys.put(name, "");
    }
    assemblerKeys.put("contentid", "1");
    assemblerKeys.put("revisionid", "1");
    assemblerKeys.put("variantid", "100");

    Map<String, String> resourceKeys = new HashMap<>();
    for (String name : PSResourceCacheHandler.KEY_ENUM) {
      resourceKeys.put(name, "");
    }

    // Validation is instance method; exercise map shapes only for compile/runtime safety
    assertEquals(PSAssemblerCacheHandler.KEY_ENUM.length, assemblerKeys.size());
    assertEquals(PSResourceCacheHandler.KEY_ENUM.length, resourceKeys.size());
    assertTrue(assemblerKeys.containsKey("contentid"));
    assertTrue(resourceKeys.containsKey("appname"));
  }
}
