/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils.properties;

import java.util.Date;
import java.util.List;

/**
 * Defines the data types that a property can represent.
 *
 * @author natechadwick
 * @author adamgent
 */
public enum PSPropertyDataType {

  STRING("string", String.class),
  ENUM("enum", String.class),
  NUMBER("number", Number.class),
  BOOL("bool", Boolean.class),
  HIDDEN("hidden", Object.class),
  DATE("date", Date.class),
  LIST("list", List.class),
  PASSWORD("password", String.class);

  private final String name;
  private final Class<?> javaType;

  PSPropertyDataType(String name, Class<?> javaType) {
    this.name = name;
    this.javaType = javaType;
  }

  /**
   * Gets the nominal value of the data type.
   * @return never null or empty.
   */
  public String getName() {
    return name;
  }

  /**
   * The java type that the widget property should be.
   * @return never null.
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * Gets the data type from widget property definition.
   * @param prop never null.
   * @return never null.
   */
  public static PSPropertyDataType fromDefinition(PSPropertyDefinition prop) {
    return parseType(prop.getDatatype());
  }

  /**
   * Parse the {@link #getName()} property definition type.
   * @param name property type name
   * @return never null.
   */
  public static PSPropertyDataType parseType(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Property type name cannot be null");
    }
    var n = name.trim().toUpperCase();
    return valueOf(n);
  }
}
