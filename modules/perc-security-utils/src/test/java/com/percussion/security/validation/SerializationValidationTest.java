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

package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SerializationValidation. Tests safe class detection and filter specification
 * building for CWE-502 protection.
 */
@DisplayName("SerializationValidation - Deserialization Security")
class SerializationValidationTest {

  @Test
  @DisplayName("Should identify standard safe Java classes")
  void testIsSafeClass() {
    assertTrue(SerializationValidation.isSafeClass("java.lang.String"));
    assertTrue(SerializationValidation.isSafeClass("java.util.ArrayList"));
    assertTrue(SerializationValidation.isSafeClass("java.util.HashMap"));
    assertTrue(SerializationValidation.isSafeClass("java.util.Date"));
  }

  @Test
  @DisplayName("Should handle array types in safe class check")
  void testIsSafeClassWithArrays() {
    assertTrue(SerializationValidation.isSafeClass("java.lang.String[]"));
    assertTrue(SerializationValidation.isSafeClass("java.util.ArrayList[]"));
  }

  @Test
  @DisplayName("Should reject gadget chain classes")
  void testRejectGadgetClasses() {
    assertFalse(SerializationValidation.isSafeClass("org.apache.commons.beanutils.BeanComparator"));
    assertFalse(
        SerializationValidation.isSafeClass(
            "com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
    assertFalse(SerializationValidation.isSafeClass("org.evil.GadgetChain"));
  }

  @Test
  @DisplayName("Should reject null or empty class names")
  void testRejectNullEmptyClasses() {
    assertFalse(SerializationValidation.isSafeClass(null));
    assertFalse(SerializationValidation.isSafeClass(""));
  }

  @Test
  @DisplayName("Should identify Percussion safe classes")
  void testIsPercussionSafeClass() {
    assertTrue(
        SerializationValidation.isPercussionSafeClass(
            "com.percussion.cms.objectstore.PSComponentSummary"));
    assertTrue(
        SerializationValidation.isPercussionSafeClass(
            "com.percussion.design.objectstore.PSLocator"));
  }

  @Test
  @DisplayName("Should build filter spec for explicit classes")
  void testBuildFilterSpec() {
    String spec =
        SerializationValidation.buildFilterSpec(
            "com.percussion.MyClass", "com.percussion.OtherClass");

    assertNotNull(spec);
    assertTrue(spec.contains("com.percussion.MyClass"));
    assertTrue(spec.contains("com.percussion.OtherClass"));
    assertTrue(spec.contains("!*")); // Should reject all others
    assertTrue(spec.contains("java.lang.String")); // Should include standard safe classes
  }

  @Test
  @DisplayName("Should reject empty class list for filter spec")
  void testBuildFilterSpecEmptyList() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SerializationValidation.buildFilterSpec(),
        "Should require at least one class");
  }

  @Test
  @DisplayName("Should build package-based filter spec")
  void testBuildPackageFilterSpec() {
    String spec = SerializationValidation.buildPackageFilterSpec("com.percussion.**");

    assertNotNull(spec);
    assertTrue(spec.contains("com.percussion.*"));
    assertTrue(spec.contains("!*")); // Should reject all others
    assertTrue(spec.contains("java.lang.*")); // Should include standard safe package patterns
  }

  @Test
  @DisplayName("Should get standard safe classes set")
  void testGetStandardSafeClasses() {
    Set<String> safeClasses = SerializationValidation.getStandardSafeClasses();

    assertNotNull(safeClasses);
    assertTrue(safeClasses.contains("java.lang.String"));
    assertTrue(safeClasses.contains("java.util.ArrayList"));
    assertTrue(safeClasses.contains("java.util.HashMap"));
    assertTrue(safeClasses.size() > 10);
  }

  @Test
  @DisplayName("Should get Percussion safe classes set")
  void testGetPercussionSafeClasses() {
    Set<String> safeClasses = SerializationValidation.getPercussionSafeClasses();

    assertNotNull(safeClasses);
    assertTrue(safeClasses.contains("com.percussion.cms.objectstore.PSComponentSummary"));
    assertTrue(safeClasses.contains("com.percussion.design.objectstore.PSLocator"));
  }
}
