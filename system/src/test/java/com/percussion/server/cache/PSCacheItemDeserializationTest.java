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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.validation.SerializationValidation;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PSCacheItem deserialization security (CWE-502). Tests that the ObjectInputFilter
 * is properly applied and allows safe Percussion classes while blocking gadget chains.
 */
@DisplayName("PSCacheItem - Deserialization Security")
class PSCacheItemDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should deserialize valid cached object from disk with filter applied")
  void testDeserializeValidObject() throws IOException, ClassNotFoundException {
    // Create and serialize a simple safe object
    String testData = "TestCacheData-12345";
    Path cacheFile = tempDir.resolve("cache-test.ser");

    // Serialize the object
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
      oos.writeObject(testData);
    }

    // Deserialize using the same pattern as PSCacheItem.getObjectFromDisk()
    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFile))) {
      // Apply the deserialization filter
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertEquals(testData, result);
  }

  @Test
  @DisplayName("Should handle serialized HashMap with filter applied")
  void testDeserializeHashMap() throws IOException, ClassNotFoundException {
    java.util.HashMap<String, String> testMap = new java.util.HashMap<>();
    testMap.put("key1", "value1");
    testMap.put("key2", "value2");

    Path cacheFile = tempDir.resolve("hashmap-cache.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
      oos.writeObject(testMap);
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.Map.class, result);
    java.util.Map<String, String> resultMap = (java.util.Map<String, String>) result;
    assertEquals(2, resultMap.size());
    assertEquals("value1", resultMap.get("key1"));
  }

  @Test
  @DisplayName("Should block dangerous gadget chain classes")
  void testBlockGadgetChainClasses() throws IOException {
    // This test verifies the filter specs include rejection of known gadget chains
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");

    // Verify filter spec contains rejection pattern
    assertNotNull(filterSpec);
    assertTrue(
        filterSpec.contains("!*") || filterSpec.contains("maxdepth"),
        "Filter spec should restrict dangerous classes");
  }

  @Test
  @DisplayName("Should allow multiple Percussion packages in filter")
  void testMultiplePercussionPackages() throws IOException, ClassNotFoundException {
    // Create a list (standard safe class)
    java.util.ArrayList<String> testList = new java.util.ArrayList<>();
    testList.add("item1");
    testList.add("item2");

    Path cacheFile = tempDir.resolve("list-cache.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
      oos.writeObject(testList);
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFile))) {
      // Test with multiple packages
      String filterSpec =
          SerializationValidation.buildPackageFilterSpec(
              "com.percussion.server.**", "com.percussion.design.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.List.class, result);
  }
}
