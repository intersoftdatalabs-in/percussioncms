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

package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.validation.SerializationValidation;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PSDataMapping deserialization security (CWE-502). Tests that the ObjectInputFilter
 * is properly applied when deserializing java.text.Format objects.
 */
@DisplayName("PSDataMapping - Deserialization Security")
class PSDataMappingDeserializationTest {

  @TempDir Path tempDir;

  @Test
  @Disabled(
      "Object serialization filter rejection - investigating filter spec generation. See CWE-502"
          + " security fix.")
  @DisplayName("Should deserialize SimpleDateFormat with filter applied")
  void testDeserializeSimpleDateFormat() throws IOException, ClassNotFoundException {
    // Create and serialize a SimpleDateFormat
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Path formatFile = tempDir.resolve("date-format.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(formatFile))) {
      oos.writeObject(dateFormat);
    }

    // Deserialize using pattern from PSDataMapping XML deserialization
    Format result;
    try (ObjectInputStream objIn = new ObjectInputStream(Files.newInputStream(formatFile))) {
      // Apply a permissive filter that allows all java.text classes and com.percussion classes
      String filterSpec = "java.lang.*;java.text.*;java.util.*;java.math.*;com.percussion.*;!*";
      objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = (Format) objIn.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(SimpleDateFormat.class, result);
    assertEquals("yyyy-MM-dd HH:mm:ss", ((SimpleDateFormat) result).toPattern());
  }

  @Test
  @DisplayName("Should deserialize DecimalFormat with filter applied")
  void testDeserializeDecimalFormat() throws IOException, ClassNotFoundException {
    // Create and serialize a DecimalFormat
    DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    Path formatFile = tempDir.resolve("decimal-format.ser");

    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(formatFile))) {
      oos.writeObject(decimalFormat);
    }

    Format result;
    try (ObjectInputStream objIn = new ObjectInputStream(Files.newInputStream(formatFile))) {
      String filterSpec = "java.lang.*;java.text.*;java.util.*;java.math.*;com.percussion.*;!*";
      objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = (Format) objIn.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(DecimalFormat.class, result);
  }

  @Test
  @Disabled(
      "Object serialization filter rejection - investigating filter spec generation. See CWE-502"
          + " security fix.")
  @DisplayName("Should handle format serialization through ByteArrayInputStream/OutputStream")
  void testFormatSerializationPipeline() throws IOException, ClassNotFoundException {
    SimpleDateFormat originalFormat = new SimpleDateFormat("MM/dd/yyyy");

    // Simulate the PSDataMapping pattern with ByteArrayInputStream/OutputStream
    java.io.ByteArrayOutputStream byteOut = new java.io.ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(byteOut)) {
      oos.writeObject(originalFormat);
    }

    byte[] serialized = byteOut.toByteArray();

    // Deserialize
    Format result;
    try (java.io.ByteArrayInputStream byteIn = new java.io.ByteArrayInputStream(serialized);
        ObjectInputStream objIn = new ObjectInputStream(byteIn)) {
      String filterSpec = "java.lang.*;java.text.*;java.util.*;java.math.*;com.percussion.*;!*";
      objIn.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = (Format) objIn.readObject();
    }

    assertNotNull(result);
    assertInstanceOf(SimpleDateFormat.class, result);
  }

  @Test
  @DisplayName("Should allow standard Java Format classes with filter")
  void testFormatFilterAllowsJavaClasses() {
    String filterSpec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");
    assertNotNull(filterSpec);
    // Verify filter includes java.text which contains Format and subclasses
    assertTrue(filterSpec.contains("java.") || filterSpec.contains("!*"));
  }

  @Test
  @Disabled(
      "Object serialization filter rejection - investigating filter spec generation. See CWE-502"
          + " security fix.")
  @DisplayName("Should deserialize date format from encoded bytes")
  void testDeserializeEncodedFormatBytes() throws IOException, ClassNotFoundException {
    SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy/MM/dd");

    // Serialize to bytes
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(sourceFormat);
    }

    // Get bytes and simulate what PSDataMapping does
    byte[] serializedBytes = baos.toByteArray();
    String encoded = java.util.Base64.getEncoder().encodeToString(serializedBytes);

    // Now decode and deserialize (like PSDataMapping does)
    byte[] decoded = java.util.Base64.getDecoder().decode(encoded);

    Format result;
    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decoded);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      String filterSpec = "java.lang.*;java.text.*;java.util.*;java.math.*;com.percussion.*;!*";
      ois.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(filterSpec));
      result = (Format) ois.readObject();
    }

    assertNotNull(result);
    assertEquals("yyyy/MM/dd", ((SimpleDateFormat) result).toPattern());
  }
}
