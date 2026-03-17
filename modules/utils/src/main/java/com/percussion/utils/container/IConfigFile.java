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

package com.percussion.utils.container;

import java.nio.file.Path;
import java.util.Map;

/**
 * Interface for configuration file operations.
 */
public interface IConfigFile {
  /**
   * Gets the path to the configuration file.
   * @return the file path
   */
  Path getPath();

  /**
   * Loads the configuration properties from the file.
   * @return the properties map
   */
  Map<String, String> load();

  /**
   * Saves the configuration properties to the file.
   * @param properties the properties to save
   */
  void save(Map<String, String> properties);
}
