// REFACTORED: CP-JAVA11
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
package com.percussion.rx.config;

import com.percussion.rx.config.data.PSConfigStatus.ConfigStatus;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Collection;

/**
 * Listener interface for changes to package configurations.
 */
public interface IPSConfigChangeListener {

  /**
   * Notifies listeners when a configuration has been applied to a package.
   *
   * @param ids a set of IDs of the configured Design Objects, never {@code null} or empty.
   * @param status the status of the package configuration, never {@code null}.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  void configChanged(Collection<IPSGuid> ids, ConfigStatus status) throws PSNotFoundException;

  /**
   * Notifies listeners before a configuration is applied to a package.
   *
   * @param name the package name, never {@code null} or empty.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  void preConfiguration(String name) throws PSNotFoundException;
}
