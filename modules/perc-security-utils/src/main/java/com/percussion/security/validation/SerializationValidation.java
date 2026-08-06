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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Security utility for validating Java object deserialization. Provides patterns and guidance to
 * prevent gadget chain attacks and unsafe deserialization (CWE-502).
 *
 * <p>Java serialization can be exploited through gadget chains in application dependencies. This
 * utility provides: - Predefined sets of safe classes - Filter specifications for use with JVM
 * serialization filters - Documentation of best practices for deserialization
 *
 * <p>Usage of JVM-level filter (recommended):
 *
 * <pre>
 * // Set system property before running application:
 * // -Djdk.serialFilter=java.lang.String;java.util.ArrayList;com.percussion.MyClass;!*
 * </pre>
 *
 * <p>Manual validation in code:
 *
 * <pre>
 * Class&lt;?&gt; cls = Class.forName(className, false, classLoader);
 * if (!SerializationValidation.isSafeClass(className)) {
 *   throw new InvalidClassException("Class not in allow-list: " + className);
 * }
 * </pre>
 *
 * <p>CWE-502: Deserialization of Untrusted Data
 *
 * <p>Reference: JEP 290 - Filter Incoming Serialization Data
 */
public class SerializationValidation {

  // Standard safe Java classes that are generally safe to deserialize
  private static final Set<String> JAVA_SAFE_CLASSES =
      new HashSet<>(
          Arrays.asList(
              "java.lang.String",
              "java.lang.Integer",
              "java.lang.Long",
              "java.lang.Double",
              "java.lang.Float",
              "java.lang.Boolean",
              "java.lang.Byte",
              "java.lang.Short",
              "java.lang.Character",
              "java.lang.Number",
              "java.util.ArrayList",
              "java.util.HashMap",
              "java.util.HashSet",
              "java.util.LinkedList",
              "java.util.TreeMap",
              "java.util.TreeSet",
              "java.util.LinkedHashMap",
              "java.util.LinkedHashSet",
              "java.util.Collections",
              "java.util.Date",
              "java.util.Calendar",
              "java.util.Locale",
              "java.util.TimeZone",
              "java.text.SimpleDateFormat",
              "java.text.DecimalFormat",
              "java.text.Format",
              "java.math.BigDecimal",
              "java.math.BigInteger",
              "java.io.File",
              "java.net.URI",
              "java.net.URL",
              "java.util.UUID",
              // Array support for collections
              "[Ljava.lang.String;",
              "[Ljava.lang.Object;",
              "[Ljava.util.Map.Entry;"));

  // Percussion framework classes commonly used in serialization
  private static final Set<String> PERCUSSION_SAFE_CLASSES =
      new HashSet<>(
          Arrays.asList(
              "com.percussion.cms.objectstore.PSComponentSummary",
              "com.percussion.design.objectstore.PSLocator",
              "com.percussion.utils.guid.IPSGuid",
              "com.percussion.services.guidmgr.data.PSGuid"));

  private SerializationValidation() {
    // Utility class - no instantiation
  }

  /**
   * Checks if a class is in the standard safe classes list.
   *
   * @param className fully qualified class name
   * @return true if the class is considered safe for deserialization
   */
  public static boolean isSafeClass(String className) {
    if (className == null || className.isEmpty()) {
      return false;
    }
    // Handle array types
    if (className.endsWith("[]")) {
      className = className.substring(0, className.length() - 2);
    }
    return JAVA_SAFE_CLASSES.contains(className);
  }

  /**
   * Checks if a class is a safe Percussion framework class.
   *
   * @param className fully qualified class name
   * @return true if the class is a known safe Percussion class
   */
  public static boolean isPercussionSafeClass(String className) {
    if (className == null || className.isEmpty()) {
      return false;
    }
    // Handle array types
    if (className.endsWith("[]")) {
      className = className.substring(0, className.length() - 2);
    }
    return PERCUSSION_SAFE_CLASSES.contains(className);
  }

  /**
   * Builds a serialization filter specification string for JVM-level filtering.
   *
   * <p>The resulting string can be used with the {@code jdk.serialFilter} system property:
   *
   * <pre>
   * String filter = SerializationValidation.buildFilterSpec(
   *     "com.percussion.MyClass", "org.custom.SafeClass"
   * );
   * // Set: -Djdk.serialFilter=filter
   * </pre>
   *
   * <p>The filter rejects all classes not explicitly allowed (!*).
   *
   * @param additionalClasses classes to allow beyond standard safe classes
   * @return filter specification string suitable for jdk.serialFilter system property
   * @throws IllegalArgumentException if additionalClasses is null or empty
   */
  public static String buildFilterSpec(String... additionalClasses) {
    if (additionalClasses == null || additionalClasses.length == 0) {
      throw new IllegalArgumentException("additionalClasses cannot be null or empty");
    }

    StringBuilder spec = new StringBuilder();

    // Add all standard safe classes
    for (String cls : JAVA_SAFE_CLASSES) {
      spec.append(cls).append(";");
    }

    // Add all Percussion safe classes
    for (String cls : PERCUSSION_SAFE_CLASSES) {
      spec.append(cls).append(";");
    }

    // Add additional classes
    for (String cls : additionalClasses) {
      spec.append(cls).append(";");
    }

    // Reject all others
    spec.append("!*");

    return spec.toString();
  }

  /**
   * Builds a permissive filter spec that allows specific packages.
   *
   * <p>Example:
   *
   * <pre>
   * String filter = SerializationValidation.buildPackageFilterSpec("com.percussion.*");
   * </pre>
   *
   * <p>Note: Uses JEP 290 filter syntax where * matches any class in that package.
   *
   * @param packagePatterns package patterns to allow (e.g., "com.percussion.*")
   * @return filter specification string
   * @throws IllegalArgumentException if packagePatterns is null or empty
   */
  public static String buildPackageFilterSpec(String... packagePatterns) {
    if (packagePatterns == null || packagePatterns.length == 0) {
      throw new IllegalArgumentException("packagePatterns cannot be null or empty");
    }

    StringBuilder spec = new StringBuilder();

    // Add safe JDK package patterns that allow serialization of standard classes
    spec.append("java.lang.*;");
    spec.append("java.util.*;");
    spec.append("java.text.*;");
    spec.append("java.io.*;");
    spec.append("java.net.*;");
    spec.append("java.math.*;");

    // Add explicitly safe classes
    for (String cls : JAVA_SAFE_CLASSES) {
      spec.append(cls).append(";");
    }

    // Add package patterns, converting ** to * for JEP 290 compatibility
    for (String pattern : packagePatterns) {
      // Convert ** wildcard to * for proper JEP 290 syntax
      String normalizedPattern = pattern.replace(".**", ".*");
      spec.append(normalizedPattern).append(";");
    }

    // Reject all others
    spec.append("!*");

    return spec.toString();
  }

  /**
   * Gets the set of standard safe Java classes that are pre-approved for deserialization.
   *
   * @return immutable copy of safe class names
   */
  public static Set<String> getStandardSafeClasses() {
    return new HashSet<>(JAVA_SAFE_CLASSES);
  }

  /**
   * Gets the set of safe Percussion framework classes.
   *
   * @return immutable copy of Percussion safe class names
   */
  public static Set<String> getPercussionSafeClasses() {
    return new HashSet<>(PERCUSSION_SAFE_CLASSES);
  }
}
