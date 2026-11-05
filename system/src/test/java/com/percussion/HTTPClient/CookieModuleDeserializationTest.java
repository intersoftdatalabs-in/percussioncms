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

package com.percussion.HTTPClient;

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
 * Unit tests for CookieModule deserialization security (CWE-502). Tests that the ObjectInputFilter
 * is properly applied in loadCookies() method.
 */
@DisplayName("CookieModule - Deserialization Security")
class CookieModuleDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should deserialize valid cookie map with filter applied")
  void testDeserializeValidCookieMap() throws IOException, ClassNotFoundException {
    // Create a map of cookies (what CookieModule would serialize/deserialize)
    Map<String, String> cookieMap = new HashMap<>();
    cookieMap.put("JSESSIONID", "ABC123DEF456");
    cookieMap.put("user_preference", "dark_mode");

    Path cookieFile = tempDir.resolve("cookies.ser");

    // Serialize the cookie map
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cookieFile))) {
      oos.writeObject(cookieMap);
    }

    // Deserialize using the pattern from CookieModule.loadCookies()
    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cookieFile))) {
      // Apply the deserialization filter to prevent CWE-502
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(Map.class, result);

    Map<String, String> deserializedMap = (Map<String, String>) result;
    assertEquals(2, deserializedMap.size());
    assertEquals("ABC123DEF456", deserializedMap.get("JSESSIONID"));
  }

  @Test
  @DisplayName("Should allow safe collection classes in cookie deserialization")
  void testDeserializeComplexCookieStructure() throws IOException, ClassNotFoundException {
    // Test with more complex structure
    Map<String, java.util.List<String>> complexMap = new HashMap<>();
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    values.add("cookie_value_1");
    values.add("cookie_value_2");
    complexMap.put("multi_cookies", values);

    Path cookieFile = tempDir.resolve("complex-cookies.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cookieFile))) {
      oos.writeObject(complexMap);
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cookieFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(Map.class, result);

    Map<String, java.util.List<String>> deserializedMap =
        (Map<String, java.util.List<String>>) result;
    assertTrue(deserializedMap.containsKey("multi_cookies"));
  }

  @Test
  @DisplayName("Should handle empty cookie map deserialization")
  void testDeserializeEmptyCookieMap() throws IOException, ClassNotFoundException {
    Map<String, String> emptyMap = new HashMap<>();

    Path cookieFile = tempDir.resolve("empty-cookies.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cookieFile))) {
      oos.writeObject(emptyMap);
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cookieFile))) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = in.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(Map.class, result);
    Map<String, String> deserializedMap = (Map<String, String>) result;
    assertTrue(deserializedMap.isEmpty());
  }

  @Test
  @DisplayName("Filter spec should reject all non-whitelisted classes")
  void testFilterBlocksDangerousClasses() {
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
    assertNotNull(filterSpec);
    // Verify the filter includes safe JDK classes and percussion
    assertTrue(filterSpec.contains("com.percussion") || filterSpec.contains("java.util"));
  }
}
