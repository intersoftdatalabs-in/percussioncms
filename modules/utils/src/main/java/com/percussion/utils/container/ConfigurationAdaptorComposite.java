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

import com.percussion.utils.container.config.ContainerConfig;

/**
 * Composite configuration adapter that combines multiple configuration adapters.
 *
 * @param <T> the configuration context type
 * @param <U> the container configuration type
 */
public interface ConfigurationAdaptorComposite<
        T extends ConfigurationCtx, U extends ContainerConfig>
    extends IPSConfigurationAdapter<T> {

  /**
   * Adds a configuration adapter to the composite.
   *
   * @param adapter the adapter to add
   */
  void addConfigurationAdapter(IPSConfigurationAdapter<T> adapter);

  /**
   * Gets the combined configuration.
   *
   * @return the configuration
   */
  U getConfig();

  /** Loads configuration from all adapters. */
  void load();

  /** Saves configuration to all adapters. */
  void save();
}
