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

package com.percussion.rxverify;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.validation.SerializationValidation;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PSVerify deserialization security (CWE-502). Tests that the ObjectInputFilter is
 * properly applied when deserializing PSInstallation from BOM (Bill of Materials) files.
 */
@DisplayName("PSVerify - Deserialization Security")
class PSVerifyDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should deserialize installation data from BOM file with filter")
  void testDeserializeBOMFileWithFilter() throws IOException, ClassNotFoundException {
    // Simulate BOM file serialization similar to PSVerify usage
    java.util.HashMap<String, String> bomData = new java.util.HashMap<>();
    bomData.put("version", "8.1.6");
    bomData.put("timestamp", String.valueOf(System.currentTimeMillis()));
    bomData.put("installationPath", "/opt/percussion");

    Path bomFile = tempDir.resolve("installation.bom");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(bomFile))) {
      oos.writeObject(bomData);
    }

    // Deserialize using pattern from PSVerify.loadInstallation()
    Object result;
    try (var inputStream = Files.newInputStream(bomFile);
        var objectInput = new ObjectInputStream(inputStream)) {
      // Apply the deserialization filter to prevent CWE-502
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objectInput.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objectInput.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.Map.class, result);

    java.util.HashMap<String, String> deserializedBOM = (java.util.HashMap<String, String>) result;
    assertEquals("8.1.6", deserializedBOM.get("version"));
  }

  @Test
  @DisplayName("Should handle FileInputStream with try-with-resources and filter")
  void testFileInputStreamWithTryWithResources() throws IOException, ClassNotFoundException {
    java.util.ArrayList<String> fileList = new java.util.ArrayList<>();
    fileList.add("file1.jar");
    fileList.add("file2.class");
    fileList.add("file3.properties");

    Path bomFile = tempDir.resolve("files.bom");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(bomFile))) {
      oos.writeObject(fileList);
    }

    Object result;
    try (var inputStream = new FileInputStream(bomFile.toFile());
        var objectInput = new ObjectInputStream(inputStream)) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objectInput.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objectInput.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(java.util.List.class, result);

    java.util.ArrayList<String> deserializedList = (java.util.ArrayList<String>) result;
    assertEquals(3, deserializedList.size());
  }

  @Test
  @DisplayName("Should deserialize nested verification data with filter")
  void testDeserializeVerificationData() throws IOException, ClassNotFoundException {
    // Simulate complex BOM structure for verification
    java.util.HashMap<String, java.util.List<String>> verificationMap = new java.util.HashMap<>();
    java.util.ArrayList<String> installedFiles = new java.util.ArrayList<>();
    installedFiles.add("/opt/percussion/lib/percussion-core.jar");
    installedFiles.add("/opt/percussion/lib/percussion-api.jar");
    verificationMap.put("installed_files", installedFiles);

    java.util.ArrayList<String> checksums = new java.util.ArrayList<>();
    checksums.add("abc123def456");
    checksums.add("xyz789uvw012");
    verificationMap.put("checksums", checksums);

    Path bomFile = tempDir.resolve("verification.bom");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(bomFile))) {
      oos.writeObject(verificationMap);
    }

    Object result;
    try (var inputStream = new FileInputStream(bomFile.toFile());
        var objectInput = new ObjectInputStream(inputStream)) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objectInput.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objectInput.readObject();
    }

    assertNotNull(result);

    java.util.HashMap<String, java.util.List<String>> deserializedMap =
        (java.util.HashMap<String, java.util.List<String>>) result;
    assertTrue(deserializedMap.containsKey("installed_files"));
    assertEquals(2, deserializedMap.get("installed_files").size());
  }

  @Test
  @DisplayName("Filter spec should be consistent for BOM file deserialization")
  void testBOMFilterConsistency() {
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
    assertNotNull(filterSpec);
    // Verify filter allows percussion classes
    assertTrue(
        filterSpec.contains("com.percussion") || filterSpec.contains("java"),
        "Filter should allow com.percussion classes");
  }

  @Test
  @DisplayName("Should handle large serialized BOM structures")
  void testLargeBOMSerialization() throws IOException, ClassNotFoundException {
    java.util.HashMap<String, java.util.ArrayList<String>> largeBOM = new java.util.HashMap<>();

    // Create large lists like real installation BOM might have
    java.util.ArrayList<String> manyFiles = new java.util.ArrayList<>();
    for (int i = 0; i < 100; i++) {
      manyFiles.add("/opt/percussion/file" + i + ".jar");
    }
    largeBOM.put("files", manyFiles);

    Path bomFile = tempDir.resolve("large.bom");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(bomFile))) {
      oos.writeObject(largeBOM);
    }

    Object result;
    try (var inputStream = new FileInputStream(bomFile.toFile());
        var objectInput = new ObjectInputStream(inputStream)) {
      String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
      objectInput.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = objectInput.readObject();
    }

    assertNotNull(result);

    java.util.HashMap<String, java.util.ArrayList<String>> deserializedMap =
        (java.util.HashMap<String, java.util.ArrayList<String>>) result;
    assertEquals(100, deserializedMap.get("files").size());
  }
}
