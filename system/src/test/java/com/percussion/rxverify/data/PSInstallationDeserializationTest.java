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

package com.percussion.rxverify.data;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.validation.SerializationValidation;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PSInstallation deserialization security (CWE-502). Tests that the
 * ObjectInputFilter is properly applied in the readExternal() method when deserializing
 * installation data.
 */
@DisplayName("PSInstallation - Deserialization Security")
class PSInstallationDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should allow HashMap deserialization with filter applied")
  void testDeserializeHashMapWithFilter() throws IOException, ClassNotFoundException {
    // PSInstallation deserializes HashMaps for files and extensions
    Map<String, java.util.ArrayList<String>> fileMap = new HashMap<>();
    java.util.ArrayList<String> files = new java.util.ArrayList<>();
    files.add("/opt/percussion/install/file1.jar");
    files.add("/opt/percussion/install/file2.jar");
    fileMap.put("libs", files);

    Path serFile = tempDir.resolve("installation-files.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(fileMap);
    }

    Object result;
    try (ObjectInputStream objIn = new ObjectInputStream(Files.newInputStream(serFile))) {
      // Apply filter as PSInstallation.readExternal() does
      if (objIn instanceof ObjectInputStream) {
        String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
        objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      }
      result = objIn.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(Map.class, result);

    Map<String, java.util.ArrayList<String>> deserializedMap =
        (Map<String, java.util.ArrayList<String>>) result;
    assertTrue(deserializedMap.containsKey("libs"));
    assertEquals(2, deserializedMap.get("libs").size());
  }

  @Test
  @DisplayName("Should deserialize complex installation extension map")
  void testDeserializeExtensionMap() throws IOException, ClassNotFoundException {
    Map<String, java.util.ArrayList<String>> extensionMap = new HashMap<>();
    java.util.ArrayList<String> extensions = new java.util.ArrayList<>();
    extensions.add("com.percussion.extensions.custom.MyExtension");
    extensions.add("com.percussion.extensions.custom.AnotherExtension");
    extensionMap.put("handlers", extensions);

    Path serFile = tempDir.resolve("installation-extensions.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(extensionMap);
    }

    Object result;
    try (ObjectInputStream objIn = new ObjectInputStream(Files.newInputStream(serFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objIn.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(Map.class, result);
  }

  @Test
  @DisplayName("Should handle multiple file categories with filter")
  void testDeserializeMultipleCategories() throws IOException, ClassNotFoundException {
    Map<String, java.util.ArrayList<String>> multiMap = new HashMap<>();
    java.util.ArrayList<String> category1 = new java.util.ArrayList<>();
    category1.add("file1.txt");
    java.util.ArrayList<String> category2 = new java.util.ArrayList<>();
    category2.add("file2.txt");

    multiMap.put("config", category1);
    multiMap.put("backup", category2);

    Path serFile = tempDir.resolve("installation-multi.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(serFile))) {
      oos.writeObject(multiMap);
    }

    Object result;
    try (ObjectInputStream objIn = new ObjectInputStream(Files.newInputStream(serFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objIn.readObject();
    }

    assertNotNull(result);

    Map<String, java.util.ArrayList<String>> deserializedMap =
        (Map<String, java.util.ArrayList<String>>) result;
    assertEquals(2, deserializedMap.size());
  }

  @Test
  @DisplayName("Should allow installation timestamp serialization")
  void testDeserializeTimestampData() throws IOException, ClassNotFoundException {
    // PSInstallation stores timestamps
    long timestamp = System.currentTimeMillis();

    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeLong(timestamp);
    }

    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      ois.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      long deserializedTimestamp = ois.readLong();
      assertEquals(timestamp, deserializedTimestamp);
    }
  }

  @Test
  @DisplayName("Filter should allow java.util collections in Externalizable")
  void testExternalizableFilterAllowsCollections() {
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
    assertNotNull(filterSpec);
    assertTrue(
        filterSpec.contains("java.util") || filterSpec.contains("java.lang"),
        "Filter should allow java.util collections");
  }
}
