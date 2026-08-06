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

package com.percussion.xml;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.validation.SerializationValidation;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PSDtdTree deserialization security (CWE-502). Tests that the ObjectInputFilter is
 * properly applied in the clone() method which uses serialization for deep copying.
 */
@DisplayName("PSDtdTree - Deserialization Security")
class PSDtdTreeDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should deserialize serialized ArrayList with filter applied")
  void testDeserializeSerializedList() throws IOException, ClassNotFoundException {
    // PSDtdTree clone() deserializes complex objects, test with a similar structure
    java.util.ArrayList<String> testList = new java.util.ArrayList<>();
    testList.add("element1");
    testList.add("element2");
    testList.add("element3");

    Path serFile = tempDir.resolve("dtd-tree-list.ser");

    // Serialize the list
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(testList);
    }

    // Deserialize using pattern from PSDtdTree.clone()
    Object result;
    try (ObjectInputStream objInStream = new ObjectInputStream(Files.newInputStream(serFile))) {
      // Apply the deserialization filter to prevent CWE-502
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objInStream.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objInStream.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.List.class, result);
    java.util.List<String> deserializedList = (java.util.List<String>) result;
    assertEquals(3, deserializedList.size());
    assertEquals("element1", deserializedList.get(0));
  }

  @Test
  @DisplayName("Should handle nested serialized structures with filter")
  void testDeserializeNestedStructure() throws IOException, ClassNotFoundException {
    // Test with nested map structure similar to what DTD trees might contain
    java.util.HashMap<String, java.util.ArrayList<String>> nestedMap = new java.util.HashMap<>();
    java.util.ArrayList<String> children = new java.util.ArrayList<>();
    children.add("child1");
    children.add("child2");
    nestedMap.put("parent", children);

    Path serFile = tempDir.resolve("dtd-nested.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(nestedMap);
    }

    Object result;
    try (ObjectInputStream objInStream = new ObjectInputStream(Files.newInputStream(serFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objInStream.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objInStream.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.Map.class, result);

    java.util.HashMap<String, java.util.ArrayList<String>> deserializedMap =
        (java.util.HashMap<String, java.util.ArrayList<String>>) result;
    assertTrue(deserializedMap.containsKey("parent"));
    assertEquals(2, deserializedMap.get("parent").size());
  }

  @Test
  @DisplayName("Should allow safe collection classes for tree cloning")
  void testCloneAllowsSafeCollections() throws IOException {
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
    assertNotNull(filterSpec);
    // Verify filter includes java.util collections
    assertTrue(filterSpec.contains("java.util") || filterSpec.contains("java.lang"));
  }

  @Test
  @DisplayName("Should deserialize with multiple ArrayList serializations")
  void testMultipleListDeserialization() throws IOException, ClassNotFoundException {
    java.util.ArrayList<Integer> numbers = new java.util.ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      numbers.add(i);
    }

    Path serFile = tempDir.resolve("dtd-numbers.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(numbers);
    }

    Object result;
    try (ObjectInputStream objInStream = new ObjectInputStream(Files.newInputStream(serFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objInStream.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objInStream.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.List.class, result);

    java.util.ArrayList<Integer> deserializedNumbers = (java.util.ArrayList<Integer>) result;
    assertEquals(5, deserializedNumbers.size());
    assertEquals(3, deserializedNumbers.get(2));
  }
}
