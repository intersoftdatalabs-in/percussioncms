// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.rx.config;

import java.util.List;
import java.util.Map;

/**
 * Provides access to Spring Bean properties defined in default and/or local configuration files.
 *
 * @author YuBingChen
 */
public interface IPSBeanProperties {

  /**
   * Gets all properties.
   *
   * @return the properties, never {@code null}, may be empty.
   */
  Map<String, Object> getProperties();

  /**
   * Gets a specified property.
   *
   * @param name the name of the property to retrieve.
   * @return the value of the property, or {@code null} if not found.
   */
  Object getProperty(String name);

  /**
   * Gets a string associated with a specified property.
   *
   * @param name the name of the property to retrieve.
   * @return the associated string, or {@code null} if not found.
   */
  String getString(String name);

  /**
   * Gets a list associated with a specified property.
   *
   * @param name the name of the property to retrieve.
   * @return the associated list, or {@code null} if not found.
   */
  List<?> getList(String name);

  /**
   * Gets a map associated with a specified property.
   *
   * @param name the name of the property to retrieve.
   * @return the associated map, or {@code null} if not found.
   */
  Map<?, ?> getMap(String name);

  /**
   * Saves a set of properties. The specified properties will override and merge into current
   * properties, and will be saved into the repository.
   *
   * @param props the properties to save. Never {@code null}.
   */
  void save(Map<String, Object> props);
}
