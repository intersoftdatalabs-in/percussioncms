/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.utils.xml;

/**
 * Maps Java type / property simple names to Betwixt-compatible XML element names used by design
 * object XML in package deploy and catalog.
 *
 * <p><strong>Naming strategy (must stay aligned with Betwixt {@code HyphenatedNameMapper} usage in
 * {@link PSXmlSerializationHelper}):</strong>
 *
 * <ol>
 *   <li>Strip leading {@code PS} or {@code IPS} type prefixes (product class naming convention).
 *   <li>For inner classes, keep only the simple name segment after the last {@code $}.
 *   <li>Flatten multi-capital runs so {@code GUID} becomes {@code Guid} (avoids {@code g-u-i-d}).
 *   <li>Hyphenate camelCase boundaries ({@code SampleKeyword} → {@code sample-keyword}).
 * </ol>
 *
 * <p>Property names (no {@code PS}/{@code IPS} strip) use the same hyphenation step only.
 *
 * @see PSXmlSerializationHelper
 * @see PSJacksonXmlSerializationHelper
 */
public final class PSXmlElementNameMapper {

  private PSXmlElementNameMapper() {
    // utility
  }

  /**
   * Map a type simple name (e.g. {@code PSKeyword}, {@code SampleKeyword}) to the Betwixt root /
   * type element name (e.g. {@code keyword}, {@code sample-keyword}).
   *
   * @param simpleName class simple name, never {@code null}
   * @return hyphenated element name, never {@code null}
   */
  public static String mapTypeToElementName(String simpleName) {
    if (simpleName == null) {
      throw new IllegalArgumentException("simpleName may not be null");
    }
    String name = simpleName;
    if (name.startsWith("PS")) {
      name = name.substring(2);
    } else if (name.startsWith("IPS")) {
      name = name.substring(3);
    }

    if (name.contains("$")) {
      int i = name.indexOf('$');
      name = name.substring(i + 1);
    }

    // Proper case multiple capitals, i.e. GUID -> Guid
    StringBuilder b = new StringBuilder();
    boolean wasCap = false;
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (Character.isUpperCase(ch)) {
        if (wasCap) {
          b.append(Character.toLowerCase(ch));
          continue;
        } else {
          wasCap = true;
        }
      } else {
        wasCap = false;
      }
      b.append(ch);
    }

    return hyphenateCamelCase(b.toString());
  }

  /**
   * Map a JavaBeans property name to a hyphenated XML element / attribute name (Betwixt property
   * mapper behavior). Does not strip {@code PS}/{@code IPS} prefixes.
   *
   * @param propertyName bean property name (e.g. {@code contentTypeId}), never {@code null}
   * @return hyphenated name (e.g. {@code content-type-id})
   */
  public static String mapPropertyToElementName(String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException("propertyName may not be null");
    }
    return hyphenateCamelCase(propertyName);
  }

  /**
   * Hyphenate a camelCase / PascalCase identifier the same way Apache Commons Betwixt {@code
   * HyphenatedNameMapper} does for type names after PS-prefix stripping.
   */
  static String hyphenateCamelCase(String name) {
    if (name.isEmpty()) {
      return name;
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (Character.isUpperCase(ch)) {
        if (i > 0) {
          out.append('-');
        }
        out.append(Character.toLowerCase(ch));
      } else {
        out.append(ch);
      }
    }
    return out.toString();
  }
}
